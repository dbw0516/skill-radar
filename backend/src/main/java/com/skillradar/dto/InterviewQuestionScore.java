package com.skillradar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** AI 面试官对一道题（含追问）的评分：score 1~5，comment 面试结束后展示给候选人。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestionScore {
    private int questionIndex;
    private int score;
    private String comment;
}
