package com.skillradar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 还没接登录/注册，MVP 阶段前端统一用种子数据里的演示账号（id=1）调用接口。
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 64)
    private String nickname;

    @Column(name = "major_id")
    private Long majorId;

    @Column(name = "target_category_id")
    private Long targetCategoryId;
}
