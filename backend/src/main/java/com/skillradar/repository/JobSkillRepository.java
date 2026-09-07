package com.skillradar.repository;

import com.skillradar.entity.JobSkill;
import com.skillradar.entity.JobSkillId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobSkillRepository extends JpaRepository<JobSkill, JobSkillId> {
    List<JobSkill> findByCategoryIdOrderByWeightDesc(Long categoryId);
}
