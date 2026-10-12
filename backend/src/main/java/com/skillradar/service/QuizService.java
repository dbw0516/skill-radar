package com.skillradar.service;

import tools.jackson.databind.ObjectMapper;
import com.skillradar.dto.Badge;
import com.skillradar.dto.LearningProgress;
import com.skillradar.dto.QuestionResult;
import com.skillradar.dto.QuestionView;
import com.skillradar.dto.QuizResult;
import com.skillradar.dto.QuizSubmitRequest;
import com.skillradar.entity.Question;
import com.skillradar.entity.QuizAttempt;
import com.skillradar.entity.UserSkill;
import com.skillradar.repository.QuestionRepository;
import com.skillradar.repository.QuizAttemptRepository;
import com.skillradar.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ④资料与测评匹配引擎里"测评"这一半：出题（列题，不带答案）+ 判分 + 回写画像。
 * passed 时把 user_skills.status 改成 quiz_verified——这就是设计文档反复提到的"回写闭环"，
 * 下一次调用 GapAnalysisService 就会看到这个技能变成已掌握。
 *
 * 三种题型判分方式不同：single_choice 精确比对下标；fill_blank 把提交文本和
 * accepted_answers 都归一化（去首尾空格+转小写）后比对，任一候选命中即算对；
 * short_answer 开放式答案没法用字符串匹配，交给 ShortAnswerGradingService 调用 DeepSeek 线上 AI 判分。
 *
 * 交卷结果逐题返回对错、正确答案和 AI 点评；顺带带上激励反馈（进度变化、可投岗位变化、新徽章、鼓励语），
 * 由 ProgressService 在判分前后各算一次得出。mode=review 是错题复习：只推进间隔复习进度，不认证技能。
 */
@Service
@RequiredArgsConstructor
public class QuizService {

    private static final double PASS_THRESHOLD = 0.6;
    // 有些技能题量差异很大（面试题批量转来的能到一两百道），一次测评封顶抽这么多道，
    // 不然"职业素养与求职技巧"这种题库能列出上百道题，没法一次答完。
    private static final int MAX_QUESTIONS_PER_ATTEMPT = 10;

    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserSkillRepository userSkillRepository;
    private final ObjectMapper objectMapper;
    private final ShortAnswerGradingService gradingService;
    private final WrongQuestionService wrongQuestionService;
    private final ProgressService progressService;
    private final TransactionTemplate transactionTemplate;

    public List<QuestionView> questionsFor(Long skillId) {
        List<Question> all = questionRepository.findBySkillId(skillId);
        List<Question> chosen = all;
        if (all.size() > MAX_QUESTIONS_PER_ATTEMPT) {
            chosen = new ArrayList<>(all);
            Collections.shuffle(chosen);
            chosen = chosen.subList(0, MAX_QUESTIONS_PER_ATTEMPT);
        }
        return chosen.stream()
                .map(q -> new QuestionView(
                        q.getId(),
                        q.getType(),
                        q.getQuestionText(),
                        q.getOptions(),
                        q.getType() == Question.QuestionType.short_answer ? q.getReferenceAnswer() : null))
                .toList();
    }

    /** 判分进度回调：每判完一道题调一次（可能来自多个线程）。 */
    @FunctionalInterface
    public interface GradingListener {
        void onGraded(int done, int total);
    }

    private record Graded(Question question, QuizSubmitRequest.Answer answer, boolean correct,
                          String feedback, String keyPointsCovered) {}

    public QuizResult submit(QuizSubmitRequest req) {
        return submit(req, (done, total) -> {});
    }

    /**
     * 判分 + 回写。简答题并发调 AI 判分，且放在数据库事务之外——几秒钟的网络调用不该一直占着事务和连接；
     * 全部判完后再在一个事务里写错题本、测评记录、技能认证。
     */
    public QuizResult submit(QuizSubmitRequest req, GradingListener listener) {
        if (req.getUserId() == null) {
            throw new IllegalArgumentException("缺少 userId");
        }
        if (req.getAnswers() == null || req.getAnswers().isEmpty()) {
            throw new IllegalArgumentException("没有作答内容");
        }
        boolean review = "review".equals(req.getMode());
        if (!review && req.getSkillId() == null) {
            throw new IllegalArgumentException("缺少 skillId");
        }
        LearningProgress before = progressService.progress(req.getUserId());

        List<Long> questionIds = req.getAnswers().stream().map(QuizSubmitRequest.Answer::getQuestionId).toList();
        Map<Long, Question> questions = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        List<QuizSubmitRequest.Answer> answered = req.getAnswers().stream()
                .filter(a -> questions.containsKey(a.getQuestionId())).toList();
        if (answered.isEmpty()) {
            throw new IllegalArgumentException("题目不存在");
        }

        List<Graded> graded = gradeAll(answered, questions, listener);
        QuizResult result = transactionTemplate.execute(status -> persist(req, review, graded));
        fillEncouragement(result, before, progressService.progress(req.getUserId()));
        return result;
    }

