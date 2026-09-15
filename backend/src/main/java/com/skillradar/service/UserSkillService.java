package com.skillradar.service;

import com.skillradar.dto.SkillStatus;
import com.skillradar.entity.Skill;
import com.skillradar.entity.UserSkill;
import com.skillradar.entity.UserSkillId;
import com.skillradar.repository.SkillRepository;
import com.skillradar.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "个人中心"里的技能自评清单——设计文档「用户画像怎么建」一节说的
 * "初始化来自专业选择 + 自评清单"，这里就是那个自评清单的写入口。
 */
@Service
@RequiredArgsConstructor
public class UserSkillService {

    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;

    /** 全部技能，标注这个用户对每一个是什么状态，给前端渲染成勾选清单。 */
    public List<SkillStatus> listForUser(Long userId) {
        Map<Long, UserSkill.Status> mine = userSkillRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserSkill::getSkillId, UserSkill::getStatus));
        return skillRepository.findAll().stream()
                .map(s -> new SkillStatus(s.getId(), s.getName(), s.getDomain(),
                        mine.containsKey(s.getId()) ? mine.get(s.getId()).name() : null))
                .toList();
    }

    /**
     * 把用户的自评技能集合设成 skillIds——多选/取消选都在这一次提交里完成。
     * 只动 self_reported 的行：已经 quiz_verified 的技能，就算用户在这次提交里没勾它，
     * 也不会被撤销——测评认证过的不应该被一次自评表单覆盖掉。
     */
    @Transactional
    public void setSelfReported(Long userId, List<Long> skillIds) {
        Set<Long> target = new HashSet<>(skillIds == null ? List.of() : skillIds);
        List<UserSkill> existing = userSkillRepository.findByUserId(userId);

        Set<Long> verifiedIds = existing.stream()
                .filter(us -> us.getStatus() == UserSkill.Status.quiz_verified)
                .map(UserSkill::getSkillId)
                .collect(Collectors.toSet());
        Set<Long> selfReportedIds = existing.stream()
                .filter(us -> us.getStatus() == UserSkill.Status.self_reported)
                .map(UserSkill::getSkillId)
                .collect(Collectors.toSet());

        // 取消勾选：之前是 self_reported、这次不在 target 里的，删掉
        for (Long skillId : selfReportedIds) {
            if (!target.contains(skillId)) {
                userSkillRepository.deleteById(new UserSkillId(userId, skillId));
            }
        }
        // 新勾选：在 target 里、之前完全没有记录的（跳过已经 quiz_verified 的，不用降级也不用重复插入）
        for (Long skillId : target) {
            if (!verifiedIds.contains(skillId) && !selfReportedIds.contains(skillId)) {
                userSkillRepository.save(new UserSkill(userId, skillId, UserSkill.Status.self_reported));
            }
        }
    }

    /**
     * 取消一个技能的已掌握状态——不管是自评还是测评认证过的，直接删记录，打回"未掌握"。
     * 和 setSelfReported 不同：那个只处理批量自评表单，特意保护 quiz_verified 不被覆盖；
     * 这里是用户自己主动点的"取消"，quiz_verified 也允许撤销。
     */
    @Transactional
    public void remove(Long userId, Long skillId) {
        UserSkillId id = new UserSkillId(userId, skillId);
        if (userSkillRepository.existsById(id)) {
            userSkillRepository.deleteById(id);
        }
    }
}
