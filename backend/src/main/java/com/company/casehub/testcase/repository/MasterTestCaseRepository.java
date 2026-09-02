package com.company.casehub.testcase.repository;

import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MasterTestCaseRepository extends JpaRepository<MasterTestCaseEntity, UUID>, JpaSpecificationExecutor<MasterTestCaseEntity> {
    boolean existsByCaseCodeIgnoreCase(String caseCode);
    Optional<MasterTestCaseEntity> findByCaseCodeIgnoreCase(String caseCode);

    /**
     * Loads a Master with a row-level write lock (SELECT ... FOR UPDATE) so that
     * concurrent Publish / Create Revision / Deprecate operations serialize on the
     * same Master row. Combined with the partial unique index
     * {@code uq_test_case_current_version} and {@code uq_test_case_versions_number}
     * this prevents two concurrent current-published versions and version-number
     * collisions without relying on Java-level synchronization.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MasterTestCaseEntity m where m.id = :id")
    Optional<MasterTestCaseEntity> findByIdWithLock(@Param("id") UUID id);
}
