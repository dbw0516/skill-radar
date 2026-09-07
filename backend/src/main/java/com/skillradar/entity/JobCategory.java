package com.skillradar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 岗位类别（稳定层），对应 database/schema.sql 中的 job_categories 表。
 * 具体招聘信息见 JobPosting；两者的拆分逻辑见设计文档「系统怎么应对变化」一节。
 */
@Entity
@Table(name = "job_categories")
@Data
@NoArgsConstructor
public class JobCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "major_id")
    private Long majorId;

    private String description;

    @Column(length = 16)
    private String status;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
