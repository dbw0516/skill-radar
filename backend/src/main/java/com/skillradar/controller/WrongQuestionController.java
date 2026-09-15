package com.skillradar.controller;

import com.skillradar.dto.WrongQuestionView;
import com.skillradar.service.WrongQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 错题本：回顾这个用户目前还没订正的题，判分/回写逻辑在 QuizService + WrongQuestionService。 */
@RestController
@RequestMapping("/api/users/{userId}/wrong-questions")
@RequiredArgsConstructor
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;

    @GetMapping
    public List<WrongQuestionView> list(@PathVariable Long userId) {
        return wrongQuestionService.listForUser(userId);
    }
}
