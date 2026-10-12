package com.skillradar.controller;

import com.skillradar.dto.QuestionView;
import com.skillradar.dto.WrongQuestionView;
import com.skillradar.service.WrongQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 错题本：回顾 + 间隔复习出题，判分/回写逻辑在 QuizService + WrongQuestionService。 */
@RestController
@RequestMapping("/api/users/{userId}/wrong-questions")
@RequiredArgsConstructor
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;

    @GetMapping
    public List<WrongQuestionView> list(@PathVariable Long userId) {
        return wrongQuestionService.listForUser(userId);
    }

    /** 错题复习出题：带 skillId 出该技能的全部错题，不带就出今天到期的。提交走 POST /api/quiz-attempts，mode=review。 */
    @GetMapping("/review")
    public List<QuestionView> review(@PathVariable Long userId, @RequestParam(required = false) Long skillId) {
        return wrongQuestionService.reviewQuestions(userId, skillId);
    }
}
