package com.skillradar.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class QuizResult {
    private String mode;             // quiz / review
    private int correctCount;
    private int totalCount;
    private boolean passed;          // review 模式恒为 false：复习不认证技能
    private List<QuestionResult> details;

    // 激励反馈：这次提交前后的目标岗位进度和可投岗位数（没选目标岗位时为 null），前端用来播放增长动画
    private Integer progressBefore;
    private Integer progressAfter;
    private Integer qualifiedBefore;
    private Integer qualifiedAfter;
    private Integer postingsWithSkills;
    private int streakDays;
    private List<Badge> newBadges;   // 这次提交新点亮的徽章
    private String encouragement;
}
