package com.company.casehub.execution.repository;

import com.company.casehub.execution.entity.ProjectTestCasePreferenceEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTestCasePreferenceRepository extends JpaRepository<ProjectTestCasePreferenceEntity, UUID> {
    Optional<ProjectTestCasePreferenceEntity> findByProjectIdAndMasterTestCaseId(UUID projectId, UUID masterTestCaseId);
}
