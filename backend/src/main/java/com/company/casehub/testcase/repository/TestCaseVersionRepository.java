package com.company.casehub.testcase.repository;

import com.company.casehub.testcase.entity.TestCaseVersionEntity;
import com.company.casehub.testcase.entity.TestCaseVersionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TestCaseVersionRepository extends JpaRepository<TestCaseVersionEntity, UUID>, JpaSpecificationExecutor<TestCaseVersionEntity> {
    Optional<TestCaseVersionEntity> findByMasterTestCaseIdAndStatus(UUID masterId, TestCaseVersionStatus status);
    List<TestCaseVersionEntity> findByMasterTestCaseIdOrderByVersionMajorDescVersionMinorDesc(UUID masterId);
    Optional<TestCaseVersionEntity> findByIdAndMasterTestCaseId(UUID id, UUID masterId);
    Optional<TestCaseVersionEntity> findFirstByMasterTestCaseIdAndStatusOrderByVersionMajorDescVersionMinorDesc(UUID masterId, TestCaseVersionStatus status);
}
