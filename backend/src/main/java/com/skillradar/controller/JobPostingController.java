package com.skillradar.controller;

import com.skillradar.entity.JobPosting;
import com.skillradar.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** 单条招聘信息详情——①岗位推荐列表点进去看的那个页面。 */
@RestController
@RequestMapping("/api/postings")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingRepository jobPostingRepository;

    @GetMapping("/{id}")
    public JobPosting detail(@PathVariable Long id) {
        return jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "这条招聘信息不存在"));
    }
}
