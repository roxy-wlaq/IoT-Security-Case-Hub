# Phase 30 V1 Finalization Implementation Plan

> **For agentic workers:** Execute this plan inline in the current DEV session. This is release documentation and evidence finalization; do not add product scope.

**Goal:** Close the stale production/bootstrap documentation findings and prepare a truthful V1 release-readiness package for final QA, Static Review, and PM gates.

**Architecture:** Keep the existing production topology, security implementation, V018 migration ceiling, and Phase 29 E2E harness unchanged. Update only operator-facing documentation, the implementation ledger, release coverage/evidence artifacts, and release notes unless a narrowly scoped release blocker is discovered.

**Tech Stack:** Markdown, existing Docker Compose/scripts, PostgreSQL/Flyway evidence, Playwright evidence, Git release metadata.

**Spec:** `/Users/roxy/.codex/attachments/3fbe7f5e-5b57-45ec-a1de-2ef79ce05a3e/pasted-text.txt`

## Global Constraints

- Branch remains `dev/v1-implementation`; do not modify `main`.
- Base candidate is `9a7bdd67d323be281d7851bca56010fe468a2151`.
- Production HTTPS uses `SPRING_PROFILES_ACTIVE=prod`; explicit local HTTP uses `prod,http`.
- Bootstrap Admin requires runtime `CASEHUB_BOOTSTRAP_ADMIN_PASSWORD`; V1 does not force first-login password change.
- Highest migration remains V018; do not create V019 or modify applied migrations.
- Do not create tags, GitHub Releases, or merge to `main`.

### Task 1: Close operator documentation findings

Modify `deploy/README.md`, `deploy/.env.example`, and stale status text in `IMPLEMENTATION_STATUS.md` so production HTTPS, local HTTP, and bootstrap-password semantics match the implementation.

### Task 2: Finalize implementation ledger

Update `IMPLEMENTATION_STATUS.md` with Phase 0–29 final-pass history, Phase 30 final-QA readiness (not release approval), the V018 ceiling, and current known-issue classifications.

### Task 3: Build release evidence package

Create a final requirement coverage matrix, security/secret/TODO scan record, and deployment/recovery/E2E evidence summary using only executed or explicitly previous-approved evidence.

### Task 4: Prepare release notes and candidate package

Create `docs/release-notes-v1.0.0.md` and a final RC handoff artifact that recommends V1.0.0 without claiming a tag, GitHub Release, or final approval.

### Task 5: Verify and hand off

Run `git diff --check`, production Compose config validation, migration enumeration, secret/TODO scans, and relevant cheap checks. Commit the finalization delta locally, do not push, and produce one consolidated `QA_TEST_PROMPT`.
