package com.company.casehub.testcase.repository;

import com.company.casehub.testcase.entity.TestCaseStandardMappingEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseStandardMappingRepository extends JpaRepository<TestCaseStandardMappingEntity, UUID> {
    void deleteByTestCaseVersionId(UUID versionId);
}
