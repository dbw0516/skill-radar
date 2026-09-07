package com.skillradar.controller;

import com.skillradar.dto.QuizResult;
import com.skillradar.dto.QuizSubmitRequest;
import com.skillradar.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/api/quiz-attempts")
    public QuizResult submit(@RequestBody QuizSubmitRequest request) {
        return quizService.submit(request);
    }
}