    private List<Graded> gradeAll(List<QuizSubmitRequest.Answer> answered, Map<Long, Question> questions,
                                  GradingListener listener) {
        int total = answered.size();
        AtomicInteger done = new AtomicInteger();
        List<CompletableFuture<Graded>> futures = new ArrayList<>();
        for (QuizSubmitRequest.Answer a : answered) {
            Question q = questions.get(a.getQuestionId());
            CompletableFuture<Graded> f = q.getType() == Question.QuestionType.short_answer
                    ? gradingService.gradeAsync(q.getQuestionText(), q.getReferenceAnswer(), a.getAnswerText())
                        .thenApply(g -> new Graded(q, a, g.correct(), g.feedback(), g.keyPointsCovered()))
                    : CompletableFuture.completedFuture(new Graded(q, a, isCorrectLocally(q, a), null, null));
            futures.add(f.whenComplete((g, e) -> {
                if (e == null) listener.onGraded(done.incrementAndGet(), total);
            }));
        }
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            // 把 DeepSeekClient 抛的"AI 服务调用失败……"原样交给前端，而不是包了一层的 CompletionException
            if (e.getCause() instanceof RuntimeException re) throw re;
            throw e;
        }
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private QuizResult persist(QuizSubmitRequest req, boolean review, List<Graded> graded) {
        Long userId = req.getUserId();
        List<QuestionResult> details = new ArrayList<>();
        int correct = 0;
        for (Graded g : graded) {
            if (g.correct()) correct++;
            String display = formatSubmittedAnswer(g.question(), g.answer());
            // 错题本 + 间隔复习：答错进表/重置，到期答对推进复习阶段，见 WrongQuestionService.record
            String note = wrongQuestionService.record(userId, g.question(), g.correct(), display);
            details.add(new QuestionResult(g.question().getId(), g.correct(), display,
                    wrongQuestionService.correctAnswerText(g.question()), g.feedback(), g.keyPointsCovered(), note));
        }
        int total = graded.size();
        boolean passed = !review && ((double) correct / total) >= PASS_THRESHOLD;

        if (review) {
            // 复习题可能来自多个技能，每个技能各记一条复习记录（quiz_attempts.skill_id 不能为空）
            Map<Long, List<Graded>> bySkill = graded.stream().collect(Collectors.groupingBy(
                    g -> g.question().getSkillId(), LinkedHashMap::new, Collectors.toList()));
            bySkill.forEach((skillId, list) -> saveAttempt(userId, skillId,
                    (int) list.stream().filter(Graded::correct).count(), list.size(), false, QuizAttempt.Kind.review));
        } else {
            saveAttempt(userId, req.getSkillId(), correct, total, passed, QuizAttempt.Kind.quiz);
            if (passed) {
                UserSkill us = userSkillRepository.findByUserIdAndSkillId(userId, req.getSkillId())
                        .orElse(new UserSkill(userId, req.getSkillId(), UserSkill.Status.quiz_verified));
                us.setStatus(UserSkill.Status.quiz_verified);
                userSkillRepository.save(us);
            }
        }

        QuizResult result = new QuizResult();
        result.setMode(review ? "review" : "quiz");
        result.setCorrectCount(correct);
        result.setTotalCount(total);
        result.setPassed(passed);
        result.setDetails(details);
        return result;
    }

