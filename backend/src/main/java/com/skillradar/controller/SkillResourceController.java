package com.skillradar.controller;

import com.skillradar.dto.QuestionView;
import com.skillradar.entity.LearningResource;
import com.skillradar.repository.LearningResourceRepository;
import com.skillradar.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** ④资料与测评匹配引擎里"资料"这一半，加上题目的只读列表（判分见 QuizController）。 */
@RestController
@RequestMapping("/api/skills/{skillId}")
@RequiredArgsConstructor
public class SkillResourceController {

    private final LearningResourceRepository learningResourceRepository;
    private final QuizService quizService;

    @GetMapping("/resources")
    public List<LearningResource> resources(@PathVariable Long skillId) {
        return learningResourceRepository.findBySkillId(skillId);
    }

    @GetMapping("/questions")
    public List<QuestionView> questions(@PathVariable Long skillId) {
        return quizService.questionsFor(skillId);
    }
}
