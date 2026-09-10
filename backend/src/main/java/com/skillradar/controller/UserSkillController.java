package com.skillradar.controller;

import com.skillradar.dto.SkillIdsRequest;
import com.skillradar.dto.SkillStatus;
import com.skillradar.service.UserSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 个人中心的"技能自评"清单，对应设计文档「用户画像怎么建」一节的自评那一半。 */
@RestController
@RequestMapping("/api/users/{userId}/skills")
@RequiredArgsConstructor
public class UserSkillController {

    private final UserSkillService userSkillService;

    @GetMapping
    public List<SkillStatus> list(@PathVariable Long userId) {
        return userSkillService.listForUser(userId);
    }

    @PutMapping
    public List<SkillStatus> setSelfReported(@PathVariable Long userId, @RequestBody SkillIdsRequest req) {
        userSkillService.setSelfReported(userId, req.getSkillIds());
        return userSkillService.listForUser(userId);
    }
}
