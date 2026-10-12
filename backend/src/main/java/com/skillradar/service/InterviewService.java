package com.skillradar.service;

import com.skillradar.dto.InterviewMessage;
import com.skillradar.dto.InterviewQuestionScore;
import com.skillradar.dto.InterviewReport;
import com.skillradar.dto.InterviewSessionView;
import com.skillradar.dto.InterviewSummary;
import com.skillradar.entity.InterviewQuestion;
import com.skillradar.entity.InterviewSession;
import com.skillradar.entity.JobCategory;
import com.skillradar.entity.JobSkill;
import com.skillradar.entity.Skill;
import com.skillradar.entity.User;
import com.skillradar.repository.InterviewQuestionRepository;
import com.skillradar.repository.InterviewSessionRepository;
import com.skillradar.repository.JobCategoryRepository;
import com.skillradar.repository.JobSkillRepository;
import com.skillradar.repository.SkillRepository;
import com.skillradar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI 模拟面试：从目标岗位相关技能的 interview_questions 里抽 5 道题，DeepSeek 扮演面试官。
 * 出题顺序和"每题最多追问一次"由这里的代码控制，AI 只负责两件事：
 *  1) 每次候选人回答后，给这道题评分/点评，并决定要不要追问（追问内容由 AI 生成）；
 *  2) 面试结束时，根据各题问答和评分写总评。
 * 这样面试节奏是确定的（不会被 AI 带偏成无限追问），而追问和评价又是因人而异的。
 * 每题的参考要点只在面试结束后展示，进行中的视图里不带。
 */
@Service
@RequiredArgsConstructor
public class InterviewService {

    private static final int QUESTIONS_PER_SESSION = 5;
    /** 从权重最高的这么多个技能里随机挑题，既贴合岗位重点，每次面试又不完全一样。 */
    private static final int TOP_SKILL_POOL = 8;
    private static final int MAX_ANSWER_LENGTH = 3000;
    private static final String[] TRANSITIONS = {"好的，我们看下一题。", "嗯，了解了。下一题：", "好，我们换个方向。"};

    private static final String EVAL_PROMPT = """
            你是「%s」岗位的技术面试官，正在进行一场模拟面试。下面给出当前这道题、参考要点（候选人看不到），
            以及这道题上你和候选人目前为止的对话。请评估候选人在这道题上的整体表现，并决定是否追问。

            score（1~5 的整数）：5=完整准确且有深度；4=核心正确、细节有小缺失；3=说到部分要点；
            2=很浅或有明显错误；1=不会或答非所问。
            comment：一两句话点评，先肯定答得好的地方，再指出最关键的遗漏或错误。面试结束后会展示给候选人，
            直接对候选人说话（用"你"）。如果回答标注了"时间到被自动提交"且明显没讲完，可以顺带提一句时间把控
            （比如先讲结论再展开），但评分仍然只看内容。
            follow_up：%s
            只输出 JSON：{"score": 整数, "comment": "...", "follow_up": "..."}""";

    private static final String FOLLOW_UP_ALLOWED = """
            如果候选人的回答有值得深挖的点，或者遗漏了关键要点但看起来有一定基础，提出一个简短、自然的追问，
            像真实面试官那样说话，不要透露参考要点原文；如果回答已经很完整，或候选人明显不会（追问没有意义），返回空字符串。""";
    private static final String FOLLOW_UP_USED = "这道题已经追问过一次了，返回空字符串。";

    private static final String REPORT_PROMPT = """
            你是「%s」岗位的资深面试官，刚结束一场模拟面试。根据下面每道题的问答记录和你当时的评分，给候选人写一份面试总评，
            直接对候选人说话（用"你"）。
            overall_score：0~100 的整数，综合各题表现（大致在各题平均分 × 20 附近，可根据表达清晰度和深度上下浮动 10 分以内）。
            summary：两三句话的总体评价。
            strengths：2~3 条做得好的地方，每条一句话，具体到题目或知识点。
            improvements：2~3 条最该提升的地方，每条给出具体可执行的建议（比如"复习 xxx 的 yyy 机制"）。
            如果回答标注了用时，说明这是限时作答：只有确实存在时间把控问题（多次时间到没讲完，或用时很短却答得很浅）时，
            才在 improvements 里加一条表达组织/时间分配的建议，否则不必提时间。
            只输出 JSON：{"overall_score": 整数, "summary": "...", "strengths": ["..."], "improvements": ["..."]}""";

