package com.skillradar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 技能节点，对应 database/schema.sql 中的 skills 表。
 * aliases 字段先按原始 JSON 文本存取（简单可用）；
 * 如果后续需要按别名做查询，再升级成真正的 JSON 类型映射。
 */
@Entity
@Table(name = "skills")
@Data
@NoArgsConstructor
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(nullable = false, length = 32)
    private String domain;

    @Column(columnDefinition = "json")
    private String aliases;

    private Integer difficulty;

    @Column(name = "est_hours")
    private Integer estHours;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;
}
