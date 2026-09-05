package com.company.casehub.audit.controller;

import com.company.casehub.audit.dto.AuditLogQuery;
import com.company.casehub.audit.dto.AuditLogResponse;
import com.company.casehub.audit.entity.AuditAction;
import com.company.casehub.audit.service.AuditService;
import com.company.casehub.testcase.dto.PagedResponse;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Audit page backend (Phase 26). Read-oriented only: {@code audit:read}
 * is seeded for ADMIN exclusively (V002), and no update/delete endpoint exists
 * for audit records — they are governance history, not application data.
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    public PagedResponse<AuditLogResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String actorUsername,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return auditService.query(new AuditLogQuery(page, size, action, resourceType, resourceId,
                actorUsername, from, to));
    }
}
