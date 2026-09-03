CREATE TABLE IF NOT EXISTS casehub.generation_rules (
    id UUID NOT NULL,
    rule_code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'ENABLED',
    description TEXT,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_generation_rules PRIMARY KEY (id),
    CONSTRAINT uq_generation_rules_code UNIQUE (rule_code),
    CONSTRAINT fk_generation_rules_created_by FOREIGN KEY (created_by) REFERENCES casehub.users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_generation_rules_mode CHECK (mode IN ('FULL', 'PROGRESSIVE_INITIAL', 'BOTH')),
    CONSTRAINT chk_generation_rules_status CHECK (status IN ('ENABLED', 'DISABLED'))
);

CREATE TABLE IF NOT EXISTS casehub.generation_condition_groups (
    id UUID NOT NULL,
    rule_id UUID NOT NULL,
    parent_group_id UUID,
    logic_operator VARCHAR(8) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_generation_condition_groups PRIMARY KEY (id),
    CONSTRAINT uq_generation_condition_groups_rule_id UNIQUE (id, rule_id),
    CONSTRAINT fk_generation_condition_groups_rule FOREIGN KEY (rule_id) REFERENCES casehub.generation_rules(id) ON DELETE CASCADE,
    CONSTRAINT fk_generation_condition_groups_parent FOREIGN KEY (parent_group_id, rule_id) REFERENCES casehub.generation_condition_groups(id, rule_id) ON DELETE CASCADE,
    CONSTRAINT chk_generation_condition_groups_operator CHECK (logic_operator IN ('AND', 'OR'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_generation_rule_root_group
    ON casehub.generation_condition_groups(rule_id) WHERE parent_group_id IS NULL;

CREATE TABLE IF NOT EXISTS casehub.generation_conditions (
    id UUID NOT NULL,
    group_id UUID NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    capability_id UUID,
    standard_task_type_id UUID,
    operator VARCHAR(32) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_generation_conditions PRIMARY KEY (id),
    CONSTRAINT fk_generation_conditions_group FOREIGN KEY (group_id) REFERENCES casehub.generation_condition_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_generation_conditions_capability FOREIGN KEY (capability_id) REFERENCES casehub.capabilities(id) ON DELETE RESTRICT,
    CONSTRAINT fk_generation_conditions_standard FOREIGN KEY (standard_task_type_id) REFERENCES casehub.standard_task_types(id) ON DELETE RESTRICT,
    CONSTRAINT chk_generation_conditions_target CHECK (target_type IN ('CAPABILITY', 'STANDARD_TASK_TYPE')),
    CONSTRAINT chk_generation_conditions_operator CHECK (operator IN ('EQ_YES', 'EQ_NO', 'EQ_UNKNOWN', 'NE_NO', 'NE_YES', 'PRESENT')),
    CONSTRAINT chk_generation_conditions_target_xor CHECK (
        (target_type = 'CAPABILITY' AND capability_id IS NOT NULL AND standard_task_type_id IS NULL)
        OR (target_type = 'STANDARD_TASK_TYPE' AND capability_id IS NULL AND standard_task_type_id IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS casehub.generation_rule_outputs (
    id UUID NOT NULL,
    rule_id UUID NOT NULL,
    master_test_case_id UUID NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_generation_rule_outputs PRIMARY KEY (id),
    CONSTRAINT uq_generation_rule_outputs UNIQUE (rule_id, master_test_case_id),
    CONSTRAINT fk_generation_rule_outputs_rule FOREIGN KEY (rule_id) REFERENCES casehub.generation_rules(id) ON DELETE CASCADE,
    CONSTRAINT fk_generation_rule_outputs_master FOREIGN KEY (master_test_case_id) REFERENCES casehub.master_test_cases(id) ON DELETE RESTRICT
);
