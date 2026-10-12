package com.skillradar.controller;

import com.skillradar.dto.LearningProgress;
import com.skillradar.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 学习进度条 + 打卡/徽章激励，进度按用户当前的目标岗位类别算。 */
@RestController
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/api/users/{userId}/progress")
    public LearningProgress progress(@PathVariable Long userId) {
        return progressService.progress(userId);
    }
}
