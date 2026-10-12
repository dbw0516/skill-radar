package com.skillradar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 开放式面试题库（interview_questions），AI 模拟面试从这里抽题。 */
@Entity
@Table(name = "interview_questions")
@Data
@NoArgsConstructor
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(name = "question_text", nullable = false, columnDefinition = "text")
    private String questionText;

    @Column(name = "key_points", columnDefinition = "text")
    private String keyPoints;

    private String source;
}
