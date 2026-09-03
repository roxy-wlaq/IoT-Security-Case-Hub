CREATE TABLE IF NOT EXISTS casehub.projects (
    id UUID NOT NULL,
    project_number VARCHAR(64) NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    device_name VARCHAR(255) NOT NULL,
    generation_mode VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT uq_projects_number UNIQUE (project_number),
    CONSTRAINT fk_projects_created_by FOREIGN KEY (created_by) REFERENCES casehub.users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_projects_generation_mode CHECK (generation_mode IN ('FULL', 'PROGRESSIVE')),
    CONSTRAINT chk_projects_status CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'ARCHIVED'))
);
CREATE INDEX IF NOT EXISTS ix_projects_status ON casehub.projects(status);
CREATE INDEX IF NOT EXISTS ix_projects_created_at ON casehub.projects(created_at);

CREATE TABLE IF NOT EXISTS casehub.project_standards (
    id UUID NOT NULL,
    project_id UUID NOT NULL,
    standard_task_type_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_project_standards PRIMARY KEY (id),
    CONSTRAINT uq_project_standards UNIQUE (project_id, standard_task_type_id),
    CONSTRAINT fk_project_standards_project FOREIGN KEY (project_id) REFERENCES casehub.projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_standards_standard FOREIGN KEY (standard_task_type_id) REFERENCES casehub.standard_task_types(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS casehub.project_coordinators (
    id UUID NOT NULL,
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_project_coordinators PRIMARY KEY (id),
    CONSTRAINT uq_project_coordinators UNIQUE (project_id, user_id),
    CONSTRAINT fk_project_coordinators_project FOREIGN KEY (project_id) REFERENCES casehub.projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_coordinators_user FOREIGN KEY (user_id) REFERENCES casehub.users(id) ON DELETE RESTRICT
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_project_primary_coordinator
    ON casehub.project_coordinators(project_id) WHERE is_primary = TRUE;
CREATE INDEX IF NOT EXISTS ix_project_coordinators_user ON casehub.project_coordinators(user_id);
