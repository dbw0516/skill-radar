package com.skillradar.controller;

import com.skillradar.dto.PostingMatch;
import com.skillradar.service.JobMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 岗位匹配度：详情页的匹配分析 + 列表页每条岗位的匹配度角标。 */
@RestController
@RequiredArgsConstructor
public class JobMatchController {

    private final JobMatchService jobMatchService;

    @GetMapping("/api/postings/{postingId}/match")
    public PostingMatch match(@PathVariable Long postingId, @RequestParam Long userId) {
        return jobMatchService.matchPosting(userId, postingId);
    }

    @GetMapping("/api/users/{userId}/posting-matches")
    public Map<Long, Integer> matches(@PathVariable Long userId, @RequestParam List<Long> ids) {
        return jobMatchService.matchPostings(userId, ids);
    }
}
