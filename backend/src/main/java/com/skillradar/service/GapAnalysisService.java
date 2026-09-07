package com.skillradar.service;

import com.skillradar.dto.GapItem;
import com.skillradar.entity.JobSkill;
import com.skillradar.entity.Skill;
import com.skillradar.entity.UserSkill;
import com.skillradar.repository.JobSkillRepository;
import com.skillradar.repository.SkillRepository;
import com.skillradar.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ②技能差距分析引擎：目标岗位技能 − 已掌握技能，按权重（同类 JD 中的提及比例）排序。
 * 对应设计文档「学习路径怎么排」「用户画像怎么建」两节。
 */
@Service
@RequiredArgsConstructor
public class GapAnalysisService {

    private final JobSkillRepository jobSkillRepository;
    private final UserSkillRepository userSkillRepository;
    private final SkillRepository skillRepository;

    /** 目标岗位的全部技能，标注每一项用户是否已掌握，按权重从高到低排列。 */
    public List<GapItem> analyze(Long categoryId, Long userId) {
        List<JobSkill> required = jobSkillRepository.findByCategoryIdOrderByWeightDesc(categoryId);
        if (required.isEmpty()) return List.of();

        Map<Long, UserSkill.Status> mastered = userSkillRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserSkill::getSkillId, UserSkill::getStatus, (a, b) -> a));

        Map<Long, Skill> skillsById = skillRepository
                .findAllById(required.stream().map(JobSkill::getSkillId).toList())
                .stream().collect(Collectors.toMap(Skill::getId, s -> s));

        return required.stream().map(js -> {
            Skill skill = skillsById.get(js.getSkillId());
            UserSkill.Status status = mastered.get(js.getSkillId());
            return new GapItem(
                    js.getSkillId(),
                    skill != null ? skill.getName() : "(未知技能)",
                    skill != null ? skill.getDomain() : null,
                    js.getWeight(),
                    status != null,
                    status != null ? status.name() : null
            );
        }).toList();
    }

    /** 只要"待学"的部分（③学习路径规划引擎的输入），仍按权重从高到低排。 */
    public List<GapItem> gapOnly(Long categoryId, Long userId) {
        return analyze(categoryId, userId).stream().filter(g -> !g.isMastered()).toList();
    }
}
