package com.company.casehub.generation.repository;

import com.company.casehub.generation.entity.GenerationRecommendationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface GenerationRecommendationRepository extends JpaRepository<GenerationRecommendationEntity, UUID> {
    @EntityGraph(attributePaths = {"masterTestCase", "resolvedVersion", "matchedRules", "matchedRules.rule"})
    List<GenerationRecommendationEntity> findByGenerationRunIdOrderByCreatedAtAsc(UUID runId);
}
