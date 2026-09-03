package com.company.casehub.generation.repository;

import com.company.casehub.generation.entity.GenerationRuleOutputEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationRuleOutputRepository extends JpaRepository<GenerationRuleOutputEntity, UUID> {
}
