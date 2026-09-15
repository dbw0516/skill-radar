package com.skillradar.dto;

import com.skillradar.entity.Question;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 错题本一行：跟 QuestionView（答题时用）不同，这里是事后回顾，
 * correctAnswer 会把正确答案摊开给用户看——不存在"泄题"的问题，判分早就结束了。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WrongQuestionView {
    private Long questionId;
    private Long skillId;
    private String skillName;
    private Question.QuestionType type;
    private String questionText;
    private String options; // 原始 JSON 数组文本，仅 single_choice 有值，前端 JSON.parse
    private String yourAnswer;
    private String correctAnswer;
    private int wrongCount;
    private LocalDateTime lastWrongAt;
}
