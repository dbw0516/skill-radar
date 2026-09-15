package com.skillradar.dto;

import com.skillradar.entity.Question;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 给用户作答用的题目视图——特意不带 correctIndex/acceptedAnswers，判分只在服务端做，
 * 不然正确答案会跟着 GET 请求一起发到前端，等于把答案泄露给用户。
 * referenceAnswer 是例外：short_answer 本来就没法自动判分，参考答案本就要展示给用户
 * 自己核对，和 interview_questions.key_points 是同一个道理。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionView {
    private Long id;
    private Question.QuestionType type;
    private String questionText;
    private String options; // 原始 JSON 数组文本，前端 JSON.parse；仅 single_choice 有值
    private String referenceAnswer; // 仅 short_answer 有值，前端"查看参考答案"后展示
}
