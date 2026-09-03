package com.company.casehub.execution.repository;

import com.company.casehub.execution.entity.ProjectTestCaseTriggerEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTestCaseTriggerRepository extends JpaRepository<ProjectTestCaseTriggerEntity, UUID> {
    List<ProjectTestCaseTriggerEntity> findBySourceProjectTestCaseId(UUID sourceProjectTestCaseId);
    List<ProjectTestCaseTriggerEntity> findByTargetProjectTestCaseId(UUID targetProjectTestCaseId);
    boolean existsBySourceProjectTestCaseIdAndSourceDecisionPointIdAndTargetProjectTestCaseId(
            UUID sourceProjectTestCaseId, UUID sourceDecisionPointId, UUID targetProjectTestCaseId);
    void deleteBySourceProjectTestCaseIdAndSourceDecisionPointId(UUID sourceProjectTestCaseId, UUID sourceDecisionPointId);
}
