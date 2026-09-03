CREATE TABLE IF NOT EXISTS casehub.project_test_cases (
    id UUID NOT NULL,
    project_id UUID NOT NULL,
    master_test_case_id UUID NOT NULL,
    test_case_version_id UUID NOT NULL,
    execution_status VARCHAR(24) NOT NULL DEFAULT 'NOT_STARTED',
    relation_status VARCHAR(24) NOT NULL DEFAULT 'FLOATING',
    is_root BOOLEAN NOT NULL DEFAULT FALSE,
    removed BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID NOT NULL,
    last_modified_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_project_test_cases PRIMARY KEY (id),
    CONSTRAINT uq_project_master_test_case UNIQUE (project_id, master_test_case_id),
    CONSTRAINT fk_project_test_cases_project FOREIGN KEY (project_id) REFERENCES casehub.projects(id) ON DELETE RESTRICT,
    CONSTRAINT fk_project_test_cases_master FOREIGN KEY (master_test_case_id) REFERENCES casehub.master_test_cases(id) ON DELETE RESTRICT,
    CONSTRAINT fk_project_test_cases_version FOREIGN KEY (test_case_version_id, master_test_case_id) REFERENCES casehub.test_case_versions(id, master_test_case_id) ON DELETE RESTRICT,
    CONSTRAINT fk_project_test_cases_created_by FOREIGN KEY (created_by) REFERENCES casehub.users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_project_test_cases_modified_by FOREIGN KEY (last_modified_by) REFERENCES casehub.users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_project_test_cases_execution_status CHECK (execution_status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT chk_project_test_cases_relation_status CHECK (relation_status IN ('CONNECTED', 'FLOATING'))
);
CREATE INDEX IF NOT EXISTS ix_project_test_cases_project_status ON casehub.project_test_cases(project_id, removed, execution_status);
CREATE INDEX IF NOT EXISTS ix_project_test_cases_project_relation ON casehub.project_test_cases(project_id, relation_status);

ALTER TABLE casehub.generation_recommendations
    ADD CONSTRAINT fk_generation_recommendations_added_ptc
    FOREIGN KEY (added_project_test_case_id) REFERENCES casehub.project_test_cases(id) ON DELETE RESTRICT;

CREATE TABLE IF NOT EXISTS casehub.project_test_case_sources (
    id UUID NOT NULL,
    project_test_case_id UUID NOT NULL,
    source_type VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_project_test_case_sources PRIMARY KEY (id),
    CONSTRAINT uq_project_test_case_sources UNIQUE (project_test_case_id, source_type),
    CONSTRAINT fk_project_test_case_sources_ptc FOREIGN KEY (project_test_case_id) REFERENCES casehub.project_test_cases(id) ON DELETE CASCADE,
    CONSTRAINT chk_project_test_case_sources_type CHECK (source_type IN ('INITIAL', 'GENERATED', 'PROGRESSIVE', 'MANUAL', 'CUSTOM'))
);

CREATE TABLE IF NOT EXISTS casehub.project_test_case_assignees (
    id UUID NOT NULL,
    project_test_case_id UUID NOT NULL,
    user_id UUID NOT NULL,
    first_viewed_at TIMESTAMPTZ,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_project_test_case_assignees PRIMARY KEY (id),
    CONSTRAINT uq_project_test_case_assignees UNIQUE (project_test_case_id, user_id),
    CONSTRAINT fk_project_test_case_assignees_ptc FOREIGN KEY (project_test_case_id) REFERENCES casehub.project_test_cases(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_test_case_assignees_user FOREIGN KEY (user_id) REFERENCES casehub.users(id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS ix_project_test_case_assignees_user ON casehub.project_test_case_assignees(user_id, project_test_case_id);
