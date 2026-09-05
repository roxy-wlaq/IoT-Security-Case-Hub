package com.company.casehub.audit.repository;

import com.company.casehub.audit.entity.AuditRecordEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditRecordRepository extends JpaRepository<AuditRecordEntity, UUID>,
        JpaSpecificationExecutor<AuditRecordEntity> {
}
