package com.company.casehub.generation.repository;

import com.company.casehub.generation.entity.GenerationConditionGroupEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationConditionGroupRepository extends JpaRepository<GenerationConditionGroupEntity, UUID> {
}
