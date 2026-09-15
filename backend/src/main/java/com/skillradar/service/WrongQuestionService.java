package com.skillradar.service;

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

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 错题本：QuizService 判分时每道题调一次 record，答错就记/累加，答对就把之前的记录清掉——
 * 表里当前"存在的行"就是用户现在还没订正的题，见 WrongQuestion 实体上的注释。
 */
@Service
@RequiredArgsConstructor
public class WrongQuestionService {

    private final WrongQuestionRepository wrongQuestionRepository;
    private final QuestionRepository questionRepository;
    private final SkillRepository skillRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void record(Long userId, Question question, boolean correct, String submittedAnswerDisplay) {
        if (correct) {
            wrongQuestionRepository.findByUserIdAndQuestionId(userId, question.getId())
                    .ifPresent(wrongQuestionRepository::delete);
            return;
        }
        WrongQuestion wq = wrongQuestionRepository.findByUserIdAndQuestionId(userId, question.getId())
                .orElse(new WrongQuestion(userId, question.getId(), submittedAnswerDisplay));
        wq.setLastWrongAnswer(submittedAnswerDisplay);
        wq.setWrongCount(wq.getWrongCount() == null ? 1 : wq.getWrongCount() + 1);
        wrongQuestionRepository.save(wq);
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
                            wq.getLastWrongAt());
                })
                .toList();
    }

    private String correctAnswerText(Question q) {
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
