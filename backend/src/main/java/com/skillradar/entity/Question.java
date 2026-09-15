package com.skillradar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 测评题，驱动④在线测评自动判分。三种题型共用这一张表，各自只用得到自己的列：
 * single_choice 用 options + correctIndex；fill_blank 用 acceptedAnswers；
 * short_answer 用 referenceAnswer（开放式答案，服务端不判分，仅供前端展示后用户自评）。
 * options/acceptedAnswers 先按原始 JSON 文本存取（前端 JSON.parse 即可），
 * 和 Skill.aliases 一个道理，需要按内容查询时再升级成真正的 JSON 类型映射。
 */
@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private QuestionType type = QuestionType.single_choice;

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Column(columnDefinition = "json")
    private String options;

    @Column(name = "correct_index")
    private Integer correctIndex;

    @Column(name = "accepted_answers", columnDefinition = "json")
    private String acceptedAnswers;

    @Column(name = "reference_answer", columnDefinition = "text")
    private String referenceAnswer;

    private Integer difficulty;

    public enum QuestionType { single_choice, fill_blank, short_answer }
}
