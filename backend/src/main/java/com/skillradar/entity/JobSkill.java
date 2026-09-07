package com.skillradar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 岗位类别所需技能（聚合层）：weight = 该类别下提及该技能的 JD 占比，②差距分析排序用。
 */
@Entity
@Table(name = "job_skills")
@IdClass(JobSkillId.class)
@Data
@NoArgsConstructor
public class JobSkill {

    @Id
    @Column(name = "category_id")
    private Long categoryId;

    @Id
    @Column(name = "skill_id")
    private Long skillId;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal weight;
}
