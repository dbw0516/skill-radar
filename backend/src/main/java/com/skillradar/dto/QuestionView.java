package com.skillradar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 给用户作答用的题目视图——特意不带 correctIndex，判分只在服务端做，
 * 不然正确答案会跟着 GET 请求一起发到前端，等于把答案泄露给用户。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionView {
    private Long id;
    private String questionText;
    private String options; // 原始 JSON 数组文本，前端 JSON.parse
}
