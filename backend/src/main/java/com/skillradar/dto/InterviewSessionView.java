package com.skillradar.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 一场模拟面试给前端的视图。面试进行中不带任何参考要点；
 * 结束后 questionReviews 才把每题的参考要点、AI 评分和点评摊开。
 */
@Data
@NoArgsConstructor
public class InterviewSessionView {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String status;              // in_progress / finished
    private int currentIndex;
    private int totalQuestions;
    private List<InterviewMessage> messages;
    private Integer overallScore;       // 0~100，结束后才有
    private InterviewReport report;
    private List<QuestionReview> questionReviews;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    public static class QuestionReview {
        private int index;
        private String question;
        private String skillName;
        private String keyPoints;
        private Integer score;          // 1~5，没答到的题为 null
        private String comment;
    }
}
