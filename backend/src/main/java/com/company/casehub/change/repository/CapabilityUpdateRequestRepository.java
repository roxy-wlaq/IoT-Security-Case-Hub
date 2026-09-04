package com.company.casehub.change.repository;

import com.company.casehub.change.entity.CapabilityUpdateRequestEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapabilityUpdateRequestRepository extends JpaRepository<CapabilityUpdateRequestEntity, UUID> {
    List<CapabilityUpdateRequestEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
