package com.company.casehub.audit.entity;

/**
 * Soft resource references used by {@link AuditRecordEntity}. Audit rows must
 * survive resource deletion, so these are plain labels persisted into
 * {@code audit_records.resource_type}, never foreign keys.
 */
public final class AuditResourceType {

    public static final String AUTHENTICATION = "AUTHENTICATION";
    public static final String USER = "USER";
    public static final String PROJECT = "PROJECT";
    public static final String TEST_CASE_VERSION = "TEST_CASE_VERSION";
    public static final String GENERATION_RULE = "GENERATION_RULE";
    public static final String CAPABILITY = "CAPABILITY";
    public static final String EVIDENCE = "EVIDENCE";

    private AuditResourceType() {
    }
}
