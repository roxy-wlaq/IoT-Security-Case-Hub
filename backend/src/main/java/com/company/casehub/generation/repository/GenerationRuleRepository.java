package com.company.casehub.generation.repository;

import com.company.casehub.generation.entity.GenerationRuleEntity;
import com.company.casehub.generation.entity.GenerationRuleStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationRuleRepository extends JpaRepository<GenerationRuleEntity, UUID> {

    boolean existsByRuleCodeIgnoreCase(String ruleCode);

    @EntityGraph(attributePaths = {"groups", "groups.conditions", "outputs"})
    List<GenerationRuleEntity> findAllByOrderByRuleCodeAsc();

    @EntityGraph(attributePaths = {"groups", "groups.conditions", "outputs"})
    java.util.Optional<GenerationRuleEntity> findWithGraphById(UUID id);

    List<GenerationRuleEntity> findByStatusOrderByRuleCodeAsc(GenerationRuleStatus status);
}
