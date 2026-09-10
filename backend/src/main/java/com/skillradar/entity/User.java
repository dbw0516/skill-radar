package com.skillradar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(length = 64)
    private String location;

    @Column(name = "target_location", length = 64)
    private String targetLocation;
}
