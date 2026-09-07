package com.skillradar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathStage {
    private int stage;          // 阶段序号，从 1 开始
    private List<GapItem> skills;
}
