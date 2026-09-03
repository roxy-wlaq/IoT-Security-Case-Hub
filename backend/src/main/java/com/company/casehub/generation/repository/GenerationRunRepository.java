package com.company.casehub.generation.repository;

import com.company.casehub.generation.entity.GenerationRunEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationRunRepository extends JpaRepository<GenerationRunEntity, UUID> {
    List<GenerationRunEntity> findByProjectIdOrderByExecutedAtDesc(UUID projectId);
}
