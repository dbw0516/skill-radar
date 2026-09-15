package com.skillradar.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuizSubmitRequest {
    private Long userId;
    private Long skillId;
    private List<Answer> answers;

    @Data
    public static class Answer {
        private Long questionId;
        private Integer selectedIndex; // single_choice
        private String answerText;     // fill_blank / short_answer
    }
}
