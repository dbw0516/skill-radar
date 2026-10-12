package com.skillradar.controller;

import com.skillradar.entity.ResourceView;
import com.skillradar.entity.ResourceViewId;
import com.skillradar.repository.ResourceViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 学习资料阅读记录：前端点开资料链接时上报一次，计入该技能的部分进度（见 ProgressService）。 */
@RestController
@RequestMapping("/api/users/{userId}/resource-views")
@RequiredArgsConstructor
public class ResourceViewController {

    private final ResourceViewRepository resourceViewRepository;

    /** 这个用户读过的全部资料 id，测评页用来给资料打"已读"标记。 */
    @GetMapping
    public List<Long> list(@PathVariable Long userId) {
        return resourceViewRepository.findByUserId(userId).stream().map(ResourceView::getResourceId).toList();
    }

    /** 幂等：同一条资料点多少次都只记一行。 */
    @PutMapping("/{resourceId}")
    public void markViewed(@PathVariable Long userId, @PathVariable Long resourceId) {
        if (!resourceViewRepository.existsById(new ResourceViewId(userId, resourceId))) {
            resourceViewRepository.save(new ResourceView(userId, resourceId));
        }
    }
}
