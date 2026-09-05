package com.company.casehub.audit.entity;

import com.company.casehub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only governance record (Phase 26). Application users never receive
 * update/delete semantics for this entity: no service writes it except
 * {@code AuditService.record}, and the query API is read-only.
 */
@Entity
@Table(name = "audit_records", schema = "casehub")
@Getter
@Setter
@NoArgsConstructor
public class AuditRecordEntity extends BaseEntity {

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 48)
    private AuditAction action;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_username", nullable = false, length = 100)
    private String actorUsername;

    @Column(name = "resource_type", nullable = false, length = 48)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "resource_label", length = 255)
    private String resourceLabel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail", columnDefinition = "jsonb")
    private Map<String, Object> detail;
}
