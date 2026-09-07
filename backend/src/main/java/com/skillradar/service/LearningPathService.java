package com.skillradar.service;

import com.skillradar.dto.GapItem;
import com.skillradar.dto.LearningPathStage;
import com.skillradar.entity.SkillPrereq;
import com.skillradar.repository.SkillPrereqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ③学习路径规划引擎：对②输出的待学技能子图做拓扑排序（Kahn 算法），按层输出。
 * 对应设计文档「学习路径怎么排」一节的示例——已经掌握的技能不算依赖，
 * 只统计"待学技能之间"的依赖关系，同一层内按权重（weight）从高到低排序。
 */
@Service
@RequiredArgsConstructor
public class LearningPathService {

    private final SkillPrereqRepository skillPrereqRepository;
    private final GapAnalysisService gapAnalysisService;

    public List<LearningPathStage> plan(Long categoryId, Long userId) {
        List<GapItem> gap = gapAnalysisService.gapOnly(categoryId, userId);
        if (gap.isEmpty()) return List.of();

        Map<Long, GapItem> bySkillId = new HashMap<>();
        for (GapItem g : gap) bySkillId.put(g.getSkillId(), g);
        Set<Long> gapIds = bySkillId.keySet();

        List<SkillPrereq> edges = skillPrereqRepository.findBySkillIdIn(new ArrayList<>(gapIds));
        // 只保留"前置技能也在待学清单里"的边——已经掌握的前置不算阻塞
        Map<Long, List<Long>> dependsOn = new HashMap<>();   // skill -> 还没学的前置技能
        Map<Long, List<Long>> unlocks = new HashMap<>();     // prereq -> 依赖它的技能（学完它之后能解锁谁）
        Map<Long, Integer> inDegree = new HashMap<>();
        for (Long id : gapIds) inDegree.put(id, 0);

        for (SkillPrereq e : edges) {
            if (!gapIds.contains(e.getPrereqSkillId())) continue; // 前置已掌握，不算依赖
            dependsOn.computeIfAbsent(e.getSkillId(), k -> new ArrayList<>()).add(e.getPrereqSkillId());
            unlocks.computeIfAbsent(e.getPrereqSkillId(), k -> new ArrayList<>()).add(e.getSkillId());
            inDegree.merge(e.getSkillId(), 1, Integer::sum);
        }

        List<LearningPathStage> stages = new ArrayList<>();
        Set<Long> done = new HashSet<>();
        int stageNo = 1;
        while (done.size() < gapIds.size()) {
            List<Long> ready = gapIds.stream()
                    .filter(id -> !done.contains(id) && inDegree.getOrDefault(id, 0) == 0)
                    .sorted((a, b) -> bySkillId.get(b).getWeight().compareTo(bySkillId.get(a).getWeight()))
                    .toList();

            if (ready.isEmpty()) {
                // 理论上不该发生（说明技能依赖表里有环），保底：把剩下的一次性扔进最后一层，避免死循环
                ready = gapIds.stream().filter(id -> !done.contains(id)).toList();
            }

            List<GapItem> stageSkills = ready.stream().map(bySkillId::get).toList();
            stages.add(new LearningPathStage(stageNo++, stageSkills));
            done.addAll(ready);
            for (Long finished : ready) {
                for (Long next : unlocks.getOrDefault(finished, List.of())) {
                    inDegree.merge(next, -1, Integer::sum);
                }
            }
        }
        return stages;
    }
}
