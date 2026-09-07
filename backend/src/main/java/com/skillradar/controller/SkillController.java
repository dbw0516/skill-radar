package com.skillradar.controller;

import com.skillradar.entity.Skill;
import com.skillradar.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 先跑通一条最小闭环：数据库 -> JPA -> Controller -> 前端。
 * 后续按同样的模式加 JobCategoryController、GapAnalysisController 等。
 */
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillRepository skillRepository;

    @GetMapping
    public List<Skill> listSkills() {
        return skillRepository.findAll();
    }
}
