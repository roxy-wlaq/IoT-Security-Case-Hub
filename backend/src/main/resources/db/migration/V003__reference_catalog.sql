-- V003: Reference catalog (dictionary) tables — Standard/Task Type, Category, Tag, Tool
-- Database Schema V1.0 §8-9. Lead is Migration Version Owner.
--
-- ---------------------------------------------------------------------------
-- DOCUMENTED DEVIATION FROM THE FROZEN DATABASE SCHEMA V1.0
-- ---------------------------------------------------------------------------
-- §9.2 (tags) and §9.3 (tools) do NOT define a `code` column: the frozen schema
-- only gives them id / name / description / enabled (+ platform, website for tools).
--
-- This migration intentionally adds `code VARCHAR(100) NOT NULL` to both tables,
-- together with case-insensitive unique indexes on `code` and `name`.
--
-- Rationale (Spec Owner requirement, Phase 4):
--   * Tag  — the owner explicitly requires "code / name uniqueness": a tag must be
--            addressable by a stable machine code AND by a human name; both must be
--            unique case-insensitively. `code` is the stable identifier used for
--            import / export and for referencing a tag from outside the platform,
--            so `name` stays a display label that may be edited or translated
--            without breaking existing references.
--            Official Tag purposes: Search / Filter / Learning / Stable Reference +
--            Import-Export. A Tag is NOT a generation-rule input — generation
--            conditions live in GenerationRule and evaluate project capabilities,
--            never tags (Implementation Plan Phase 11). The earlier wording
--            "generation-rule matching" was wrong and has been removed.
--   * Tool — the owner explicitly requires the tool dictionary to expose at least
--            `id, code, name, description, enabled`. `code` is the stable key used by
--            test-case tooling references, while `name` stays a display label that may
--            be edited/translated without breaking those references.
--
-- This is a deliberate, approved superset of the frozen schema — NOT an accidental
-- drift. Everywhere else (column order, types, nullability, the two-level category
-- model, the pg_trgm search indexes) this file follows Database Schema V1.0 §8-9
-- exactly. If the document is ever re-frozen, roll the `code` column into it rather
-- than dropping it.
-- ---------------------------------------------------------------------------

-- =============================================================================
-- standard_task_types (§8.1)
-- =============================================================================
CREATE TABLE IF NOT EXISTS casehub.standard_task_types (
    id          UUID        NOT NULL,
    code        VARCHAR(100) NOT NULL,
    name        VARCHAR(200) NOT NULL,
    type        VARCHAR(32) NOT NULL,
    description TEXT,
    enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_standard_task_types PRIMARY KEY (id),
    CONSTRAINT uq_standard_task_types_code UNIQUE (code),
    CONSTRAINT chk_standard_task_types_type CHECK (type IN ('STANDARD', 'TASK_TYPE'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_standard_task_types_code_lower
    ON casehub.standard_task_types (LOWER(code));

-- =============================================================================
-- categories (§9.1) — two-level hierarchy
-- =============================================================================
CREATE TABLE IF NOT EXISTS casehub.categories (
    id          UUID        NOT NULL,
    parent_id   UUID,
    code        VARCHAR(100) NOT NULL,
    name        VARCHAR(150) NOT NULL,
    level       SMALLINT    NOT NULL,
    description TEXT,
    sort_order  INTEGER     NOT NULL DEFAULT 0,
    enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uq_categories_code UNIQUE (code),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES casehub.categories (id) ON DELETE RESTRICT,
    CONSTRAINT chk_categories_level CHECK (level IN (1, 2)),
    CONSTRAINT chk_categories_parent_consistency CHECK (
        (level = 1 AND parent_id IS NULL)
     OR (level = 2 AND parent_id IS NOT NULL)
    )
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_code_lower
    ON casehub.categories (LOWER(code));
CREATE INDEX IF NOT EXISTS ix_categories_parent  ON casehub.categories (parent_id);
CREATE INDEX IF NOT EXISTS ix_categories_enabled ON casehub.categories (enabled);

-- =============================================================================
-- tags (§9.2 + user-mandated `code`)
-- -----------------------------------------------------------------------------
-- Purpose: Search / Filter / Learning / Stable Reference / Import-Export.
-- A tag is a label attached to library content; it is explicitly NOT a
-- generation-rule input (see the deviation note at the top of this file).
-- =============================================================================
CREATE TABLE IF NOT EXISTS casehub.tags (
    id          UUID        NOT NULL,
    code        VARCHAR(100) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tags PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_tags_code_lower ON casehub.tags (LOWER(code));
CREATE UNIQUE INDEX IF NOT EXISTS uq_tags_name_lower ON casehub.tags (LOWER(name));

-- =============================================================================
-- tools (§9.3 + user-mandated `code`)
-- =============================================================================
CREATE TABLE IF NOT EXISTS casehub.tools (
    id          UUID        NOT NULL,
    code        VARCHAR(100) NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    platform    VARCHAR(100),
    website     TEXT,
    enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tools PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_tools_code_lower ON casehub.tools (LOWER(code));
CREATE UNIQUE INDEX IF NOT EXISTS uq_tools_name_lower ON casehub.tools (LOWER(name));

-- =============================================================================
-- pg_trgm fuzzy-search indexes (V001 enables the extension)
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_standard_task_types_name_trgm
    ON casehub.standard_task_types USING GIN (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_categories_name_trgm
    ON casehub.categories USING GIN (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_tags_name_trgm
    ON casehub.tags USING GIN (name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_tools_name_trgm
    ON casehub.tools USING GIN (name gin_trgm_ops);
