package com.company.casehub.change.repository;

import com.company.casehub.change.entity.CapabilityUpdateRequestEntity;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapabilityUpdateRequestRepository extends JpaRepository<CapabilityUpdateRequestEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CapabilityUpdateRequestEntity r where r.id = :id")
    java.util.Optional<CapabilityUpdateRequestEntity> findByIdForUpdate(@Param("id") UUID id);
    List<CapabilityUpdateRequestEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
