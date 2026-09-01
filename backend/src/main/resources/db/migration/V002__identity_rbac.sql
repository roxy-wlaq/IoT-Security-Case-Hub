-- V002: Identity & RBAC tables + seed (Final Technical Review / Security RBAC Detail §33-37)
-- All migrations are owned by the Lead (Migration Version Owner). Do not edit an
-- already-published migration; add a new one instead.

CREATE TABLE IF NOT EXISTS casehub.users (
    id                  UUID        NOT NULL,
    username            VARCHAR(100) NOT NULL,
    display_name        VARCHAR(150) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    enabled             BOOLEAN     NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN    NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username      ON casehub.users (username);
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username_lower ON casehub.users (LOWER(username));

CREATE TABLE IF NOT EXISTS casehub.roles (
    id          UUID        NOT NULL,
    code        VARCHAR(64) NOT NULL,
    name        VARCHAR(100),
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS casehub.permissions (
    id          UUID        NOT NULL,
    code        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uq_permissions_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS casehub.user_roles (
    id         UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    role_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_user_roles PRIMARY KEY (id),
    CONSTRAINT uq_user_roles UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_roles_user     FOREIGN KEY (user_id)    REFERENCES casehub.users    (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role     FOREIGN KEY (role_id)    REFERENCES casehub.roles    (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS ix_user_roles_user  ON casehub.user_roles (user_id);
CREATE INDEX IF NOT EXISTS ix_user_roles_role  ON casehub.user_roles (role_id);

CREATE TABLE IF NOT EXISTS casehub.role_permissions (
    id            UUID        NOT NULL,
    role_id       UUID        NOT NULL,
    permission_id UUID        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_role_permissions PRIMARY KEY (id),
    CONSTRAINT uq_role_permissions UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role       FOREIGN KEY (role_id)       REFERENCES casehub.roles       (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES casehub.permissions (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS ix_role_permissions_role      ON casehub.role_permissions (role_id);
CREATE INDEX IF NOT EXISTS ix_role_permissions_permission ON casehub.role_permissions (permission_id);

-- ---------------------------------------------------------------------------
-- Seed: permissions (idempotent)
-- ---------------------------------------------------------------------------
INSERT INTO casehub.permissions (id, code, description)
SELECT gen_random_uuid(), v.code, v.code
FROM (VALUES
    ('user:read'),
    ('user:create'),
    ('user:update'),
    ('user:disable'),
    ('role:manage'),
    ('standard:read'),
    ('standard:manage'),
    ('category:read'),
    ('category:manage'),
    ('tag:read'),
    ('tag:manage'),
    ('tool:read'),
    ('tool:manage'),
    ('capability:read'),
    ('capability:manage_library'),
    ('project_capability:read'),
    ('project_capability:update'),
    ('capability_request:create'),
    ('capability_request:review'),
    ('project:read'),
    ('project:create'),
    ('project:update'),
    ('project:archive'),
    ('project:complete'),
    ('project_test_case:read'),
    ('project_test_case:add'),
    ('project_test_case:remove'),
    ('project_test_case:restore'),
    ('project_test_case:assign'),
    ('project_test_case:execute'),
    ('test_case:read'),
    ('test_case:draft_create'),
    ('test_case:draft_edit'),
    ('test_case:submit_review'),
    ('test_case:review'),
    ('test_case:publish'),
    ('test_case:deprecate'),
    ('generation_rule:read'),
    ('generation_rule:manage'),
    ('generation:run'),
    ('generation:review_recommendation'),
    ('evidence:read'),
    ('evidence:upload'),
    ('evidence:delete'),
    ('note:read'),
    ('note:create'),
    ('note:update_own'),
    ('note:delete_own'),
    ('change_request:create'),
    ('change_request:review'),
    ('export:project'),
    ('audit:read'),
    ('project_custom_test_case:create'),
    ('project_custom_test_case:edit_own_or_assigned')
) AS v(code)
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Seed: roles (idempotent)
-- ---------------------------------------------------------------------------
INSERT INTO casehub.roles (id, code, name, description)
SELECT gen_random_uuid(), v.code, v.name, v.role_description
FROM (VALUES
    ('ADMIN',            'Administrator',     'Full system access'),
    ('TEST_COORDINATOR', 'Test Coordinator',  'Coordinates test projects and reviews'),
    ('TESTER',           'Tester',            'Executes assigned test cases')
) AS v(code, name, role_description)
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Seed: role_permissions (idempotent)
-- ---------------------------------------------------------------------------

-- ADMIN: every frozen permission
INSERT INTO casehub.role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM casehub.roles r, casehub.permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN (
    'user:read','user:create','user:update','user:disable','role:manage',
    'standard:read','standard:manage','category:read','category:manage','tag:read','tag:manage',
    'tool:read','tool:manage','capability:read','capability:manage_library',
    'project_capability:read','project_capability:update',
    'capability_request:create','capability_request:review',
    'project:read','project:create','project:update','project:archive','project:complete',
    'project_test_case:read','project_test_case:add','project_test_case:remove','project_test_case:restore',
    'project_test_case:assign','project_test_case:execute',
    'test_case:read','test_case:draft_create','test_case:draft_edit','test_case:submit_review',
    'test_case:review','test_case:publish','test_case:deprecate',
    'generation_rule:read','generation_rule:manage','generation:run','generation:review_recommendation',
    'evidence:read','evidence:upload','evidence:delete',
    'note:read','note:create','note:update_own','note:delete_own',
    'change_request:create','change_request:review','export:project','audit:read',
    'project_custom_test_case:create','project_custom_test_case:edit_own_or_assigned'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- TEST_COORDINATOR: coordinator default permission set (Security RBAC Detail §35)
INSERT INTO casehub.role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM casehub.roles r, casehub.permissions p
WHERE r.code = 'TEST_COORDINATOR'
  AND p.code IN (
    'project:read','project:create','project:update','project:archive','project:complete',
    'project_capability:read','project_capability:update',
    'capability_request:review',
    'project_test_case:read','project_test_case:add','project_test_case:remove','project_test_case:restore',
    'project_test_case:assign','project_test_case:execute',
    'test_case:read','test_case:draft_create','test_case:draft_edit','test_case:submit_review',
    'generation_rule:read','generation:run','generation:review_recommendation',
    'evidence:read','evidence:upload','evidence:delete',
    'note:read','note:create','note:update_own','note:delete_own',
    'change_request:create','change_request:review','export:project'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- TESTER: tester default permission set + custom case permissions (§36, §37)
INSERT INTO casehub.role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM casehub.roles r, casehub.permissions p
WHERE r.code = 'TESTER'
  AND p.code IN (
    'project:read','project_capability:read',
    'project_test_case:read','project_test_case:execute',
    'test_case:read',
    'evidence:read','evidence:upload','evidence:delete',
    'note:read','note:create','note:update_own','note:delete_own',
    'capability_request:create','change_request:create',
    'tool:read','standard:read','category:read','tag:read',
    'export:project',
    'project_custom_test_case:create','project_custom_test_case:edit_own_or_assigned'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;
