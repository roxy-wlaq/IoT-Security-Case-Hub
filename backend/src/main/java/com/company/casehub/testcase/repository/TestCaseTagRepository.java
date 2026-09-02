package com.company.casehub.testcase.repository;

import com.company.casehub.testcase.entity.TestCaseTagEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseTagRepository extends JpaRepository<TestCaseTagEntity, UUID> {
    void deleteByMasterTestCaseId(UUID masterId);
}
