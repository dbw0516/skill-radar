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

    /** 取消某个技能的已掌握状态——自评、测评认证过的都能取消，跟 PUT 的批量自评表单是两回事。 */
    @DeleteMapping("/{skillId}")
    public List<SkillStatus> remove(@PathVariable Long userId, @PathVariable Long skillId) {
        userSkillService.remove(userId, skillId);
        return userSkillService.listForUser(userId);
    }
}