    private void saveAttempt(Long userId, Long skillId, int correct, int total, boolean passed, QuizAttempt.Kind kind) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(userId);
        attempt.setSkillId(skillId);
        attempt.setCorrectCount(correct);
        attempt.setTotalCount(total);
        attempt.setPassed(passed);
        attempt.setKind(kind);
        quizAttemptRepository.save(attempt);
    }

    private void fillEncouragement(QuizResult result, LearningProgress before, LearningProgress after) {
        Set<String> earnedBefore = before.getBadges().stream()
                .filter(Badge::isEarned).map(Badge::getKey).collect(Collectors.toSet());
        result.setNewBadges(after.getBadges().stream()
                .filter(b -> b.isEarned() && !earnedBefore.contains(b.getKey())).toList());
        result.setProgressBefore(before.getPercent());
        result.setProgressAfter(after.getPercent());
        result.setQualifiedBefore(before.getQualifiedPostings());
        result.setQualifiedAfter(after.getQualifiedPostings());
        result.setPostingsWithSkills(after.getPostingsWithSkills());
        result.setStreakDays(after.getStreakDays());

        String streakPart = after.getStreakDays() > 1 ? "已连续学习 " + after.getStreakDays() + " 天，" : "";
        boolean grew = before.getPercent() != null && after.getPercent() != null && after.getPercent() > before.getPercent();
        String progressPart = grew ? "目标岗位进度 " + before.getPercent() + "% → " + after.getPercent() + "%。" : "";
        boolean moreJobs = before.getQualifiedPostings() != null && after.getQualifiedPostings() != null
                && after.getQualifiedPostings() > before.getQualifiedPostings();
        String jobsPart = moreJobs
                ? "匹配度达标的可投岗位从 " + before.getQualifiedPostings() + " 条增加到 " + after.getQualifiedPostings() + " 条！"
                : "";

        if ("review".equals(result.getMode())) {
            result.setEncouragement(result.getCorrectCount() == result.getTotalCount()
                    ? "复习全对！" + streakPart + "按计划隔几天再巩固一次，这些题就真正是你的了。"
                    : "复习完成，" + result.getTotalCount() + " 道里答对 " + result.getCorrectCount() + " 道。"
                      + streakPart + "答错的明天会再提醒你，多见几次就记住了。");
        } else if (result.isPassed()) {
            result.setEncouragement((grew ? "太棒了！又拿下一项技能，" + progressPart : "通过测评，这项技能已经认证到你的画像里了。")
                    + jobsPart + streakPart + "继续冲！");
        } else if (grew) {
            result.setEncouragement("这次没通过，但" + progressPart + "进步看得见。" + streakPart
                    + "看看下面每道题的点评，下次一定能拿下。");
        } else if (result.getCorrectCount() * 2 >= result.getTotalCount()) {
            result.setEncouragement("差一点就过了！" + streakPart + "看看下面每道题的点评，错题已经放进错题本，明天提醒你复习。");
        } else {
            result.setEncouragement("每一次测评都在帮你找到短板。" + streakPart + "先看看下面的点评和学习资料，再来挑战一次吧。");
        }
    }

    /** 选择题、填空题本地即可判；简答题走 gradeAll 里的 AI 判分，不走这里。 */
    private boolean isCorrectLocally(Question q, QuizSubmitRequest.Answer a) {
        return switch (q.getType()) {
            case single_choice -> q.getCorrectIndex() != null && q.getCorrectIndex().equals(a.getSelectedIndex());
            case fill_blank -> matchesAcceptedAnswer(q.getAcceptedAnswers(), a.getAnswerText());
            case short_answer -> throw new IllegalStateException("简答题应走 AI 判分");
        };
    }

    /** 错题本要存"用户当时答的是什么"——选择题把下标转成选项文字，其他题型本来就是文本，直接存。 */
    private String formatSubmittedAnswer(Question q, QuizSubmitRequest.Answer a) {
        if (q.getType() != Question.QuestionType.single_choice) {
            return a.getAnswerText();
        }
        Integer idx = a.getSelectedIndex();
        if (idx == null || q.getOptions() == null) {
            return null;
        }
        try {
            String[] opts = objectMapper.readValue(q.getOptions(), String[].class);
            return (idx >= 0 && idx < opts.length) ? opts[idx] : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean matchesAcceptedAnswer(String acceptedAnswersJson, String submitted) {
        if (acceptedAnswersJson == null || submitted == null) {
            return false;
        }
        String normalizedSubmitted = submitted.strip().toLowerCase();
        if (normalizedSubmitted.isEmpty()) {
            return false;
        }
        try {
            String[] accepted = objectMapper.readValue(acceptedAnswersJson, String[].class);
            for (String candidate : accepted) {
                if (candidate != null && candidate.strip().toLowerCase().equals(normalizedSubmitted)) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}
