package com.company.casehub.change.repository;

import com.company.casehub.change.entity.TestCaseChangeRequestEntity;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseChangeRequestRepository extends JpaRepository<TestCaseChangeRequestEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from TestCaseChangeRequestEntity r where r.id = :id")
    java.util.Optional<TestCaseChangeRequestEntity> findByIdForUpdate(@Param("id") UUID id);
    List<TestCaseChangeRequestEntity> findByMasterTestCaseIdOrderByCreatedAtDesc(UUID masterId);
}
