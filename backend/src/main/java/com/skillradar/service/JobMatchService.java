package com.skillradar.service;

import com.skillradar.dto.PostingMatch;
import com.skillradar.dto.SkillSummary;
import com.skillradar.entity.JobPosting;
import com.skillradar.entity.JobSkill;
import com.skillradar.entity.Skill;
import com.skillradar.entity.UserSkill;
import com.skillradar.repository.JobPostingRepository;
import com.skillradar.repository.JobSkillRepository;
import com.skillradar.repository.SkillRepository;
import com.skillradar.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 岗位匹配度：用户已掌握的技能（自评 + 测评认证）覆盖了一条 JD 要求技能的百分之多少。
 * "可投岗位"= 匹配度达到 QUALIFY_PERCENT 的在招岗位，只统计抽取出了技能的 JD
 * （没抽出技能的 JD 没法算匹配度，硬算进分母会让数字失真）。
 */
@Service
@RequiredArgsConstructor
public class JobMatchService {

    public static final int QUALIFY_PERCENT = 70;
    /** JD 没抽出技能时按类别估算，只取提及比例不低于这个值的技能，免得长尾技能把要求撑得过高。 */
    private static final BigDecimal CATEGORY_FALLBACK_MIN_WEIGHT = new BigDecimal("0.3");

    private final JdbcTemplate jdbc;
    private final JobPostingRepository jobPostingRepository;
    private final JobSkillRepository jobSkillRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;

    public record QualifiedSummary(int qualified, int total) {}

    public PostingMatch matchPosting(Long userId, Long postingId) {
        JobPosting posting = jobPostingRepository.findById(postingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "这条招聘信息不存在"));
        List<Long> required = jdbc.queryForList(
                "SELECT skill_id FROM posting_skills WHERE posting_id = ?", Long.class, postingId);
        String basis = "posting";
        if (required.isEmpty() && posting.getCategoryId() != null) {
            basis = "category";
            List<JobSkill> jobSkills = jobSkillRepository.findByCategoryIdOrderByWeightDesc(posting.getCategoryId());
            required = jobSkills.stream()
                    .filter(js -> js.getWeight().compareTo(CATEGORY_FALLBACK_MIN_WEIGHT) >= 0)
                    .map(JobSkill::getSkillId).toList();
            if (required.isEmpty()) {
                required = jobSkills.stream().map(JobSkill::getSkillId).toList();
            }
        }
        Set<Long> mastered = masteredSkillIds(userId);
        Map<Long, Skill> skills = skillRepository.findAllById(required).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));
        List<SkillSummary> matched = new ArrayList<>();
        List<SkillSummary> missing = new ArrayList<>();
        for (Long id : required) {
            Skill s = skills.get(id);
            if (s == null) continue;
            (mastered.contains(id) ? matched : missing).add(new SkillSummary(s.getId(), s.getName(), s.getDomain()));
        }
        int total = matched.size() + missing.size();
        return new PostingMatch(total == 0 ? 0 : matched.size() * 100 / total, basis, matched, missing);
    }

    /** 一批 JD 各自的匹配度（岗位列表上的小角标用），没抽出技能的 JD 不出现在结果里。 */
    public Map<Long, Integer> matchPostings(Long userId, Collection<Long> postingIds) {
        if (postingIds.isEmpty()) return Map.of();
        Map<Long, List<Long>> skillsByPosting = new HashMap<>();
        String placeholders = postingIds.stream().map(id -> "?").collect(Collectors.joining(","));
        jdbc.query("SELECT posting_id, skill_id FROM posting_skills WHERE posting_id IN (" + placeholders + ")",
                rs -> {
                    skillsByPosting.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(rs.getLong(2));
                },
                postingIds.toArray());
        return percentByPosting(skillsByPosting, masteredSkillIds(userId));
    }

    /** 这个类别里在招、且抽出了技能的岗位中，匹配度达标的有几条。 */
    public QualifiedSummary qualified(Long userId, Long categoryId) {
        Map<Long, List<Long>> skillsByPosting = new HashMap<>();
        jdbc.query("SELECT ps.posting_id, ps.skill_id FROM posting_skills ps "
                        + "JOIN job_postings p ON p.id = ps.posting_id "
                        + "WHERE p.category_id = ? AND p.status = 'open'",
                rs -> {
                    skillsByPosting.computeIfAbsent(rs.getLong(1), k -> new ArrayList<>()).add(rs.getLong(2));
                },
                categoryId);
        Map<Long, Integer> percents = percentByPosting(skillsByPosting, masteredSkillIds(userId));
        int qualified = (int) percents.values().stream().filter(p -> p >= QUALIFY_PERCENT).count();
        return new QualifiedSummary(qualified, percents.size());
    }

    private Map<Long, Integer> percentByPosting(Map<Long, List<Long>> skillsByPosting, Set<Long> mastered) {
        Map<Long, Integer> result = new HashMap<>();
        skillsByPosting.forEach((postingId, skills) -> {
            long hit = skills.stream().filter(mastered::contains).count();
            result.put(postingId, (int) (hit * 100 / skills.size()));
        });
        return result;
    }

    private Set<Long> masteredSkillIds(Long userId) {
        if (userId == null) return Set.of();
        return userSkillRepository.findByUserId(userId).stream()
                .map(UserSkill::getSkillId).collect(Collectors.toSet());
    }
}
