package com.skillradar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ②差距分析引擎的一条输出：一个技能，是否已掌握，权重（同类岗位 JD 中的提及比例）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GapItem {
    private Long skillId;
    private String name;
    private String domain;
    private BigDecimal weight;
    private boolean mastered;
    private String masteredSource; // self_reported / quiz_verified / null（未掌握）
}
