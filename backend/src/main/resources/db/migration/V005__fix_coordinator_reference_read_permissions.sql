-- V005: TEST_COORDINATOR reference / capability read permissions
-- Phase 4/5 Code Review — HIGH finding.
--
-- ---------------------------------------------------------------------------
-- Context
-- ---------------------------------------------------------------------------
-- V002 seeded TEST_COORDINATOR (Security & RBAC Detail V1.0 §35) with project,
-- test-case, generation and evidence permissions, but NOT the dictionary /
-- capability read permissions. Every read endpoint under Phase 4 / Phase 5 runs
-- under SecurityConfig's `anyRequest().authenticated()`, so an authenticated
-- coordinator could technically call them — but the permission-driven UI
-- (RouteGuard / PermissionGuard / navigation filtering) hides those pages and the
-- coordinator never receives the `:read` authorities that §35 expects a
-- coordinating role to hold.
--
-- V002 is FROZEN and must not be edited, so this is delivered as an additive
-- follow-up migration.
--
-- ---------------------------------------------------------------------------
-- Guarantees
-- ---------------------------------------------------------------------------
-- 1. Idempotent: INSERT ... SELECT with ON CONFLICT (role_id, permission_id) DO
--    NOTHING — safe to re-run, and harmless if V002's coordinator set is later
--    extended with any of these permissions.
-- 2. Additive only: no DELETE / UPDATE of existing rows. ADMIN and TESTER
--    permission sets are untouched (ADMIN already holds all 54 permissions via
--    V002; TESTER's set is defined by V002 §36/§37 and is deliberately left as-is).
-- 3. Only permissions already seeded by V002 are referenced, so no new permission
--    codes are invented: standard:read, category:read, tag:read, tool:read,
--    capability:read.
-- 4. Re-running Flyway on a database that already has the grants is a no-op.
--
-- Note: this migration makes TEST_COORDINATOR hold 36 role_permission rows
-- (31 from V002 + 5 here). The MigrationIT expectation is updated accordingly.
-- ---------------------------------------------------------------------------

INSERT INTO casehub.role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM casehub.roles r
JOIN casehub.permissions p
  ON p.code IN (
    'standard:read',
    'category:read',
    'tag:read',
    'tool:read',
    'capability:read'
  )
WHERE r.code = 'TEST_COORDINATOR'
ON CONFLICT (role_id, permission_id) DO NOTHING;
