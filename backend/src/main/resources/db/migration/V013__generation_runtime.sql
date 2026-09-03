CREATE TABLE IF NOT EXISTS casehub.generation_runs (
    id UUID NOT NULL,
    project_id UUID NOT NULL,
    mode VARCHAR(32) NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    executed_by UUID NOT NULL,
    executed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_generation_runs PRIMARY KEY (id),
    CONSTRAINT fk_generation_runs_project FOREIGN KEY (project_id) REFERENCES casehub.projects(id) ON DELETE RESTRICT,
    CONSTRAINT fk_generation_runs_executed_by FOREIGN KEY (executed_by) REFERENCES casehub.users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_generation_runs_mode CHECK (mode IN ('FULL', 'PROGRESSIVE_INITIAL')),
    CONSTRAINT chk_generation_runs_trigger CHECK (trigger_type IN ('PROJECT_INITIAL', 'CAPABILITY_UPDATE', 'STANDARD_CHANGE', 'MANUAL_REGENERATE'))
);
CREATE INDEX IF NOT EXISTS ix_generation_runs_project_time ON casehub.generation_runs(project_id, executed_at DESC);

CREATE TABLE IF NOT EXISTS casehub.generation_recommendations (
    id UUID NOT NULL,
    generation_run_id UUID NOT NULL,
    master_test_case_id UUID NOT NULL,
    resolved_test_case_version_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    added_project_test_case_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_generation_recommendations PRIMARY KEY (id),
    CONSTRAINT uq_generation_recommendations UNIQUE (generation_run_id, master_test_case_id),
    CONSTRAINT fk_generation_recommendations_run FOREIGN KEY (generation_run_id) REFERENCES casehub.generation_runs(id) ON DELETE CASCADE,
    CONSTRAINT fk_generation_recommendations_master FOREIGN KEY (master_test_case_id) REFERENCES casehub.master_test_cases(id) ON DELETE RESTRICT,
    CONSTRAINT fk_generation_recommendations_version FOREIGN KEY (resolved_test_case_version_id) REFERENCES casehub.test_case_versions(id) ON DELETE RESTRICT,
    CONSTRAINT chk_generation_recommendations_status CHECK (status IN ('NEW', 'ADDED', 'IGNORED'))
);

CREATE TABLE IF NOT EXISTS casehub.generation_recommendation_rules (
    id UUID NOT NULL,
    recommendation_id UUID NOT NULL,
    generation_rule_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_generation_recommendation_rules PRIMARY KEY (id),
    CONSTRAINT uq_generation_recommendation_rules UNIQUE (recommendation_id, generation_rule_id),
    CONSTRAINT fk_generation_recommendation_rules_recommendation FOREIGN KEY (recommendation_id) REFERENCES casehub.generation_recommendations(id) ON DELETE CASCADE,
    CONSTRAINT fk_generation_recommendation_rules_rule FOREIGN KEY (generation_rule_id) REFERENCES casehub.generation_rules(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS casehub.project_test_case_preferences (
    id UUID NOT NULL,
    project_id UUID NOT NULL,
    master_test_case_id UUID NOT NULL,
    state VARCHAR(20) NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_project_test_case_preferences PRIMARY KEY (id),
    CONSTRAINT uq_project_test_case_preferences UNIQUE (project_id, master_test_case_id),
    CONSTRAINT fk_project_test_case_preferences_project FOREIGN KEY (project_id) REFERENCES casehub.projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_test_case_preferences_master FOREIGN KEY (master_test_case_id) REFERENCES casehub.master_test_cases(id) ON DELETE RESTRICT,
    CONSTRAINT fk_project_test_case_preferences_updated_by FOREIGN KEY (updated_by) REFERENCES casehub.users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_project_test_case_preferences_state CHECK (state IN ('IGNORED'))
);
