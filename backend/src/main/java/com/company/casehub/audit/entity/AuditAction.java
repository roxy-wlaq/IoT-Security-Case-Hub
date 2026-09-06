package com.company.casehub.audit.entity;

/**
 * Frozen Batch 5 (Phase 26) audit event catalog. One enum value per governance
 * event; the V018 CHECK constraint mirrors this list.
 *
 * <p>ROLE_CHANGE is part of the frozen catalog. The bootstrap ADMIN creation
 * ({@code BootstrapUserService}) and the interactive role-assignment workflow
 * ({@code UserManagementService}) both emit it.</p>
 *
 * <p>The USER_* values (V019) cover the admin user-management module: user
 * creation, profile update, enable / disable and password reset. Role
 * assignment itself keeps using ROLE_CHANGE.</p>
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
    EVIDENCE_DELETE,
    USER_CREATE,
    USER_UPDATE,
    USER_ENABLE,
    USER_DISABLE,
    USER_PASSWORD_RESET
}
