package com.skillradar.controller;

import com.skillradar.dto.InterviewRequest;
import com.skillradar.dto.InterviewSessionView;
import com.skillradar.dto.InterviewSummary;
import com.skillradar.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** AI 模拟面试，逻辑见 InterviewService。 */
@RestController
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/api/interview/sessions")
    public InterviewSessionView start(@RequestBody InterviewRequest req) {
        return interviewService.start(req.getUserId(), req.getCategoryId());
    }

    @GetMapping("/api/interview/sessions/{id}")
    public InterviewSessionView get(@PathVariable Long id, @RequestParam Long userId) {
        return interviewService.get(id, userId);
    }

    @PostMapping("/api/interview/sessions/{id}/answer")
    public InterviewSessionView answer(@PathVariable Long id, @RequestBody InterviewRequest req) {
        return interviewService.answer(id, req.getUserId(), req.getAnswer(),
                req.getSeconds(), req.getTimeLimit(), req.getTimedOut());
    }

    @PostMapping("/api/interview/sessions/{id}/finish")
    public InterviewSessionView finish(@PathVariable Long id, @RequestBody InterviewRequest req) {
        return interviewService.finish(id, req.getUserId());
    }

    @GetMapping("/api/users/{userId}/interview-sessions")
    public List<InterviewSummary> list(@PathVariable Long userId) {
        return interviewService.list(userId);
    }
}