    private final DeepSeekClient deepSeekClient;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final JobSkillRepository jobSkillRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public InterviewSessionView start(Long userId, Long categoryId) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少 userId");
        if (categoryId == null) {
            categoryId = userRepository.findById(userId).map(User::getTargetCategoryId).orElse(null);
        }
        if (categoryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先选择一个目标岗位");
        }
        String categoryName = categoryName(categoryId);
        List<InterviewQuestion> questions = pickQuestions(categoryId);
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "「" + categoryName + "」方向暂时还没有面试题");
        }

        InterviewSession s = new InterviewSession();
        s.setUserId(userId);
        s.setCategoryId(categoryId);
        s.setQuestionIds(write(questions.stream().map(InterviewQuestion::getId).toList()));
        List<InterviewMessage> transcript = new ArrayList<>();
        transcript.add(new InterviewMessage("interviewer",
                "你好，我是今天的面试官。这场模拟面试针对「" + categoryName + "」岗位，一共 " + questions.size()
                        + " 道题，有的题我会追问一次。回答时尽量讲清楚原理，能结合实际经验更好；不会的也可以直接说。\n\n"
                        + "第 1 题：" + questions.get(0).getQuestionText(), 0));
        s.setTranscript(write(transcript));
        s.setQuestionScores(write(List.of()));
        sessionRepository.save(s);
        return view(s);
    }

    public InterviewSessionView get(Long sessionId, Long userId) {
        return view(load(sessionId, userId));
    }

    public List<InterviewSummary> list(Long userId) {
        Map<Long, String> categoryNames = jobCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(JobCategory::getId, JobCategory::getName));
        return sessionRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(s -> new InterviewSummary(s.getId(), categoryNames.get(s.getCategoryId()), s.getStatus().name(),
                        s.getOverallScore(), readScores(s).size(), readIds(s).size(), s.getCreatedAt()))
                .toList();
    }

    /**
     * 候选人回答一句：评分 → 追问 / 下一题 / 结束。AI 调用失败时什么都不保存，前端保留输入让用户重发。
     * seconds / timeLimit / timedOut 只在限时模式下有值，计时由前端负责，这里只记录并交给 AI 参考。
     */
    public InterviewSessionView answer(Long sessionId, Long userId, String answer,
                                       Integer seconds, Integer timeLimit, Boolean timedOut) {
        InterviewSession s = load(sessionId, userId);
        if (s.getStatus() == InterviewSession.Status.finished) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "这场面试已经结束了");
        }
        if (answer == null || answer.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "回答不能为空");
        }
        String trimmed = answer.strip();
        if (trimmed.length() > MAX_ANSWER_LENGTH) trimmed = trimmed.substring(0, MAX_ANSWER_LENGTH);

        List<Long> ids = readIds(s);
        int idx = s.getCurrentIndex();
        List<InterviewMessage> transcript = new ArrayList<>(readTranscript(s));
        boolean timed = seconds != null && timeLimit != null;
        transcript.add(new InterviewMessage("candidate", trimmed, idx,
                timed ? Math.max(0, Math.min(seconds, timeLimit)) : null,
                timed ? timeLimit : null,
                timed ? Boolean.TRUE.equals(timedOut) : null));

        InterviewQuestion q = interviewQuestionRepository.findById(ids.get(idx))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE, "题目已被删除"));
        String categoryName = categoryName(s.getCategoryId());
        JsonNode eval = deepSeekClient.chatJson(
                EVAL_PROMPT.formatted(categoryName, s.isFollowUpUsed() ? FOLLOW_UP_USED : FOLLOW_UP_ALLOWED),
                "题目：" + q.getQuestionText() + "\n参考要点：" + Objects.toString(q.getKeyPoints(), "（无）")
                        + "\n\n对话：\n" + dialogueFor(transcript, idx),
                0.3);
        int score = Math.max(1, Math.min(5, eval.path("score").asInt(1)));
        List<InterviewQuestionScore> scores = new ArrayList<>(readScores(s));
        scores.removeIf(sc -> sc.getQuestionIndex() == idx);
        scores.add(new InterviewQuestionScore(idx, score, eval.path("comment").asText("")));
        s.setQuestionScores(write(scores));

        String followUp = eval.path("follow_up").asText("").strip();
        if (!s.isFollowUpUsed() && !followUp.isEmpty()) {
            s.setFollowUpUsed(true);
            transcript.add(new InterviewMessage("interviewer", followUp, idx));
            s.setTranscript(write(transcript));
        } else if (idx + 1 < ids.size()) {
            int next = idx + 1;
            String nextText = interviewQuestionRepository.findById(ids.get(next))
                    .map(InterviewQuestion::getQuestionText).orElse("（题目已被删除，请直接回答\"跳过\"）");
            String transition = TRANSITIONS[ThreadLocalRandom.current().nextInt(TRANSITIONS.length)];
            transcript.add(new InterviewMessage("interviewer",
                    transition + "\n\n第 " + (next + 1) + " 题：" + nextText, next));
            s.setCurrentIndex(next);
            s.setFollowUpUsed(false);
            s.setTranscript(write(transcript));
        } else {
            s.setTranscript(write(transcript));
            complete(s, categoryName);
        }
        sessionRepository.save(s);
        return view(s);
    }

    /** 提前结束：按已经答过的题出总评。 */
    public InterviewSessionView finish(Long sessionId, Long userId) {
        InterviewSession s = load(sessionId, userId);
        if (s.getStatus() == InterviewSession.Status.in_progress) {
            complete(s, categoryName(s.getCategoryId()));
            sessionRepository.save(s);
        }
        return view(s);
    }

    private void complete(InterviewSession s, String categoryName) {
        List<InterviewMessage> transcript = new ArrayList<>(readTranscript(s));
        List<InterviewQuestionScore> scores = readScores(s);
        if (scores.isEmpty()) {
            s.setReport(write(new InterviewReport("这场面试没有作答，所以没有生成评价。准备好了随时可以再来一场。",
                    List.of(), List.of())));
        } else {
            List<Long> ids = readIds(s);
            Map<Long, InterviewQuestion> questions = interviewQuestionRepository.findAllById(ids).stream()
                    .collect(Collectors.toMap(InterviewQuestion::getId, Function.identity()));
            Map<Long, String> skillNames = skillNames(questions.values());
            StringBuilder sb = new StringBuilder();
            for (InterviewQuestionScore sc : scores.stream()
                    .sorted(Comparator.comparingInt(InterviewQuestionScore::getQuestionIndex)).toList()) {
                InterviewQuestion q = questions.get(ids.get(sc.getQuestionIndex()));
                sb.append("第 ").append(sc.getQuestionIndex() + 1).append(" 题（技能：")
                        .append(q == null ? "未知" : skillNames.get(q.getSkillId()))
                        .append("，评分 ").append(sc.getScore()).append("/5）\n")
                        .append(dialogueFor(transcript, sc.getQuestionIndex()))
                        .append("你当时的点评：").append(sc.getComment()).append("\n\n");
            }
            JsonNode r = deepSeekClient.chatJson(REPORT_PROMPT.formatted(categoryName), sb.toString(), 0.5);
            s.setOverallScore(Math.max(0, Math.min(100, r.path("overall_score").asInt(0))));
            s.setReport(write(new InterviewReport(r.path("summary").asText(""),
                    strings(r.path("strengths")), strings(r.path("improvements")))));
        }
        transcript.add(new InterviewMessage("interviewer",
                "好的，今天的面试就到这里，辛苦了！面试评价已经生成，可以在下方查看每道题的点评和参考要点。",
                s.getCurrentIndex()));
        s.setTranscript(write(transcript));
        s.setStatus(InterviewSession.Status.finished);
        s.setFinishedAt(LocalDateTime.now());
    }

    /** 从权重最高的 TOP_SKILL_POOL 个（有面试题的）技能里随机挑技能，每个技能抽一道；技能不够就同一技能多抽。 */
    private List<InterviewQuestion> pickQuestions(Long categoryId) {
        List<Long> skillIds = jobSkillRepository.findByCategoryIdOrderByWeightDesc(categoryId).stream()
                .map(JobSkill::getSkillId).toList();
        Map<Long, List<InterviewQuestion>> bySkill = interviewQuestionRepository.findBySkillIdIn(skillIds).stream()
                .collect(Collectors.groupingBy(InterviewQuestion::getSkillId));
        List<Long> pool = new ArrayList<>(skillIds.stream().filter(bySkill::containsKey).limit(TOP_SKILL_POOL).toList());
        Collections.shuffle(pool);
        bySkill.values().forEach(Collections::shuffle);

        List<InterviewQuestion> picked = new ArrayList<>();
        Map<Long, Integer> used = new HashMap<>();
        while (picked.size() < QUESTIONS_PER_SESSION) {
            boolean added = false;
            for (Long skillId : pool) {
                if (picked.size() >= QUESTIONS_PER_SESSION) break;
                List<InterviewQuestion> qs = bySkill.get(skillId);
                int n = used.getOrDefault(skillId, 0);
                if (n < qs.size()) {
                    picked.add(qs.get(n));
                    used.put(skillId, n + 1);
                    added = true;
                }
            }
            if (!added) break; // 题库总共不足 5 道
        }
        return picked;
    }

    private InterviewSessionView view(InterviewSession s) {
        InterviewSessionView v = new InterviewSessionView();
        List<Long> ids = readIds(s);
        v.setId(s.getId());
        v.setCategoryId(s.getCategoryId());
        v.setCategoryName(categoryName(s.getCategoryId()));
        v.setStatus(s.getStatus().name());
        v.setCurrentIndex(s.getCurrentIndex());
        v.setTotalQuestions(ids.size());
        v.setMessages(readTranscript(s));
        v.setCreatedAt(s.getCreatedAt());
        if (s.getStatus() == InterviewSession.Status.finished) {
            v.setOverallScore(s.getOverallScore());
            if (s.getReport() != null) v.setReport(objectMapper.readValue(s.getReport(), InterviewReport.class));
            Map<Integer, InterviewQuestionScore> scores = readScores(s).stream()
                    .collect(Collectors.toMap(InterviewQuestionScore::getQuestionIndex, Function.identity(), (a, b) -> b));
            Map<Long, InterviewQuestion> questions = interviewQuestionRepository.findAllById(ids).stream()
                    .collect(Collectors.toMap(InterviewQuestion::getId, Function.identity()));
            Map<Long, String> skillNames = skillNames(questions.values());
            List<InterviewSessionView.QuestionReview> reviews = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                InterviewQuestion q = questions.get(ids.get(i));
                if (q == null) continue;
                InterviewSessionView.QuestionReview r = new InterviewSessionView.QuestionReview();
                r.setIndex(i);
                r.setQuestion(q.getQuestionText());
                r.setSkillName(skillNames.get(q.getSkillId()));
                r.setKeyPoints(q.getKeyPoints());
                InterviewQuestionScore sc = scores.get(i);
                if (sc != null) {
                    r.setScore(sc.getScore());
                    r.setComment(sc.getComment());
                }
                reviews.add(r);
            }
            v.setQuestionReviews(reviews);
        }
        return v;
    }

    private InterviewSession load(Long sessionId, Long userId) {
        InterviewSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "这场面试不存在"));
        if (!s.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "这不是你的面试记录");
        }
        return s;
    }

    /** 某一道题上的对话（含追问），格式化成给 AI 看的文本。 */
    private String dialogueFor(List<InterviewMessage> transcript, int questionIndex) {
        StringBuilder sb = new StringBuilder();
        for (InterviewMessage m : transcript) {
            if (m.getQuestionIndex() != questionIndex) continue;
            if ("interviewer".equals(m.getRole())) {
                sb.append("面试官：");
            } else if (m.getSeconds() != null) {
                sb.append("候选人（限时 ").append(m.getTimeLimit()).append(" 秒，用时 ").append(m.getSeconds()).append(" 秒")
                        .append(Boolean.TRUE.equals(m.getTimedOut()) ? "，时间到被自动提交" : "").append("）：");
            } else {
                sb.append("候选人：");
            }
            sb.append(m.getContent()).append("\n");
        }
        return sb.toString();
    }

    private String categoryName(Long categoryId) {
        return jobCategoryRepository.findById(categoryId).map(JobCategory::getName).orElse("目标");
    }

    private Map<Long, String> skillNames(Collection<InterviewQuestion> questions) {
        return skillRepository.findAllById(questions.stream().map(InterviewQuestion::getSkillId).distinct().toList())
                .stream().collect(Collectors.toMap(Skill::getId, Skill::getName));
    }

    private List<String> strings(JsonNode arr) {
        List<String> out = new ArrayList<>();
        if (arr != null && arr.isArray()) arr.forEach(n -> out.add(n.asText()));
        return out;
    }

    private List<Long> readIds(InterviewSession s) {
        return Arrays.asList(objectMapper.readValue(s.getQuestionIds(), Long[].class));
    }

    private List<InterviewMessage> readTranscript(InterviewSession s) {
        return Arrays.asList(objectMapper.readValue(s.getTranscript(), InterviewMessage[].class));
    }

    private List<InterviewQuestionScore> readScores(InterviewSession s) {
        if (s.getQuestionScores() == null) return List.of();
        return Arrays.asList(objectMapper.readValue(s.getQuestionScores(), InterviewQuestionScore[].class));
    }

    private String write(Object o) {
        return objectMapper.writeValueAsString(o);
    }
}
