package com.skillradar.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学习进度 + 激励信息，给前端的进度条/打卡/徽章面板用。
 * 目标岗位相关的字段（categoryId 到 postingsWithSkills）在用户还没选目标岗位时为 null。
 */
@Data
@NoArgsConstructor
public class LearningProgress {
    private Long categoryId;
    private String categoryName;
    private Integer totalSkills;
    private Integer masteredSkills;      // 自评 + 测评认证
    private Integer verifiedSkills;      // 其中测评认证的
    private Integer learningSkills;      // 没掌握但已经有部分进度的
    private Integer percent;             // 各技能 score 的平均值，0~100（含部分进度）
    // 进度条三段各占的宽度（0~100，相加 = percent），前端直接拿来画
    private Double verifiedShare;
    private Double selfShare;
    private Double learningShare;
    private Integer weightedPercent;     // 已掌握技能按 JD 提及权重加权的覆盖率，0~100
    private String nextMilestone;        // "再掌握 3 个技能，就掌握了一半" 这类
    private Long nextSkillId;            // 下一步推荐学的技能：未掌握里权重最高的
    private String nextSkillName;
    private Integer nextSkillWeight;     // 0~100
    private List<SkillProgress> skills;  // 按权重从高到低
    private Integer qualifiedPostings;   // 匹配度达标（≥70%）的在招岗位数
    private Integer postingsWithSkills;  // 能算匹配度的在招岗位总数

    private int streakDays;              // 当前连续学习天数（今天还没学、但昨天学了，也算没断）
    private int longestStreak;
    private boolean practicedToday;
    private int totalAttempts;           // 正式测评次数（不含错题复习）
    private int passedAttempts;
    private int reviewAttempts;          // 错题复习次数
    private int totalAnswered;           // 累计答题数（含错题复习）
    private int dueReviews;              // 今天到期该复习的错题数
    private int interviewsFinished;

    private String message;              // 一句鼓励语
    private List<Badge> badges;
}
