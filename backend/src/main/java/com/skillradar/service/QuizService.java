package com.skillradar.service;

import tools.jackson.databind.ObjectMapper;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ④资料与测评匹配引擎里"测评"这一半：出题（列题，不带答案）+ 判分 + 回写画像。
 * passed 时把 user_skills.status 改成 quiz_verified——这就是设计文档反复提到的"回写闭环"，
 * 下一次调用 GapAnalysisService 就会看到这个技能变成已掌握。
 *
 * 三种题型判分方式不同：single_choice 精确比对下标；fill_blank 把提交文本和
 * accepted_answers 都归一化（去首尾空格+转小写）后比对，任一候选命中即算对；
 * short_answer 开放式答案没法用字符串匹配，交给 ShortAnswerGradingService 调用本地 AI 判分。
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

    @Transactional
    public QuizResult submit(QuizSubmitRequest req) {
        if (req.getAnswers() == null || req.getAnswers().isEmpty()) {
            throw new IllegalArgumentException("没有作答内容");
        }
        List<Long> questionIds = req.getAnswers().stream().map(QuizSubmitRequest.Answer::getQuestionId).toList();
        Map<Long, Question> questions = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        int correct = 0;
        for (QuizSubmitRequest.Answer a : req.getAnswers()) {
            Question q = questions.get(a.getQuestionId());
            if (q == null) {
                continue;
            }
            boolean ok = isCorrect(q, a);
            if (ok) {
                correct++;
            }
            // 错题本：答对了就把之前的错题记录清掉，答错了记一条/累加次数，见 WrongQuestionService。
            wrongQuestionService.record(req.getUserId(), q, ok, formatSubmittedAnswer(q, a));
        }
        int total = req.getAnswers().size();
        boolean passed = total > 0 && ((double) correct / total) >= PASS_THRESHOLD;

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(req.getUserId());
        attempt.setSkillId(req.getSkillId());
        attempt.setCorrectCount(correct);
        attempt.setTotalCount(total);
        attempt.setPassed(passed);
        quizAttemptRepository.save(attempt);

        if (passed) {
            UserSkill us = userSkillRepository.findByUserIdAndSkillId(req.getUserId(), req.getSkillId())
                    .orElse(new UserSkill(req.getUserId(), req.getSkillId(), UserSkill.Status.quiz_verified));
            us.setStatus(UserSkill.Status.quiz_verified);
            userSkillRepository.save(us);
        }

        return new QuizResult(correct, total, passed);
    }

    private boolean isCorrect(Question q, QuizSubmitRequest.Answer a) {
        return switch (q.getType()) {
            case single_choice -> q.getCorrectIndex() != null && q.getCorrectIndex().equals(a.getSelectedIndex());
            case fill_blank -> matchesAcceptedAnswer(q.getAcceptedAnswers(), a.getAnswerText());
            case short_answer -> gradingService.isCorrect(q.getQuestionText(), q.getReferenceAnswer(), a.getAnswerText());
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
