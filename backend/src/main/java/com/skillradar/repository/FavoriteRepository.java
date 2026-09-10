package com.skillradar.repository;

import com.skillradar.entity.Favorite;
import com.skillradar.entity.FavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndPostingId(Long userId, Long postingId);
}
