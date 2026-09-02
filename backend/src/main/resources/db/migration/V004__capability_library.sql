-- V004: Capability Library — capabilities (self-referencing tree)
-- Database Schema V1.0 §10.1. Lead is Migration Version Owner.

-- =============================================================================
-- capabilities (§10.1) — Capability Tree
-- =============================================================================
-- The Capability Library answers "what capabilities does a device have". It is a
-- GLOBAL definition tree, deliberately kept separate from:
--   * casehub.categories  — the test-case Category tree (independent tree, §9.1)
--   * Project Capability  — the YES / NO / UNKNOWN security conclusion per project
--                           (later phase; NOT stored here)
-- A capability row therefore carries no YES / NO / UNKNOWN state. `enabled` only
-- expresses whether the capability definition itself is still selectable.
--
-- Tree rule (§10.1): the Capability Tree must not contain a cycle.
-- The cycle rule is NOT expressible as a table CHECK constraint because it spans
-- an unbounded number of rows, so it is enforced by the Service layer
-- (CapabilityService#assertAcyclic), which walks the parent_id chain upwards and
-- rejects a proposed parent that is the node itself or one of its descendants.
--
-- Retention rule: capabilities are never physically deleted. Historical references
-- (project capabilities, generated test cases) may still point at them, so a
-- capability is retired by setting enabled = FALSE. The self-referencing FK is
-- therefore ON DELETE RESTRICT.
-- =============================================================================
CREATE TABLE IF NOT EXISTS casehub.capabilities (
    id          UUID         NOT NULL,
    parent_id   UUID,
    code        VARCHAR(120) NOT NULL,
    name        VARCHAR(180) NOT NULL,
    description TEXT,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_capabilities PRIMARY KEY (id),
    CONSTRAINT uq_capabilities_code UNIQUE (code),
    CONSTRAINT fk_capabilities_parent FOREIGN KEY (parent_id)
        REFERENCES casehub.capabilities (id) ON DELETE RESTRICT
);

-- Case-insensitive uniqueness: `code` is a natural key typed by humans, so
-- 'BLE' and 'ble' must not coexist. The plain UNIQUE constraint above keeps the
-- exact-match index; this index enforces the case-insensitive rule.
CREATE UNIQUE INDEX IF NOT EXISTS uq_capabilities_code_lower
    ON casehub.capabilities (LOWER(code));

CREATE INDEX IF NOT EXISTS ix_capabilities_parent ON casehub.capabilities (parent_id);
CREATE INDEX IF NOT EXISTS ix_capabilities_enabled ON casehub.capabilities (enabled);
