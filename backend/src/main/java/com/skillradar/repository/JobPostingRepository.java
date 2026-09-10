package com.skillradar.repository;

import com.skillradar.entity.JobPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    Page<JobPosting> findByCategoryIdAndStatus(Long categoryId, JobPosting.Status status, Pageable pageable);

    /**
     * 同一批"在招"岗位里，地点匹配用户意向地区的排在前面——不是过滤掉其他地区，只是排序优先，
     * 意向地区没填（loc 为 null）时就是普通的按 id 倒序，跟原来一样。
     */
    @Query("SELECT p FROM JobPosting p WHERE p.categoryId = :categoryId AND p.status = 'open' " +
           "ORDER BY CASE WHEN :loc IS NOT NULL AND p.location LIKE CONCAT('%', :loc, '%') THEN 0 ELSE 1 END, p.id DESC")
    Page<JobPosting> findOpenPreferLocation(@Param("categoryId") Long categoryId, @Param("loc") String loc, Pageable pageable);
}
