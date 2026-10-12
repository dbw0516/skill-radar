package com.skillradar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 面试结束时 AI 生成的总评，以 JSON 存在 interview_sessions.report。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewReport {
    private String summary;
    private List<String> strengths;
    private List<String> improvements;
}
