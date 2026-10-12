package com.skillradar.repository;

import com.skillradar.entity.ResourceView;
import com.skillradar.entity.ResourceViewId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceViewRepository extends JpaRepository<ResourceView, ResourceViewId> {
    List<ResourceView> findByUserId(Long userId);
}
