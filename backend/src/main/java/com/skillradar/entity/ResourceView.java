package com.skillradar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 用户点开过某条学习资料——计入该技能的部分进度，见 ProgressService。 */
@Entity
@Table(name = "resource_views")
@IdClass(ResourceViewId.class)
@Data
@NoArgsConstructor
public class ResourceView {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "viewed_at", insertable = false, updatable = false)
    private LocalDateTime viewedAt;

    public ResourceView(Long userId, Long resourceId) {
        this.userId = userId;
        this.resourceId = resourceId;
    }
}
