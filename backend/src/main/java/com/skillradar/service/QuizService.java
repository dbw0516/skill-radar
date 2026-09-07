package com.skillradar.service;

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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ④资料与测评匹配引擎里"测评"这一半：出题（列题，不带答案）+ 判分 + 回写画像。
 * passed 时把 user_skills.status 改成 quiz_verified——这就是设计文档反复提到的"回写闭环"，
 * 下一次调用 GapAnalysisService 就会看到这个技能变成已掌握。
 */
@Service
@RequiredArgsConstructor
public class QuizService {

    private static final double PASS_THRESHOLD = 0.6;

    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserSkillRepository userSkillRepository;

    public List<QuestionView> questionsFor(Long skillId) {
        return questionRepository.findBySkillId(skillId).stream()
                .map(q -> new QuestionView(q.getId(), q.getQuestionText(), q.getOptions()))
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
            if (q != null && q.getCorrectIndex().equals(a.getSelectedIndex())) {
                correct++;
            }
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
}
