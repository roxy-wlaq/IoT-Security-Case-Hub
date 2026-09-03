package com.company.casehub.execution.repository;

import com.company.casehub.execution.entity.BranchOutcomeEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchOutcomeRepository extends JpaRepository<BranchOutcomeEntity, UUID> {
    List<BranchOutcomeEntity> findByProjectTestCaseId(UUID projectTestCaseId);
    void deleteByProjectTestCaseId(UUID projectTestCaseId);
}
