CREATE TABLE IF NOT EXISTS casehub.evidence (
    id UUID NOT NULL,
    project_test_case_id UUID NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(255),
    file_size BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    uploaded_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_evidence PRIMARY KEY (id),
    CONSTRAINT fk_evidence_ptc FOREIGN KEY (project_test_case_id) REFERENCES casehub.project_test_cases(id) ON DELETE CASCADE,
    CONSTRAINT fk_evidence_user FOREIGN KEY (uploaded_by) REFERENCES casehub.users(id) ON DELETE RESTRICT,
    CONSTRAINT uq_evidence_storage_key UNIQUE (storage_key)
);
CREATE INDEX IF NOT EXISTS ix_evidence_ptc ON casehub.evidence(project_test_case_id, created_at);

CREATE TABLE IF NOT EXISTS casehub.notes (
    id UUID NOT NULL,
    project_test_case_id UUID NOT NULL,
    author_id UUID NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_notes PRIMARY KEY (id),
    CONSTRAINT fk_notes_ptc FOREIGN KEY (project_test_case_id) REFERENCES casehub.project_test_cases(id) ON DELETE CASCADE,
    CONSTRAINT fk_notes_author FOREIGN KEY (author_id) REFERENCES casehub.users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_notes_body_not_blank CHECK (length(trim(body)) > 0)
);
CREATE INDEX IF NOT EXISTS ix_notes_ptc ON casehub.notes(project_test_case_id, created_at);

CREATE TABLE IF NOT EXISTS casehub.project_decision_selections (
    id UUID NOT NULL,
    project_test_case_id UUID NOT NULL,
    decision_point_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_project_decision_selections PRIMARY KEY (id),
    CONSTRAINT uq_project_decision_selection UNIQUE (project_test_case_id, decision_point_id),
    CONSTRAINT fk_selection_ptc FOREIGN KEY (project_test_case_id) REFERENCES casehub.project_test_cases(id) ON DELETE CASCADE,
    CONSTRAINT fk_selection_dp FOREIGN KEY (decision_point_id) REFERENCES casehub.decision_points(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS casehub.branch_outcomes (
    id UUID NOT NULL,
    project_test_case_id UUID NOT NULL,
    decision_point_id UUID NOT NULL,
    transition_type VARCHAR(32) NOT NULL,
    target_master_test_case_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_branch_outcomes PRIMARY KEY (id),
    CONSTRAINT uq_branch_outcome UNIQUE (project_test_case_id, decision_point_id, target_master_test_case_id),
    CONSTRAINT fk_outcome_ptc FOREIGN KEY (project_test_case_id) REFERENCES casehub.project_test_cases(id) ON DELETE CASCADE,
    CONSTRAINT fk_outcome_dp FOREIGN KEY (decision_point_id) REFERENCES casehub.decision_points(id) ON DELETE RESTRICT,
    CONSTRAINT fk_outcome_target FOREIGN KEY (target_master_test_case_id) REFERENCES casehub.master_test_cases(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS casehub.project_test_case_triggers (
    id UUID NOT NULL,
    source_project_test_case_id UUID NOT NULL,
    source_test_case_version_id UUID NOT NULL,
    source_decision_point_id UUID NOT NULL,
    target_project_test_case_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_project_test_case_triggers PRIMARY KEY (id),
    CONSTRAINT uq_project_test_case_trigger UNIQUE (source_project_test_case_id, source_decision_point_id, target_project_test_case_id),
    CONSTRAINT fk_trigger_source_ptc FOREIGN KEY (source_project_test_case_id) REFERENCES casehub.project_test_cases(id) ON DELETE CASCADE,
    CONSTRAINT fk_trigger_source_version FOREIGN KEY (source_test_case_version_id) REFERENCES casehub.test_case_versions(id) ON DELETE RESTRICT,
    CONSTRAINT fk_trigger_source_dp FOREIGN KEY (source_decision_point_id) REFERENCES casehub.decision_points(id) ON DELETE RESTRICT,
    CONSTRAINT fk_trigger_target_ptc FOREIGN KEY (target_project_test_case_id) REFERENCES casehub.project_test_cases(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS ix_project_test_case_triggers_target ON casehub.project_test_case_triggers(target_project_test_case_id);
