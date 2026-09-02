package com.company.casehub.testcase.repository;

import com.company.casehub.testcase.entity.TestCaseToolEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseToolRepository extends JpaRepository<TestCaseToolEntity, UUID> {
    void deleteByTestCaseVersionId(UUID versionId);
}
