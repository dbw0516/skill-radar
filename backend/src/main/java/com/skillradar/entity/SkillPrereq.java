package com.skillradar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 技能依赖边：skillId 依赖 prereqSkillId（要先学 prereq 再学 skill）。
 * ③学习路径规划引擎做拓扑排序的原始数据。
 */
@Entity
@Table(name = "skill_prereq")
@IdClass(SkillPrereqId.class)
@Data
@NoArgsConstructor
public class SkillPrereq {

    @Id
    @Column(name = "skill_id")
    private Long skillId;

    @Id
    @Column(name = "prereq_skill_id")
    private Long prereqSkillId;
}
