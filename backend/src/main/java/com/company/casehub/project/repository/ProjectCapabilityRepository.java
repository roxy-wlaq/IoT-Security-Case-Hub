package com.company.casehub.project.repository;

import com.company.casehub.project.entity.ProjectCapabilityEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectCapabilityRepository extends JpaRepository<ProjectCapabilityEntity, UUID> {

    List<ProjectCapabilityEntity> findByProjectId(UUID projectId);

    Optional<ProjectCapabilityEntity> findByProjectIdAndCapabilityId(UUID projectId, UUID capabilityId);
}
