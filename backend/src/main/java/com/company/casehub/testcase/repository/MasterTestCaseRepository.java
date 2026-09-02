package com.company.casehub.testcase.repository;

import com.company.casehub.testcase.entity.MasterTestCaseEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MasterTestCaseRepository extends JpaRepository<MasterTestCaseEntity, UUID>, JpaSpecificationExecutor<MasterTestCaseEntity> {
    boolean existsByCaseCodeIgnoreCase(String caseCode);
    Optional<MasterTestCaseEntity> findByCaseCodeIgnoreCase(String caseCode);
}
