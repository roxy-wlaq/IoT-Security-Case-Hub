package com.company.casehub.audit.entity;

/**
 * Frozen Batch 5 (Phase 26) audit event catalog. One enum value per governance
 * event; the V018 CHECK constraint mirrors this list.
 *
 * <p>ROLE_CHANGE is part of the frozen catalog: the only authoritative role
 * mutation point in V1 is the bootstrap ADMIN creation ({@code BootstrapUserService});
 * no interactive role-management workflow exists yet, so no other production
 * call site emits this event.</p>
 */
public enum AuditAction {
    LOGIN,
    LOGIN_FAILURE,
    ROLE_CHANGE,
    PROJECT_CREATE,
    PROJECT_ARCHIVE,
    TEST_CASE_PUBLISH,
    TEST_CASE_DEPRECATE,
    GENERATION_RULE_UPDATE,
    CAPABILITY_LIBRARY_UPDATE,
    EVIDENCE_DELETE
}
