package com.skillradar.dto;

import lombok.Data;

/**
 * 模拟面试的请求体：开始一场（categoryId 可空，默认用户的目标岗位）/ 回答一句 / 提前结束。
 * 限时模式下回答时还带上 seconds（作答用时）、timeLimit（作答限时，秒）、timedOut（时间到自动提交）。
 */
@Data
public class InterviewRequest {
    private Long userId;
    private Long categoryId;
    private String answer;
    private Integer seconds;
    private Integer timeLimit;
    private Boolean timedOut;
}
