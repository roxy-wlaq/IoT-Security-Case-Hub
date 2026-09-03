CREATE TABLE IF NOT EXISTS casehub.project_capabilities (
    id UUID NOT NULL,
    project_id UUID NOT NULL,
    capability_id UUID NOT NULL,
    value VARCHAR(16) NOT NULL,
    source VARCHAR(40) NOT NULL,
    is_derived BOOLEAN NOT NULL DEFAULT FALSE,
    comment TEXT,
    updated_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_project_capabilities PRIMARY KEY (id),
    CONSTRAINT uq_project_capabilities UNIQUE (project_id, capability_id),
    CONSTRAINT fk_project_capabilities_project FOREIGN KEY (project_id) REFERENCES casehub.projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_capabilities_capability FOREIGN KEY (capability_id) REFERENCES casehub.capabilities(id) ON DELETE RESTRICT,
    CONSTRAINT fk_project_capabilities_updated_by FOREIGN KEY (updated_by) REFERENCES casehub.users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_project_capabilities_value CHECK (value IN ('YES', 'NO', 'UNKNOWN')),
    CONSTRAINT chk_project_capabilities_source CHECK (source IN ('CUSTOMER_PROVIDED', 'TESTER_DISCOVERED', 'DOCUMENT', 'AUTOMATIC_DETECTION', 'COORDINATOR_INPUT', 'DERIVED', 'OTHER'))
);
CREATE INDEX IF NOT EXISTS ix_project_capabilities_project ON casehub.project_capabilities(project_id);
