-- Batch 5 (Phase 26): append-only audit trail for governance events.
--
-- Governance history must survive the deletion of the actor or the resource,
-- so this table deliberately carries NO foreign keys to business tables.
-- Actor identity is denormalized (actor_id + actor_username) and resources are
-- referenced by (resource_type, resource_id) soft references only.

CREATE TABLE IF NOT EXISTS casehub.audit_records (
    id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    action VARCHAR(48) NOT NULL,
    actor_id UUID,
    actor_username VARCHAR(100) NOT NULL,
    resource_type VARCHAR(48) NOT NULL,
    resource_id VARCHAR(100),
    resource_label VARCHAR(255),
    detail JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_audit_records PRIMARY KEY (id),
    CONSTRAINT chk_audit_records_action CHECK (action IN (
        'LOGIN', 'LOGIN_FAILURE', 'ROLE_CHANGE', 'PROJECT_CREATE', 'PROJECT_ARCHIVE',
        'TEST_CASE_PUBLISH', 'TEST_CASE_DEPRECATE', 'GENERATION_RULE_UPDATE',
        'CAPABILITY_LIBRARY_UPDATE', 'EVIDENCE_DELETE')),
    CONSTRAINT chk_audit_records_actor_username CHECK (length(trim(actor_username)) > 0),
    CONSTRAINT chk_audit_records_actor CHECK (action = 'LOGIN_FAILURE' OR actor_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS ix_audit_records_occurred_at ON casehub.audit_records (occurred_at DESC);
CREATE INDEX IF NOT EXISTS ix_audit_records_action ON casehub.audit_records (action);
CREATE INDEX IF NOT EXISTS ix_audit_records_actor ON casehub.audit_records (actor_id);
CREATE INDEX IF NOT EXISTS ix_audit_records_resource ON casehub.audit_records (resource_type, resource_id);
