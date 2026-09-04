-- Batch 4: project custom cases, request workflows, and safe custom runtime links.

CREATE TABLE IF NOT EXISTS casehub.project_custom_test_cases (
    id UUID NOT NULL,
    project_id UUID NOT NULL,
    case_code VARCHAR(100) NOT NULL,
    case_name VARCHAR(255) NOT NULL,
    test_purpose TEXT,
    preconditions TEXT,
    selection_mode VARCHAR(16) NOT NULL,
    evidence_required BOOLEAN NOT NULL DEFAULT FALSE,
    evidence_requirement TEXT,
    remark_requirement TEXT,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_project_custom_test_cases PRIMARY KEY (id),
    CONSTRAINT uq_project_custom_test_cases_code UNIQUE (project_id, case_code),
    CONSTRAINT uq_project_custom_test_cases_project_id UNIQUE (id, project_id),
    CONSTRAINT fk_custom_cases_project FOREIGN KEY (project_id) REFERENCES casehub.projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_custom_cases_created_by FOREIGN KEY (created_by) REFERENCES casehub.users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_custom_cases_updated_by FOREIGN KEY (updated_by) REFERENCES casehub.users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_custom_cases_selection CHECK (selection_mode IN ('SINGLE', 'MULTIPLE'))
);

CREATE TABLE IF NOT EXISTS casehub.custom_test_steps (
    id UUID NOT NULL,
    custom_test_case_id UUID NOT NULL,
    sequence_no INTEGER NOT NULL,
    title VARCHAR(255),
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_custom_test_steps PRIMARY KEY (id),
    CONSTRAINT uq_custom_test_steps_sequence UNIQUE (custom_test_case_id, sequence_no),
    CONSTRAINT fk_custom_test_steps_case FOREIGN KEY (custom_test_case_id) REFERENCES casehub.project_custom_test_cases(id) ON DELETE CASCADE,
    CONSTRAINT chk_custom_test_steps_sequence CHECK (sequence_no >= 1)
);

