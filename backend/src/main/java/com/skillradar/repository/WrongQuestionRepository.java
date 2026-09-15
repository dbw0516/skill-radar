package com.skillradar.repository;

import com.skillradar.entity.WrongQuestion;
import com.skillradar.entity.WrongQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WrongQuestionRepository extends JpaRepository<WrongQuestion, WrongQuestionId> {
    List<WrongQuestion> findByUserIdOrderByLastWrongAtDesc(Long userId);
    Optional<WrongQuestion> findByUserIdAndQuestionId(Long userId, Long questionId);
}
