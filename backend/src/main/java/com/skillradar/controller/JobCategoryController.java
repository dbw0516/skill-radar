package com.skillradar.controller;

import com.skillradar.entity.JobCategory;
import com.skillradar.entity.JobPosting;
import com.skillradar.repository.JobCategoryRepository;
import com.skillradar.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-categories")
@RequiredArgsConstructor
public class JobCategoryController {

    private final JobCategoryRepository jobCategoryRepository;
    private final JobPostingRepository jobPostingRepository;

    @GetMapping
    public List<JobCategory> listCategories() {
        return jobCategoryRepository.findAll();
    }

    /**
     * ①岗位推荐引擎的展示层：某个类别下的具体招聘信息，默认只看"在招"的，分页避免一次性把上百条都拖回来。
     * preferLocation 传了的话（用户的意向就业地区），地点匹配的岗位排在前面，不传就是原来的顺序。
     */
    @GetMapping("/{id}/postings")
    public Page<JobPosting> postings(@PathVariable Long id,
                                      @RequestParam(required = false) String preferLocation,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return jobPostingRepository.findOpenPreferLocation(id, preferLocation, PageRequest.of(page, size));
    }
}
