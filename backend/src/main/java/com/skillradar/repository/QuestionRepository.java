package com.skillradar.repository;

import com.skillradar.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findBySkillId(Long skillId);
}
