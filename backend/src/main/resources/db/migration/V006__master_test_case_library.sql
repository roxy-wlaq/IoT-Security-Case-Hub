-- V006: Phase 6 Master Test Case library foundation.
-- Migration ownership remains with the Lead. Do not edit V001-V005.

CREATE TABLE IF NOT EXISTS casehub.master_test_cases (
    id          UUID NOT NULL,
    case_code   VARCHAR(100) NOT NULL,
    category_id UUID NOT NULL,
    created_by  UUID NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_master_test_cases PRIMARY KEY (id),
    CONSTRAINT uq_master_test_cases_case_code UNIQUE (case_code),
    CONSTRAINT fk_master_test_cases_category FOREIGN KEY (category_id)
        REFERENCES casehub.categories (id) ON DELETE RESTRICT,
    CONSTRAINT fk_master_test_cases_created_by FOREIGN KEY (created_by)
        REFERENCES casehub.users (id) ON DELETE RESTRICT
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_master_test_cases_case_code_lower
    ON casehub.master_test_cases (LOWER(case_code));
CREATE INDEX IF NOT EXISTS ix_master_test_cases_category ON casehub.master_test_cases (category_id);
CREATE INDEX IF NOT EXISTS ix_master_test_cases_created_by ON casehub.master_test_cases (created_by);
CREATE INDEX IF NOT EXISTS ix_master_test_cases_enabled ON casehub.master_test_cases (enabled);

CREATE TABLE IF NOT EXISTS casehub.test_case_versions (
    id                    UUID NOT NULL,
    master_test_case_id   UUID NOT NULL,
    version_major         INTEGER NOT NULL,
    version_minor         INTEGER NOT NULL,
    status                VARCHAR(32) NOT NULL,
    is_current_version    BOOLEAN NOT NULL DEFAULT FALSE,
    case_name             VARCHAR(255) NOT NULL,
    test_purpose          TEXT,
    preconditions         TEXT,
    selection_mode        VARCHAR(16) NOT NULL,
    evidence_required     BOOLEAN NOT NULL DEFAULT FALSE,
    evidence_requirement  TEXT,
    remark_requirement    TEXT,
    progressive_role      VARCHAR(16),
    based_on_version_id   UUID,
    change_request_id     UUID,
    change_reason         TEXT,
    created_by            UUID NOT NULL,
    reviewed_by           UUID,
    published_at          TIMESTAMPTZ,
    deprecated_at         TIMESTAMPTZ,
    revision_closed       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_test_case_versions PRIMARY KEY (id),
    CONSTRAINT uq_test_case_versions_number UNIQUE (master_test_case_id, version_major, version_minor),
    CONSTRAINT uq_test_case_versions_id_master UNIQUE (id, master_test_case_id),
    CONSTRAINT fk_test_case_versions_master FOREIGN KEY (master_test_case_id)
        REFERENCES casehub.master_test_cases (id) ON DELETE CASCADE,
    CONSTRAINT fk_test_case_versions_based_on FOREIGN KEY (based_on_version_id)
        REFERENCES casehub.test_case_versions (id) ON DELETE RESTRICT,
    CONSTRAINT fk_test_case_versions_created_by FOREIGN KEY (created_by)
        REFERENCES casehub.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_test_case_versions_reviewed_by FOREIGN KEY (reviewed_by)
        REFERENCES casehub.users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_test_case_versions_major CHECK (version_major >= 1),
    CONSTRAINT chk_test_case_versions_minor CHECK (version_minor >= 0),
    CONSTRAINT chk_test_case_versions_status CHECK (status IN ('DRAFT', 'REVIEW', 'PUBLISHED', 'DEPRECATED')),
    CONSTRAINT chk_test_case_versions_selection CHECK (selection_mode IN ('SINGLE', 'MULTIPLE')),
    CONSTRAINT chk_test_case_versions_progressive_role CHECK (progressive_role IS NULL OR progressive_role IN ('ENTRY', 'NORMAL')),
    CONSTRAINT chk_test_case_versions_current_published CHECK (is_current_version = FALSE OR status = 'PUBLISHED')
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_test_case_current_version
    ON casehub.test_case_versions (master_test_case_id) WHERE is_current_version = TRUE;
CREATE INDEX IF NOT EXISTS ix_test_case_versions_master ON casehub.test_case_versions (master_test_case_id);
CREATE INDEX IF NOT EXISTS ix_test_case_versions_status ON casehub.test_case_versions (status);
CREATE INDEX IF NOT EXISTS ix_test_case_versions_current ON casehub.test_case_versions (is_current_version);
CREATE INDEX IF NOT EXISTS ix_test_case_versions_created_by ON casehub.test_case_versions (created_by);
CREATE INDEX IF NOT EXISTS idx_test_case_version_name_trgm
    ON casehub.test_case_versions USING GIN (case_name gin_trgm_ops);

CREATE TABLE IF NOT EXISTS casehub.test_steps (
    id                  UUID NOT NULL,
    test_case_version_id UUID NOT NULL,
    sequence_no         INTEGER NOT NULL,
    title               VARCHAR(255),
    content             TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_test_steps PRIMARY KEY (id),
    CONSTRAINT uq_test_steps_sequence UNIQUE (test_case_version_id, sequence_no),
    CONSTRAINT fk_test_steps_version FOREIGN KEY (test_case_version_id)
        REFERENCES casehub.test_case_versions (id) ON DELETE CASCADE,
    CONSTRAINT chk_test_steps_sequence CHECK (sequence_no >= 1)
);
CREATE INDEX IF NOT EXISTS idx_test_steps_content_trgm
    ON casehub.test_steps USING GIN (content gin_trgm_ops);

CREATE TABLE IF NOT EXISTS casehub.test_case_tags (
    id                  UUID NOT NULL,
    master_test_case_id UUID NOT NULL,
    tag_id              UUID NOT NULL,
    CONSTRAINT pk_test_case_tags PRIMARY KEY (id),
    CONSTRAINT uq_test_case_tags UNIQUE (master_test_case_id, tag_id),
    CONSTRAINT fk_test_case_tags_master FOREIGN KEY (master_test_case_id)
        REFERENCES casehub.master_test_cases (id) ON DELETE CASCADE,
    CONSTRAINT fk_test_case_tags_tag FOREIGN KEY (tag_id)
        REFERENCES casehub.tags (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS casehub.test_case_tools (
    id                  UUID NOT NULL,
    test_case_version_id UUID NOT NULL,
    tool_id             UUID NOT NULL,
    sort_order          INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_test_case_tools PRIMARY KEY (id),
    CONSTRAINT uq_test_case_tools UNIQUE (test_case_version_id, tool_id),
    CONSTRAINT fk_test_case_tools_version FOREIGN KEY (test_case_version_id)
        REFERENCES casehub.test_case_versions (id) ON DELETE CASCADE,
    CONSTRAINT fk_test_case_tools_tool FOREIGN KEY (tool_id)
        REFERENCES casehub.tools (id) ON DELETE RESTRICT,
    CONSTRAINT chk_test_case_tools_sort_order CHECK (sort_order >= 0)
);

CREATE TABLE IF NOT EXISTS casehub.test_case_standard_mappings (
    id                    UUID NOT NULL,
    test_case_version_id  UUID NOT NULL,
    standard_task_type_id UUID NOT NULL,
    mapping_note          TEXT,
    CONSTRAINT pk_test_case_standard_mappings PRIMARY KEY (id),
    CONSTRAINT uq_test_case_standard_mappings UNIQUE (test_case_version_id, standard_task_type_id),
    CONSTRAINT fk_test_case_standard_mappings_version FOREIGN KEY (test_case_version_id)
        REFERENCES casehub.test_case_versions (id) ON DELETE CASCADE,
    CONSTRAINT fk_test_case_standard_mappings_standard FOREIGN KEY (standard_task_type_id)
        REFERENCES casehub.standard_task_types (id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS casehub.test_case_attachments (
    id                  UUID NOT NULL,
    test_case_version_id UUID NOT NULL,
    original_filename   VARCHAR(255) NOT NULL,
    storage_key         VARCHAR(512) NOT NULL,
    file_size           BIGINT NOT NULL,
    content_type        VARCHAR(150),
    sha256              VARCHAR(64),
    description         TEXT,
    uploaded_by         UUID NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_test_case_attachments PRIMARY KEY (id),
    CONSTRAINT uq_test_case_attachments_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_test_case_attachments_version FOREIGN KEY (test_case_version_id)
        REFERENCES casehub.test_case_versions (id) ON DELETE CASCADE,
    CONSTRAINT fk_test_case_attachments_uploaded_by FOREIGN KEY (uploaded_by)
        REFERENCES casehub.users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_test_case_attachments_file_size CHECK (file_size >= 0)
);
