package com.skillradar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 选择题，驱动④在线测评自动判分。options 先按原始 JSON 文本存取（前端 JSON.parse 即可），
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

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @Column(columnDefinition = "json", nullable = false)
    private String options;

    @Column(name = "correct_index", nullable = false)
    private Integer correctIndex;

    private Integer difficulty;
}
