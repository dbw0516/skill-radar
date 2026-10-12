package com.skillradar.service;

import com.skillradar.dto.Badge;
import com.skillradar.dto.GapItem;
import com.skillradar.dto.LearningProgress;
import com.skillradar.dto.SkillProgress;
import com.skillradar.entity.InterviewSession;
import com.skillradar.entity.LearningResource;
import com.skillradar.entity.QuizAttempt;
import com.skillradar.entity.ResourceView;
import com.skillradar.entity.User;
import com.skillradar.entity.UserSkill;
import com.skillradar.repository.InterviewSessionRepository;
import com.skillradar.repository.JobCategoryRepository;
import com.skillradar.repository.LearningResourceRepository;
import com.skillradar.repository.QuizAttemptRepository;
import com.skillradar.repository.ResourceViewRepository;
import com.skillradar.repository.UserRepository;
import com.skillradar.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习进度 + 激励机制：目标岗位技能进度条、可投岗位数、连续学习打卡、错题复习提醒、徽章、鼓励语。
 * 全部从现有表现算，不存快照——徽章是"满足条件就点亮"，所以撤销某个技能后对应的进度类徽章也会熄灭，
 * 这是有意的：进度徽章反映的是"现在的水平"。连续打卡类徽章按历史最长连续天数算，断签不会收回。
 *
 * 技能内部的部分进度：没掌握的技能也按"历史最好正确率 × 60% + 资料阅读比例 × 30%"给分，封顶 90%——
 * 不然 5 个技能的岗位每通过一项跳 20%、没通过就一点不动，反复挑战的人完全看不到自己在进步。
 * 错题复习的正确率不计入：复习题是自己错过、看过答案的那几道，算进来会虚高。
 */
@Service
@RequiredArgsConstructor
public class ProgressService {

    private static final double QUIZ_PART = 0.6;
    private static final double RESOURCE_PART = 0.3;
    private static final double PARTIAL_CAP = 0.9;

    private final GapAnalysisService gapAnalysisService;
    private final JobMatchService jobMatchService;
    private final WrongQuestionService wrongQuestionService;
    private final UserRepository userRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final UserSkillRepository userSkillRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final ResourceViewRepository resourceViewRepository;
    private final InterviewSessionRepository interviewSessionRepository;

    public LearningProgress progress(Long userId) {
        LearningProgress p = new LearningProgress();
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserId(userId);
        Long categoryId = userRepository.findById(userId).map(User::getTargetCategoryId).orElse(null);
        if (categoryId != null) {
            fillCategoryProgress(p, categoryId, userId, attempts);
        }
        List<InterviewSession> finishedInterviews = interviewSessionRepository.findByUserIdOrderByIdDesc(userId).stream()
                .filter(s -> s.getStatus() == InterviewSession.Status.finished).toList();
        p.setInterviewsFinished(finishedInterviews.size());
        p.setDueReviews((int) wrongQuestionService.dueCount(userId));
        fillActivity(p, attempts, finishedInterviews);

        long verifiedTotal = userSkillRepository.findByUserId(userId).stream()
                .filter(us -> us.getStatus() == UserSkill.Status.quiz_verified).count();
        p.setBadges(badges(p, verifiedTotal));
        p.setMessage(message(p));
        return p;
    }

