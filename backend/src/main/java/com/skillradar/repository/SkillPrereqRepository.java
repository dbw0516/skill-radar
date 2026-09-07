package com.skillradar.repository;

import com.skillradar.entity.SkillPrereq;
import com.skillradar.entity.SkillPrereqId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillPrereqRepository extends JpaRepository<SkillPrereq, SkillPrereqId> {
    List<SkillPrereq> findBySkillIdIn(List<Long> skillIds);
}
