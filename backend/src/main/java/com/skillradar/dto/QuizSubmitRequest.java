package com.skillradar.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuizSubmitRequest {
    private Long userId;
    private Long skillId;   // quiz 模式必填；review 模式可空（混合多个技能的到期错题）
    private String mode;    // quiz（默认）/ review（错题复习：不认证技能，只推进复习进度）
    private List<Answer> answers;

    @Data
    public static class Answer {
        private Long questionId;
        private Integer selectedIndex; // single_choice
        private String answerText;     // fill_blank / short_answer
    }
}
