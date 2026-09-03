-- V009: Phase 8 Decision Point / Master Test Case DAG template.
-- Targets intentionally reference Master Test Cases, not runtime versions.

CREATE TABLE IF NOT EXISTS casehub.decision_points (
    id                    UUID NOT NULL,
    test_case_version_id  UUID NOT NULL,
    name                  VARCHAR(255) NOT NULL,
    description           TEXT,
    display_order         INTEGER NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_decision_points PRIMARY KEY (id),
    CONSTRAINT uq_decision_points_order UNIQUE (test_case_version_id, display_order),
    CONSTRAINT uq_decision_points_version_id UNIQUE (id, test_case_version_id),
    CONSTRAINT fk_decision_points_version FOREIGN KEY (test_case_version_id)
        REFERENCES casehub.test_case_versions (id) ON DELETE CASCADE,
    CONSTRAINT chk_decision_points_order CHECK (display_order >= 1)
);

CREATE TABLE IF NOT EXISTS casehub.transitions (
    id                  UUID NOT NULL,
    decision_point_id   UUID NOT NULL,
    type                VARCHAR(32) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_transitions PRIMARY KEY (id),
    CONSTRAINT uq_transitions_decision_point UNIQUE (decision_point_id),
    CONSTRAINT fk_transitions_decision_point FOREIGN KEY (decision_point_id)
        REFERENCES casehub.decision_points (id) ON DELETE CASCADE,
    CONSTRAINT chk_transitions_type CHECK (type IN ('NEXT_CASE', 'NEXT_CASES', 'PASS', 'FAIL', 'N_A'))
);

CREATE TABLE IF NOT EXISTS casehub.transition_targets (
    id                       UUID NOT NULL,
    transition_id            UUID NOT NULL,
    target_order             INTEGER NOT NULL,
    target_master_test_case_id UUID NOT NULL,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_transition_targets PRIMARY KEY (id),
    CONSTRAINT uq_transition_targets_order UNIQUE (transition_id, target_order),
    CONSTRAINT uq_transition_targets_master UNIQUE (transition_id, target_master_test_case_id),
    CONSTRAINT fk_transition_targets_transition FOREIGN KEY (transition_id)
        REFERENCES casehub.transitions (id) ON DELETE CASCADE,
    CONSTRAINT fk_transition_targets_master FOREIGN KEY (target_master_test_case_id)
        REFERENCES casehub.master_test_cases (id) ON DELETE RESTRICT,
    CONSTRAINT chk_transition_targets_order CHECK (target_order >= 1)
);

CREATE INDEX IF NOT EXISTS ix_decision_points_version ON casehub.decision_points (test_case_version_id, display_order);
CREATE INDEX IF NOT EXISTS ix_transition_targets_master ON casehub.transition_targets (target_master_test_case_id);
