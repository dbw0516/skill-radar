package com.skillradar.service;

import com.skillradar.dto.QuestionView;
import com.skillradar.dto.WrongQuestionView;
import com.skillradar.entity.Question;
import com.skillradar.entity.Skill;
import com.skillradar.entity.WrongQuestion;
import com.skillradar.repository.QuestionRepository;
import com.skillradar.repository.SkillRepository;
import com.skillradar.repository.WrongQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 错题本 + 间隔复习：QuizService 判分时每道题调一次 record。
 *  - 答错：进错题本（或重置），复习阶段归 0，明天到期；
 *  - 到期后答对：阶段 +1，下次分别隔 3 天、7 天；第 3 次到期答对就移出错题本；
 *  - 还没到期就答对：不推进阶段——同一天连刷三遍不算巩固，间隔复习的意义就在"隔一段时间还记得"。
 */
@Service
@RequiredArgsConstructor
public class WrongQuestionService {

    private final WrongQuestionRepository wrongQuestionRepository;
    private final QuestionRepository questionRepository;
    private final SkillRepository skillRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 复习间隔：阶段 0（刚答错）→ 1 天后；阶段 1 → 3 天后；阶段 2 → 7 天后；阶段 2 到期再答对就移出。 */
    private static final int[] REVIEW_INTERVAL_DAYS = {1, 3, 7};
    private static final int STAGES_TO_GRADUATE = REVIEW_INTERVAL_DAYS.length;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M月d日");
    private static final int MAX_REVIEW_QUESTIONS = 10;

    /**
     * 回写一道题的对错，返回一句给用户看的复习状态说明（这道题不在错题本、答对了也没变化时返回 null）。
     */
    @Transactional
    public String record(Long userId, Question question, boolean correct, String submittedAnswerDisplay) {
        LocalDateTime now = LocalDateTime.now();
        Optional<WrongQuestion> existing = wrongQuestionRepository.findByUserIdAndQuestionId(userId, question.getId());
        if (correct) {
            if (existing.isEmpty()) {
                return null;
            }
            WrongQuestion wq = existing.get();
            if (wq.getNextReviewAt() != null && wq.getNextReviewAt().isAfter(now)) {
                return "答对了！按复习计划 " + wq.getNextReviewAt().format(DATE) + " 再来巩固一次";
            }
            int stage = wq.getReviewStage() + 1;
            if (stage >= STAGES_TO_GRADUATE) {
                wrongQuestionRepository.delete(wq);
                return "已经隔天巩固 " + STAGES_TO_GRADUATE + " 次，移出错题本 🎉";
            }
            wq.setReviewStage(stage);
            wq.setNextReviewAt(now.plusDays(REVIEW_INTERVAL_DAYS[stage]));
            wrongQuestionRepository.save(wq);
            return "巩固进度 " + stage + "/" + STAGES_TO_GRADUATE + "，" + REVIEW_INTERVAL_DAYS[stage] + " 天后再复习";
        }

        WrongQuestion wq = existing.orElseGet(() -> new WrongQuestion(userId, question.getId(), submittedAnswerDisplay));
        if (existing.isPresent()) {
            wq.setWrongCount(wq.getWrongCount() == null ? 1 : wq.getWrongCount() + 1);
        }
        wq.setLastWrongAnswer(submittedAnswerDisplay);
        wq.setLastWrongAt(now);
        wq.setReviewStage(0);
        wq.setNextReviewAt(now.plusDays(REVIEW_INTERVAL_DAYS[0]));
        wrongQuestionRepository.save(wq);
        return existing.isPresent() ? "又答错了，复习进度清零，明天再复习" : "已加入错题本，明天提醒你复习";
    }

    public long dueCount(Long userId) {
        return wrongQuestionRepository.countByUserIdAndNextReviewAtLessThanEqual(userId, LocalDateTime.now());
    }

    /**
     * 错题复习要出的题：指定了 skillId 就出这个技能的全部错题（到期的排前面），
     * 没指定就出所有技能里今天到期的。最多 10 道，跟正式测评一样一次答得完。
     */
    public List<QuestionView> reviewQuestions(Long userId, Long skillId) {
        LocalDateTime now = LocalDateTime.now();
        List<WrongQuestion> rows = wrongQuestionRepository.findByUserIdOrderByLastWrongAtDesc(userId);
        Map<Long, Question> questionsById = questionRepository
                .findAllById(rows.stream().map(WrongQuestion::getQuestionId).toList()).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        return rows.stream()
                .filter(wq -> questionsById.containsKey(wq.getQuestionId()))
                .filter(wq -> skillId != null
                        ? skillId.equals(questionsById.get(wq.getQuestionId()).getSkillId())
                        : !wq.getNextReviewAt().isAfter(now))
                .sorted(Comparator.comparing(WrongQuestion::getNextReviewAt))
                .limit(MAX_REVIEW_QUESTIONS)
                .map(wq -> {
                    Question q = questionsById.get(wq.getQuestionId());
                    return new QuestionView(q.getId(), q.getType(), q.getQuestionText(), q.getOptions(),
                            q.getType() == Question.QuestionType.short_answer ? q.getReferenceAnswer() : null);
                })
                .toList();
    }

    public List<WrongQuestionView> listForUser(Long userId) {
        List<WrongQuestion> rows = wrongQuestionRepository.findByUserIdOrderByLastWrongAtDesc(userId);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, Question> questionsById = questionRepository
                .findAllById(rows.stream().map(WrongQuestion::getQuestionId).toList()).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        Map<Long, Skill> skillsById = skillRepository.findAll().stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));

        return rows.stream()
                .filter(wq -> questionsById.containsKey(wq.getQuestionId()))
                .map(wq -> {
                    Question q = questionsById.get(wq.getQuestionId());
                    Skill skill = skillsById.get(q.getSkillId());
                    return new WrongQuestionView(
                            q.getId(),
                            q.getSkillId(),
                            skill != null ? skill.getName() : null,
                            q.getType(),
                            q.getQuestionText(),
                            q.getType() == Question.QuestionType.single_choice ? q.getOptions() : null,
                            wq.getLastWrongAnswer(),
                            correctAnswerText(q),
                            wq.getWrongCount() == null ? 1 : wq.getWrongCount(),
                            wq.getLastWrongAt(),
                            wq.getReviewStage(),
                            wq.getNextReviewAt(),
                            !wq.getNextReviewAt().isAfter(LocalDateTime.now()));
                })
                .toList();
    }

    /** 正确答案的展示文本：选择题给选项文字、填空题列出可接受答案、简答题给参考要点。QuizService 交卷结果也用。 */
    public String correctAnswerText(Question q) {
        try {
            return switch (q.getType()) {
                case single_choice -> {
                    if (q.getOptions() == null || q.getCorrectIndex() == null) yield null;
                    String[] opts = objectMapper.readValue(q.getOptions(), String[].class);
                    yield (q.getCorrectIndex() >= 0 && q.getCorrectIndex() < opts.length)
                            ? opts[q.getCorrectIndex()] : null;
                }
                case fill_blank -> {
                    if (q.getAcceptedAnswers() == null) yield null;
                    String[] accepted = objectMapper.readValue(q.getAcceptedAnswers(), String[].class);
                    yield String.join(" / ", accepted);
                }
                case short_answer -> q.getReferenceAnswer();
            };
        } catch (Exception e) {
            return null;
        }
    }
}
