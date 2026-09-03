package com.company.casehub.project.repository;

import com.company.casehub.project.entity.ProjectCoordinatorEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectCoordinatorRepository extends JpaRepository<ProjectCoordinatorEntity, UUID> {

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    Optional<ProjectCoordinatorEntity> findByProjectIdAndUserId(UUID projectId, UUID userId);

    boolean existsByProjectIdAndPrimaryTrue(UUID projectId);
}
