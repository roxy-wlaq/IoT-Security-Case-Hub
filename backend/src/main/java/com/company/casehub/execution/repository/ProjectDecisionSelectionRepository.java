package com.company.casehub.execution.repository;

import com.company.casehub.execution.entity.ProjectDecisionSelectionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectDecisionSelectionRepository extends JpaRepository<ProjectDecisionSelectionEntity, UUID> {
    List<ProjectDecisionSelectionEntity> findByProjectTestCaseId(UUID projectTestCaseId);
    void deleteByProjectTestCaseId(UUID projectTestCaseId);
}
