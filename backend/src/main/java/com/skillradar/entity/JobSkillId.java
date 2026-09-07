package com.skillradar.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobSkillId implements Serializable {
    private Long categoryId;
    private Long skillId;
}
