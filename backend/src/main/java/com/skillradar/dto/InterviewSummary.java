package com.skillradar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 面试历史列表的一行。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSummary {
    private Long id;
    private String categoryName;
    private String status;
    private Integer overallScore;
    private int answeredQuestions;
    private int totalQuestions;
    private LocalDateTime createdAt;
}
