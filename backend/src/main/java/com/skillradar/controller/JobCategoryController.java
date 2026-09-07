package com.skillradar.controller;

import com.skillradar.entity.JobCategory;
import com.skillradar.repository.JobCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/job-categories")
@RequiredArgsConstructor
public class JobCategoryController {

    private final JobCategoryRepository jobCategoryRepository;

    @GetMapping
    public List<JobCategory> listCategories() {
        return jobCategoryRepository.findAll();
    }
}
