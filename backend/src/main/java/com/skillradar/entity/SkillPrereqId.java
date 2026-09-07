package com.skillradar.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillPrereqId implements Serializable {
    private Long skillId;
    private Long prereqSkillId;
}
