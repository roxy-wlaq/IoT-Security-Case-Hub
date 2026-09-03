package com.company.casehub.execution.repository;

import com.company.casehub.execution.entity.ProjectTestCaseAssigneeEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTestCaseAssigneeRepository extends JpaRepository<ProjectTestCaseAssigneeEntity, UUID> {
    List<ProjectTestCaseAssigneeEntity> findByProjectTestCaseId(UUID projectTestCaseId);
    List<ProjectTestCaseAssigneeEntity> findByUserId(UUID userId);
    Optional<ProjectTestCaseAssigneeEntity> findByProjectTestCaseIdAndUserId(UUID projectTestCaseId, UUID userId);
    boolean existsByProjectTestCaseIdAndUserId(UUID projectTestCaseId, UUID userId);
    boolean existsByProjectTestCaseProjectIdAndUserId(UUID projectId, UUID userId);
}
