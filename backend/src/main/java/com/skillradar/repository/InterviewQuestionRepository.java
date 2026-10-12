package com.skillradar.repository;

import com.skillradar.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    List<InterviewQuestion> findBySkillIdIn(Collection<Long> skillIds);
}
