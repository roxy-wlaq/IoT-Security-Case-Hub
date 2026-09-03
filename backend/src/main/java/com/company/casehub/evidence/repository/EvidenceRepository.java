package com.company.casehub.evidence.repository;

import com.company.casehub.evidence.entity.EvidenceEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceRepository extends JpaRepository<EvidenceEntity, UUID> {
    List<EvidenceEntity> findByProjectTestCaseIdOrderByCreatedAtAsc(UUID projectTestCaseId);
    long countByProjectTestCaseId(UUID projectTestCaseId);
}
