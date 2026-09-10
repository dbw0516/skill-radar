package com.skillradar.controller;

import com.skillradar.entity.Favorite;
import com.skillradar.entity.FavoriteId;
import com.skillradar.entity.JobPosting;
import com.skillradar.repository.FavoriteRepository;
import com.skillradar.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** ①岗位详情页的收藏按钮：收藏/取消收藏，以及"我的收藏"列表。 */
@RestController
@RequestMapping("/api/users/{userId}/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteRepository favoriteRepository;
    private final JobPostingRepository jobPostingRepository;

    @GetMapping
    public List<JobPosting> list(@PathVariable Long userId) {
        List<Long> postingIds = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(Favorite::getPostingId).toList();
        return jobPostingRepository.findAllById(postingIds);
    }

    @GetMapping("/{postingId}")
    public Map<String, Boolean> check(@PathVariable Long userId, @PathVariable Long postingId) {
        return Map.of("favorited", favoriteRepository.existsByUserIdAndPostingId(userId, postingId));
    }

    @PutMapping("/{postingId}")
    public void add(@PathVariable Long userId, @PathVariable Long postingId) {
        if (!favoriteRepository.existsByUserIdAndPostingId(userId, postingId)) {
            favoriteRepository.save(new Favorite(userId, postingId));
        }
    }

    @DeleteMapping("/{postingId}")
    public void remove(@PathVariable Long userId, @PathVariable Long postingId) {
        // deleteById 在记录不存在时会抛异常，先查一下再删，保证重复调用/取消已经取消过的收藏不会报错
        if (favoriteRepository.existsByUserIdAndPostingId(userId, postingId)) {
            favoriteRepository.deleteById(new FavoriteId(userId, postingId));
        }
    }
}
