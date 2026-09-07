package com.skillradar.repository;

import com.skillradar.entity.JobPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    Page<JobPosting> findByCategoryIdAndStatus(Long categoryId, JobPosting.Status status, Pageable pageable);
}
