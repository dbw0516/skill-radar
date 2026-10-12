package com.skillradar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 一场 AI 模拟面试。对话记录、每题评分、总评都以 JSON 文本存，由 InterviewService 读写——
 * 这几样只在这一场面试内部用，不需要单独拆表查询。
 */
@Entity
@Table(name = "interview_sessions")
@Data
@NoArgsConstructor
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "question_ids", nullable = false, columnDefinition = "json")
    private String questionIds;

    @Column(name = "current_index", nullable = false)
    private Integer currentIndex = 0;

    @Column(name = "follow_up_used", nullable = false)
    private boolean followUpUsed = false;

    @Column(nullable = false, columnDefinition = "json")
    private String transcript;

    @Column(name = "question_scores", columnDefinition = "json")
    private String questionScores;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Status status = Status.in_progress;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(columnDefinition = "json")
    private String report;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public enum Status { in_progress, finished }
}
