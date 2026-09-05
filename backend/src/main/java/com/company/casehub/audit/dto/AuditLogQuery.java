package com.company.casehub.audit.dto;

import com.company.casehub.audit.entity.AuditAction;
import java.time.Instant;

/**
 * Audit page query filters (Phase 26). All filters are optional and combined
 * with AND; ordering is fixed to occurredAt descending.
 */
public record AuditLogQuery(
        int page,
        int size,
        AuditAction action,
        String resourceType,
        String resourceId,
        String actorUsername,
        Instant occurredFrom,
        Instant occurredTo) {
}
