package com.skillradar.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSkillId implements Serializable {
    private Long userId;
    private Long skillId;
}
