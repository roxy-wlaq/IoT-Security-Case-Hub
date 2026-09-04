package com.company.casehub.change.repository;

import com.company.casehub.change.entity.TestCaseChangeRequestEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseChangeRequestRepository extends JpaRepository<TestCaseChangeRequestEntity, UUID> {
    List<TestCaseChangeRequestEntity> findByMasterTestCaseIdOrderByCreatedAtDesc(UUID masterId);
}
