package com.company.casehub.audit.dto;

import com.company.casehub.audit.entity.AuditAction;
import com.company.casehub.audit.entity.AuditRecordEntity;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Read-only audit view for the Admin Audit page. Never exposes credentials,
 * session ids or storage internals because those are never persisted into
 * {@code audit_records}.
 */
public record AuditLogResponse(
        UUID id,
        Instant occurredAt,
        AuditAction action,
        UUID actorId,
        String actorUsername,
        String resourceType,
        String resourceId,
        String resourceLabel,
        Map<String, Object> detail) {

    public static AuditLogResponse from(AuditRecordEntity entity) {
        return new AuditLogResponse(entity.getId(), entity.getOccurredAt(), entity.getAction(),
                entity.getActorId(), entity.getActorUsername(), entity.getResourceType(),
                entity.getResourceId(), entity.getResourceLabel(), entity.getDetail());
    }
}
