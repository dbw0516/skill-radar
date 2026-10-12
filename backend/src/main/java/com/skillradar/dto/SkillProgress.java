package com.skillradar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 目标岗位里单个技能的进度。status：verified（测评认证）/ self（自评掌握）/ learning（学了一部分）/ none。
 * score 0~100：掌握了就是 100；没掌握时按"历史最好正确率 × 60% + 资料阅读比例 × 30%"给部分分，封顶 90。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillProgress {
    private Long skillId;
    private String name;
    private int weight;            // 0~100，同类 JD 中的提及比例
    private int score;
    private String status;
    private Integer bestAccuracy;  // 0~100，没测过为 null
    private int resourcesViewed;
    private int resourcesTotal;
}