CREATE TABLE IF NOT EXISTS casehub.capability_update_requests (
    id UUID NOT NULL,
    project_id UUID NOT NULL,
    capability_id UUID NOT NULL,
    current_value VARCHAR(16) NOT NULL,
    proposed_value VARCHAR(16) NOT NULL,
    reason TEXT NOT NULL,
    evidence_reference TEXT,
    submitted_by UUID NOT NULL,
    reviewed_by UUID,
    review_comment TEXT,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_capability_update_requests PRIMARY KEY (id),
    CONSTRAINT fk_capability_requests_project FOREIGN KEY (project_id) REFERENCES casehub.projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_capability_requests_capability FOREIGN KEY (capability_id) REFERENCES casehub.capabilities(id) ON DELETE RESTRICT,
    CONSTRAINT fk_capability_requests_submitter FOREIGN KEY (submitted_by) REFERENCES casehub.users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_capability_requests_reviewer FOREIGN KEY (reviewed_by) REFERENCES casehub.users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_capability_requests_values CHECK (current_value IN ('YES', 'NO', 'UNKNOWN') AND proposed_value IN ('YES', 'NO', 'UNKNOWN')),
    CONSTRAINT chk_capability_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS casehub.test_case_change_requests (
    id UUID NOT NULL,
    master_test_case_id UUID NOT NULL,
    source_version_id UUID NOT NULL,
    reason TEXT NOT NULL,
    submitted_by UUID NOT NULL,
    reviewed_by UUID,
    review_comment TEXT,
    revision_draft_version_id UUID,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_test_case_change_requests PRIMARY KEY (id),
    CONSTRAINT fk_change_requests_master FOREIGN KEY (master_test_case_id) REFERENCES casehub.master_test_cases(id) ON DELETE CASCADE,
    CONSTRAINT fk_change_requests_source_version FOREIGN KEY (source_version_id) REFERENCES casehub.test_case_versions(id) ON DELETE RESTRICT,
    CONSTRAINT fk_change_requests_submitter FOREIGN KEY (submitted_by) REFERENCES casehub.users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_change_requests_reviewer FOREIGN KEY (reviewed_by) REFERENCES casehub.users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_change_requests_revision FOREIGN KEY (revision_draft_version_id) REFERENCES casehub.test_case_versions(id) ON DELETE SET NULL,
    CONSTRAINT chk_change_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

ALTER TABLE casehub.test_case_versions
    ADD CONSTRAINT fk_test_case_versions_change_request
    FOREIGN KEY (change_request_id) REFERENCES casehub.test_case_change_requests(id) ON DELETE SET NULL;

ALTER TABLE casehub.project_test_cases
    ALTER COLUMN master_test_case_id DROP NOT NULL,
    ALTER COLUMN test_case_version_id DROP NOT NULL,
    ADD COLUMN custom_test_case_id UUID;
ALTER TABLE casehub.project_test_cases
    DROP CONSTRAINT fk_project_test_cases_master,
    DROP CONSTRAINT fk_project_test_cases_version,
    ADD CONSTRAINT fk_project_test_cases_master FOREIGN KEY (master_test_case_id) REFERENCES casehub.master_test_cases(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_project_test_cases_version FOREIGN KEY (test_case_version_id, master_test_case_id) REFERENCES casehub.test_case_versions(id, master_test_case_id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_project_test_cases_custom FOREIGN KEY (custom_test_case_id, project_id) REFERENCES casehub.project_custom_test_cases(id, project_id) ON DELETE RESTRICT,
    ADD CONSTRAINT uq_project_custom_test_case UNIQUE (project_id, custom_test_case_id),
    ADD CONSTRAINT chk_project_test_case_backing CHECK (
        (master_test_case_id IS NOT NULL AND test_case_version_id IS NOT NULL AND custom_test_case_id IS NULL)
        OR (master_test_case_id IS NULL AND test_case_version_id IS NULL AND custom_test_case_id IS NOT NULL)
    );

ALTER TABLE casehub.decision_points
    ALTER COLUMN test_case_version_id DROP NOT NULL,
    ADD COLUMN custom_test_case_id UUID;
ALTER TABLE casehub.decision_points
    DROP CONSTRAINT fk_decision_points_version,
    ADD CONSTRAINT fk_decision_points_version FOREIGN KEY (test_case_version_id) REFERENCES casehub.test_case_versions(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_decision_points_custom FOREIGN KEY (custom_test_case_id) REFERENCES casehub.project_custom_test_cases(id) ON DELETE CASCADE,
    ADD CONSTRAINT chk_decision_points_backing CHECK (
        (test_case_version_id IS NOT NULL AND custom_test_case_id IS NULL)
        OR (test_case_version_id IS NULL AND custom_test_case_id IS NOT NULL)
    );
CREATE INDEX IF NOT EXISTS ix_decision_points_custom_case ON casehub.decision_points(custom_test_case_id, display_order);

ALTER TABLE casehub.transition_targets
    ALTER COLUMN target_master_test_case_id DROP NOT NULL,
    ADD COLUMN target_custom_test_case_id UUID;
ALTER TABLE casehub.transition_targets
    DROP CONSTRAINT fk_transition_targets_master,
    ADD CONSTRAINT fk_transition_targets_master FOREIGN KEY (target_master_test_case_id) REFERENCES casehub.master_test_cases(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_transition_targets_custom FOREIGN KEY (target_custom_test_case_id) REFERENCES casehub.project_custom_test_cases(id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_transition_targets_backing CHECK (
        (target_master_test_case_id IS NOT NULL AND target_custom_test_case_id IS NULL)
        OR (target_master_test_case_id IS NULL AND target_custom_test_case_id IS NOT NULL)
    );

ALTER TABLE casehub.branch_outcomes
    ADD COLUMN target_custom_test_case_id UUID;
ALTER TABLE casehub.branch_outcomes
    DROP CONSTRAINT fk_outcome_target,
    ADD CONSTRAINT fk_outcome_target_master FOREIGN KEY (target_master_test_case_id) REFERENCES casehub.master_test_cases(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_outcome_target_custom FOREIGN KEY (target_custom_test_case_id) REFERENCES casehub.project_custom_test_cases(id) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_branch_outcomes_target_backing CHECK (
        (target_master_test_case_id IS NOT NULL AND target_custom_test_case_id IS NULL)
        OR (target_master_test_case_id IS NULL AND target_custom_test_case_id IS NOT NULL)
        OR (target_master_test_case_id IS NULL AND target_custom_test_case_id IS NULL)
    );

ALTER TABLE casehub.project_test_case_triggers
    ALTER COLUMN source_test_case_version_id DROP NOT NULL,
    ADD CONSTRAINT chk_project_trigger_source_version CHECK (
        source_test_case_version_id IS NOT NULL OR source_decision_point_id IS NOT NULL
    );
