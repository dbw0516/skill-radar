package com.skillradar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模拟面试对话里的一条消息。role：interviewer / candidate。questionIndex：属于第几题（从 0 开始）。
 * 限时模式下候选人的回答还带 seconds（作答用时）、timeLimit（作答限时）、timedOut（是否时间到被自动提交），
 * 不限时的回答这三项为 null。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewMessage {
    private String role;
    private String content;
    private int questionIndex;
    private Integer seconds;
    private Integer timeLimit;
    private Boolean timedOut;

    public InterviewMessage(String role, String content, int questionIndex) {
        this(role, content, questionIndex, null, null, null);
    }
}
