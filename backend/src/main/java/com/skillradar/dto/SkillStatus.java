package com.skillradar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 个人中心"技能自评"清单里的一行：这个技能，用户现在是什么状态。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillStatus {
    private Long skillId;
    private String name;
    private String domain;
    private String status; // null=未掌握 / self_reported / quiz_verified
}
