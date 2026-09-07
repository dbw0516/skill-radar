package com.skillradar.controller;

import com.skillradar.dto.LearningPathStage;
import com.skillradar.service.LearningPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;

    @GetMapping("/api/learning-path")
    public List<LearningPathStage> learningPath(@RequestParam Long categoryId,
                                                 @RequestParam(defaultValue = "1") Long userId) {
        return learningPathService.plan(categoryId, userId);
    }
}
