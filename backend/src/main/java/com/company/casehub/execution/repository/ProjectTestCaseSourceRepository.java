package com.company.casehub.execution.repository;

import com.company.casehub.execution.entity.ProjectTestCaseSourceEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectTestCaseSourceRepository extends JpaRepository<ProjectTestCaseSourceEntity, UUID> {
    boolean existsByProjectTestCaseIdAndSourceType(UUID projectTestCaseId,
                                                    com.company.casehub.execution.entity.ProjectTestCaseSourceType sourceType);
}
