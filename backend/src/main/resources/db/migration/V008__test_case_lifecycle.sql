-- V008: Phase 7 Test Case Lifecycle.
-- Adds the formal review history (append-only) and revision contributor tables.
-- Migration ownership remains with the Lead. V001-V007 are not modified.

-- ---------------------------------------------------------------------------
-- test_case_review_records
-- ---------------------------------------------------------------------------
-- Append-only audit trail of every lifecycle action performed on a version.
-- A version's "latest ReviewRecord" (by created_at) is used to derive UI
-- business labels such as "Rejected" without introducing a REJECTED version
-- status (Final Technical Review V1.0 §11).
--
-- Declared deviation from Database Schema V1.0 §33.1: the original 3-value
-- CHECK ('PUBLISH','RETURN','REJECT') cannot express the SUBMIT action that
-- starts the audit chain, nor the DEPRECATE action required by the task book
-- §14. Since this table did not exist in V001-V007, V008 builds it directly;
-- the 5-value CHECK is an intentional, documented extension of §33.1, not a
-- rewrite of a frozen constraint. Recorded in IMPLEMENTATION_STATUS.md.
CREATE TABLE IF NOT EXISTS casehub.test_case_review_records (
    id                    UUID        NOT NULL,
    test_case_version_id  UUID        NOT NULL,
    action                VARCHAR(32) NOT NULL,
    reviewer_id           UUID        NOT NULL,
    comment               TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_test_case_review_records PRIMARY KEY (id),
    CONSTRAINT fk_review_records_version FOREIGN KEY (test_case_version_id)
        REFERENCES casehub.test_case_versions (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_records_reviewer FOREIGN KEY (reviewer_id)
        REFERENCES casehub.users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_review_records_action CHECK (action IN ('SUBMIT', 'PUBLISH', 'RETURN', 'REJECT', 'DEPRECATE'))
);
CREATE INDEX IF NOT EXISTS ix_review_records_version
    ON casehub.test_case_review_records (test_case_version_id, created_at);
CREATE INDEX IF NOT EXISTS ix_review_records_reviewer
    ON casehub.test_case_review_records (reviewer_id);

-- ---------------------------------------------------------------------------
-- revision_contributors
-- ---------------------------------------------------------------------------
-- Grants a specific user temporary edit access to one Draft version,
-- distinct from the Draft owner (Data Model §50). Resource-level RBAC in
-- TestCaseAccessPolicy checks membership here for edit/submit permissions.
CREATE TABLE IF NOT EXISTS casehub.revision_contributors (
    id                    UUID        NOT NULL,
    test_case_version_id  UUID        NOT NULL,
    user_id               UUID        NOT NULL,
    added_by              UUID        NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_revision_contributors PRIMARY KEY (id),
    CONSTRAINT uq_revision_contributors UNIQUE (test_case_version_id, user_id),
    CONSTRAINT fk_revision_contributors_version FOREIGN KEY (test_case_version_id)
        REFERENCES casehub.test_case_versions (id) ON DELETE CASCADE,
    CONSTRAINT fk_revision_contributors_user FOREIGN KEY (user_id)
        REFERENCES casehub.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_revision_contributors_added_by FOREIGN KEY (added_by)
        REFERENCES casehub.users (id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS ix_revision_contributors_version
    ON casehub.revision_contributors (test_case_version_id);
CREATE INDEX IF NOT EXISTS ix_revision_contributors_user
    ON casehub.revision_contributors (user_id);
