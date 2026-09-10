package com.skillradar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 用户在①岗位详情页收藏的具体招聘信息。 */
@Entity
@Table(name = "favorites")
@IdClass(FavoriteId.class)
@Data
@NoArgsConstructor
public class Favorite {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "posting_id")
    private Long postingId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Favorite(Long userId, Long postingId) {
        this.userId = userId;
        this.postingId = postingId;
    }
}
