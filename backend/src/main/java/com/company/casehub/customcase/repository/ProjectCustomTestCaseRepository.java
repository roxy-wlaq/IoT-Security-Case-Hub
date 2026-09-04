package com.company.casehub.customcase.repository;

import com.company.casehub.customcase.entity.ProjectCustomTestCaseEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectCustomTestCaseRepository extends JpaRepository<ProjectCustomTestCaseEntity, UUID> {
    boolean existsByProjectIdAndCaseCodeIgnoreCase(UUID projectId, String caseCode);
    List<ProjectCustomTestCaseEntity> findByProjectIdOrderByCreatedAtAsc(UUID projectId);
    Optional<ProjectCustomTestCaseEntity> findByIdAndProjectId(UUID id, UUID projectId);
}
