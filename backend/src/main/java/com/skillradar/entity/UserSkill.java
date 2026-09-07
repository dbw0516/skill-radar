package com.skillradar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户技能画像（细粒度层）。status 区分自评 / 测评认证——
 * 测评通过时由 QuizService 把这里的 status 改成 quiz_verified，这就是设计文档里反复提到的"回写画像"。
 */
@Entity
@Table(name = "user_skills")
@IdClass(UserSkillId.class)
@Data
@NoArgsConstructor
public class UserSkill {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "skill_id")
    private Long skillId;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private Status status = Status.self_reported;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public enum Status { self_reported, quiz_verified }

    public UserSkill(Long userId, Long skillId, Status status) {
        this.userId = userId;
        this.skillId = skillId;
        this.status = status;
    }
}
