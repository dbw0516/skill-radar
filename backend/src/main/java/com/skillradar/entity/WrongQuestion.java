package com.skillradar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 错题本：表里"存在的行"就代表用户现在还没订正这道题——答对一次就整行删掉，
 * 不需要 status 字段。见 QuizService.submit 里对每道题结果的回写逻辑。
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

    @Column(name = "last_wrong_at", insertable = false, updatable = false)
    private LocalDateTime lastWrongAt;

    public WrongQuestion(Long userId, Long questionId, String lastWrongAnswer) {
        this.userId = userId;
        this.questionId = questionId;
        this.lastWrongAnswer = lastWrongAnswer;
        this.wrongCount = 1;
    }
}
