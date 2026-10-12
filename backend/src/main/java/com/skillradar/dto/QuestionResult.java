package com.skillradar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 交卷后单道题的判分结果：对错、你的作答、正确答案，简答题还有 AI 的要点分析和点评。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResult {
    private Long questionId;
    private boolean correct;
    private String yourAnswer;
    private String correctAnswer;     // 选择题：正确选项文字；填空题：可接受答案；简答题：参考要点
    private String feedback;          // 仅简答题：AI 写给学生的一句点评
    private String keyPointsCovered;  // 仅简答题：AI 对要点覆盖/遗漏的分析
    private String reviewNote;        // 错题本/复习状态变化，如"已加入错题本，明天提醒你复习"；无变化为 null
}
