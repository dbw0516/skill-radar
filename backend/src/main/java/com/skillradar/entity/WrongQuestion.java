package com.skillradar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 错题本 + 间隔复习：表里"存在的行"代表这道题还没巩固好。答错进表（或重置为阶段 0），
 * 之后按 1 天 / 3 天 / 7 天到期复习，到期答对 3 次才整行删掉。规则见 WrongQuestionService.record。
 */
@Entity
@Table(name = "wrong_questions")
@IdClass(WrongQuestionId.class)
@Data
@NoArgsConstructor
public class WrongQuestion {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "last_wrong_answer", columnDefinition = "text")
    private String lastWrongAnswer;

    @Column(name = "wrong_count", nullable = false)
    private Integer wrongCount = 1;

    @Column(name = "first_wrong_at", insertable = false, updatable = false)
    private LocalDateTime firstWrongAt;

    // 不再交给数据库 ON UPDATE 自动维护：答对推进复习阶段时也会更新这一行，那时不能改"最近答错时间"
    @Column(name = "last_wrong_at")
    private LocalDateTime lastWrongAt;

    @Column(name = "review_stage", nullable = false)
    private Integer reviewStage = 0;

    @Column(name = "next_review_at", nullable = false)
    private LocalDateTime nextReviewAt;

    public WrongQuestion(Long userId, Long questionId, String lastWrongAnswer) {
        this.userId = userId;
        this.questionId = questionId;
        this.lastWrongAnswer = lastWrongAnswer;
        this.wrongCount = 1;
    }
}
