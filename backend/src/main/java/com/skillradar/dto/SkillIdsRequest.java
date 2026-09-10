package com.skillradar.dto;

import lombok.Data;

import java.util.List;

@Data
public class SkillIdsRequest {
    private List<Long> skillIds;
}