    private void fillCategoryProgress(LearningProgress p, Long categoryId, Long userId, List<QuizAttempt> attempts) {
        List<GapItem> items = gapAnalysisService.analyze(categoryId, userId);
        p.setCategoryId(categoryId);
        p.setCategoryName(jobCategoryRepository.findById(categoryId).map(c -> c.getName()).orElse(null));

        Set<Long> skillIds = items.stream().map(GapItem::getSkillId).collect(Collectors.toSet());
        Map<Long, Double> bestAccuracy = new HashMap<>();
        for (QuizAttempt a : attempts) {
            if (a.getKind() == QuizAttempt.Kind.quiz && skillIds.contains(a.getSkillId()) && a.getTotalCount() > 0) {
                bestAccuracy.merge(a.getSkillId(), (double) a.getCorrectCount() / a.getTotalCount(), Math::max);
            }
        }
        List<LearningResource> resources = learningResourceRepository.findBySkillIdIn(skillIds);
        Set<Long> viewed = resourceViewRepository.findByUserId(userId).stream()
                .map(ResourceView::getResourceId).collect(Collectors.toSet());
        Map<Long, int[]> resourceStats = new HashMap<>(); // skillId -> [viewed, total]
        for (LearningResource r : resources) {
            int[] st = resourceStats.computeIfAbsent(r.getSkillId(), k -> new int[2]);
            st[1]++;
            if (viewed.contains(r.getId())) st[0]++;
        }

        int total = items.size();
        int mastered = 0, verified = 0, learning = 0;
        double sumScore = 0, sumVerified = 0, sumSelf = 0, sumLearning = 0;
        List<SkillProgress> skillProgress = new ArrayList<>();
        for (GapItem g : items) {
            int[] st = resourceStats.getOrDefault(g.getSkillId(), new int[2]);
            Double best = bestAccuracy.get(g.getSkillId());
            double score;
            String status;
            if (g.isMastered()) {
                score = 1.0;
                mastered++;
                if ("quiz_verified".equals(g.getMasteredSource())) {
                    verified++;
                    sumVerified += 1;
                    status = "verified";
                } else {
                    sumSelf += 1;
                    status = "self";
                }
            } else {
                double resourceRatio = st[1] == 0 ? 0 : (double) st[0] / st[1];
                score = Math.min(PARTIAL_CAP, (best == null ? 0 : best) * QUIZ_PART + resourceRatio * RESOURCE_PART);
                status = score > 0 ? "learning" : "none";
                if (score > 0) learning++;
                sumLearning += score;
            }
            sumScore += score;
            skillProgress.add(new SkillProgress(g.getSkillId(), g.getName(),
                    g.getWeight().multiply(BigDecimal.valueOf(100)).intValue(),
                    (int) Math.round(score * 100), status,
                    best == null ? null : (int) Math.round(best * 100), st[0], st[1]));
        }

        BigDecimal weightAll = items.stream().map(GapItem::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal weightMastered = items.stream().filter(GapItem::isMastered)
                .map(GapItem::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);

        p.setTotalSkills(total);
        p.setMasteredSkills(mastered);
        p.setVerifiedSkills(verified);
        p.setLearningSkills(learning);
        p.setSkills(skillProgress);
        if (total > 0) {
            p.setVerifiedShare(sumVerified * 100 / total);
            p.setSelfShare(sumSelf * 100 / total);
            p.setLearningShare(sumLearning * 100 / total);
            // 向下取整：没全部掌握时不会因为四舍五入显示成 100%
            p.setPercent((int) Math.floor(sumScore * 100 / total + 1e-9));
        } else {
            p.setVerifiedShare(0.0);
            p.setSelfShare(0.0);
            p.setLearningShare(0.0);
            p.setPercent(0);
        }
        p.setWeightedPercent(weightAll.signum() == 0 ? 0
                : weightMastered.multiply(BigDecimal.valueOf(100)).divide(weightAll, 0, RoundingMode.DOWN).intValue());

        // analyze() 已按权重降序，第一个没掌握的就是最值得先学的
        items.stream().filter(g -> !g.isMastered()).findFirst().ifPresent(g -> {
            p.setNextSkillId(g.getSkillId());
            p.setNextSkillName(g.getName());
            p.setNextSkillWeight(g.getWeight().multiply(BigDecimal.valueOf(100)).intValue());
        });

        String[] milestoneNames = {"四分之一", "一半", "四分之三"};
        int[] milestones = {25, 50, 75, 100};
        for (int i = 0; i < milestones.length; i++) {
            int needed = (int) Math.ceil(total * milestones[i] / 100.0) - mastered;
            if (needed > 0) {
                p.setNextMilestone(milestones[i] == 100
                        ? "再掌握 " + needed + " 个技能，就能全部覆盖这个岗位的技能要求"
                        : "再掌握 " + needed + " 个技能，就掌握了这个岗位" + milestoneNames[i] + "的技能");
                break;
            }
        }

        JobMatchService.QualifiedSummary qs = jobMatchService.qualified(userId, categoryId);
        p.setQualifiedPostings(qs.qualified());
        p.setPostingsWithSkills(qs.total());
    }

    private void fillActivity(LearningProgress p, List<QuizAttempt> attempts, List<InterviewSession> finishedInterviews) {
        LocalDate today = LocalDate.now();
        Set<LocalDate> days = new TreeSet<>();
        for (QuizAttempt a : attempts) {
            days.add(a.getAttemptedAt() == null ? today : a.getAttemptedAt().toLocalDate());
        }
        for (InterviewSession s : finishedInterviews) {
            if (s.getFinishedAt() != null) days.add(s.getFinishedAt().toLocalDate());
        }

        p.setTotalAttempts((int) attempts.stream().filter(a -> a.getKind() == QuizAttempt.Kind.quiz).count());
        p.setPassedAttempts((int) attempts.stream()
                .filter(a -> a.getKind() == QuizAttempt.Kind.quiz && a.isPassed()).count());
        p.setReviewAttempts((int) attempts.stream().filter(a -> a.getKind() == QuizAttempt.Kind.review).count());
        p.setTotalAnswered(attempts.stream().mapToInt(QuizAttempt::getTotalCount).sum());
        p.setPracticedToday(days.contains(today));

        // 当前连续天数：今天学了从今天往前数；今天还没学但昨天学了，从昨天往前数（今天还有机会续上，不算断）
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        p.setStreakDays(streak);

        int longest = 0, run = 0;
        LocalDate prev = null;
        for (LocalDate d : days) {
            run = (prev != null && prev.plusDays(1).equals(d)) ? run + 1 : 1;
            longest = Math.max(longest, run);
            prev = d;
        }
        p.setLongestStreak(longest);
    }

    private List<Badge> badges(LearningProgress p, long verifiedTotal) {
        int total = p.getTotalSkills() == null ? 0 : p.getTotalSkills();
        int mastered = p.getMasteredSkills() == null ? 0 : p.getMasteredSkills();
        List<Badge> list = new ArrayList<>();
        list.add(new Badge("first_quiz", "🚀", "初次出发", "完成第一次测评", p.getTotalAttempts() >= 1));
        list.add(new Badge("first_pass", "✅", "首战告捷", "第一次通过测评", p.getPassedAttempts() >= 1));
        list.add(new Badge("streak_3", "🔥", "三天连学", "连续 3 天学习打卡", p.getLongestStreak() >= 3));
        list.add(new Badge("streak_7", "🏅", "一周坚持", "连续 7 天学习打卡", p.getLongestStreak() >= 7));
        list.add(new Badge("review_3", "🧠", "温故知新", "完成 3 次错题复习", p.getReviewAttempts() >= 3));
        list.add(new Badge("answered_100", "💯", "百题斩", "累计作答 100 道题", p.getTotalAnswered() >= 100));
        list.add(new Badge("interview_1", "🎤", "初试锋芒", "完成一场 AI 模拟面试", p.getInterviewsFinished() >= 1));
        list.add(new Badge("verified_5", "🎓", "认证达人", "5 个技能通过测评认证", verifiedTotal >= 5));
        list.add(new Badge("half_way", "🌗", "半程达标", "掌握目标岗位一半的技能", total > 0 && mastered * 2 >= total));
        list.add(new Badge("job_ready", "🏁", "岗位就绪", "目标岗位技能全部掌握", total > 0 && mastered == total));
        return list;
    }

    private String message(LearningProgress p) {
        if (p.getTotalSkills() != null && p.getTotalSkills() > 0 && p.getMasteredSkills().equals(p.getTotalSkills())) {
            return "目标岗位要求的技能你已经全部掌握了，去模拟面试练练手，然后放心投简历吧！";
        }
        if (p.getTotalAttempts() == 0 && p.getReviewAttempts() == 0 && p.getInterviewsFinished() == 0) {
            return "迈出第一步吧：做一次测评只要几分钟，就能知道自己离目标岗位还有多远。";
        }
        if (!p.isPracticedToday() && p.getDueReviews() > 0) {
            return "今天有 " + p.getDueReviews() + " 道错题到了复习时间，复习一下就能"
                    + (p.getStreakDays() > 0 ? "续上连续 " + p.getStreakDays() + " 天的打卡 🔥" : "完成今天的打卡");
        }
        if (p.isPracticedToday()) {
            return p.getStreakDays() > 1
                    ? "今天已打卡，连续学习 " + p.getStreakDays() + " 天，保持这个节奏！"
                    : "今天已打卡，明天再来就能开始累计连续天数。";
        }
        if (p.getStreakDays() > 0) {
            return "你已经连续学习 " + p.getStreakDays() + " 天，今天做一次测评就能续上 🔥";
        }
        return "好久不见！今天做一次测评重新出发，随时都不晚。";
    }
}
