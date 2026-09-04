# Batch 4 Customization & Change Management Implementation Plan

> **For agentic workers:** Implement this plan task-by-task in the current `dev/v1-implementation` checkout. Use TDD for every production behavior and keep the existing Batch 0–20 contracts intact.

**Goal:** Deliver Phases 21–24 as one vertical slice: project-scoped custom cases, capability and test-case change requests, revision integration, and safe project version keep/upgrade.

**Architecture:** Add project-owned custom-case definitions and expose them through the existing ProjectTestCase execution identity without inserting them into the Master Test Case DAG. Add request aggregates with explicit state transitions and transactional side effects. Reuse the existing TestCaseVersion lifecycle for library revisions and add a compatibility-checked version binding service that preserves the same PTC and its execution data.

**Tech Stack:** Spring Boot 3, Java 21, JPA/Hibernate, PostgreSQL/Flyway, Testcontainers, React, TypeScript, Ant Design, Vitest.

**Spec:** `/Users/roxy/.codex/attachments/50c8d5aa-5659-4921-8e0b-e35381c9f964/pasted-text.txt`

## Global Constraints

- Work only on `dev/v1-implementation`; never modify `main`.
- Start from approved SHA `a1b385ae8d20afb8767c838207ac35d23f21bead`.
- Highest existing migration is V016; add only V017+ for Batch 4.
- Existing PTC runtime version binding, assignee-only execution, CONNECTED/FLOATING, and Phase 7 lifecycle rules remain authoritative.
- Custom cases are project-scoped and must never become Master Test Case DAG nodes.
- Capability approval produces recommendations only; it never auto-adds a ProjectTestCase.
- Version upgrade changes the existing PTC binding only after compatibility checks; no silent auto-upgrade.

### Task 1: Model and migrate project custom cases

**Files:**
- Create: `backend/src/main/resources/db/migration/V017__custom_cases_and_requests.sql`
- Create: `backend/src/main/java/com/company/casehub/customcase/entity/*`
- Create: `backend/src/main/java/com/company/casehub/customcase/repository/*`
- Create: `backend/src/main/java/com/company/casehub/customcase/dto/*`
- Modify: `backend/src/main/java/com/company/casehub/execution/entity/ProjectTestCaseEntity.java`
- Modify: `backend/src/main/java/com/company/casehub/execution/repository/ProjectTestCaseRepository.java`

**Behavior:** Create project-owned custom case, steps, decision points, transitions, targets, and assignees. Enforce exactly one PTC backing mode: Master-based or Custom-based. Custom targets reference project custom cases, while library targets continue referencing Master Test Cases.

**Tests:** Add PostgreSQL migration and repository tests for foreign keys, uniqueness, custom PTC identity, and mixed master/custom backing rejection. Watch the new tests fail before implementation.

### Task 2: Custom case service and runtime integration

**Files:**
- Create: `backend/src/main/java/com/company/casehub/customcase/service/CustomTestCaseService.java`
- Create: `backend/src/main/java/com/company/casehub/customcase/controller/CustomTestCaseController.java`
- Modify: `backend/src/main/java/com/company/casehub/execution/service/ExecutionService.java`
- Modify: `backend/src/main/java/com/company/casehub/execution/service/ProgressiveRuntimeService.java`
- Modify: execution DTOs and response mappers for custom identity.

**Behavior:** Coordinator or Tester may create; Tester is automatically self-assigned and cannot assign others; Coordinator may manage assignees. Custom cases execute through the existing lifecycle and can become progressive targets without entering the library DAG.

**Tests:** Cover create, self-assignment, assignment denial, custom execution, progressive target creation/reuse, and member/assignee authorization.

### Task 3: Capability update requests

**Files:**
- Create: capability request entity/repository/DTO/service/controller files.
- Modify: `CapabilityEngine`, `GenerationRuntimeService`, `ErrorCode`.
- Modify: `docs/batch4-api-contract.md`.

**Behavior:** Tester submits a snapshot request; Coordinator/Admin reviews. Approve updates the ProjectCapability, recalculates derived values, runs generation with `CAPABILITY_UPDATE`, and leaves recommendations unadded. Reject changes only request state.

**Tests:** Submit snapshot, unauthorized review, approve transaction, derived recalculation, generation trigger, recommendation creation, no auto-add, and reject rollback/no capability mutation.

### Task 4: Test case change requests and revision lifecycle

**Files:**
- Create: change-request entity/repository/DTO/service/controller files.
- Modify: `TestCaseLifecycleService`, lifecycle controller/DTOs, and contributor handling.
- Modify: `docs/batch4-api-contract.md`.

**Behavior:** Tester submits against a concrete source version. Coordinator approval creates a DRAFT through the existing version lifecycle with `basedOnVersion` and `changeRequestId`; the Tester becomes contributor, can edit, cannot submit review. Existing owner/coordinator and Admin lifecycle paths remain in use.

**Tests:** Submit/approve/reject, draft linkage, contributor edit-only, submit-review denial, and publish through the existing lifecycle.

### Task 5: Version availability, diff, keep, and safe upgrade

**Files:**
- Create: upgrade/diff DTOs and service/controller files.
- Modify: `ProjectTestPlanService`, PTC repositories/entities/DTOs.
- Modify: `docs/batch4-api-contract.md`.

**Behavior:** Detect a newer current Published version without changing the PTC. Return concise field/logic diff and low-risk vs logic-affecting warning. KEEP leaves the old binding. UPGRADE preserves PTC ID, evidence, notes, assignees, sources, status, selections, outcomes, and valid triggers; incompatible decision references block the operation with no partial update.

**Tests:** Availability, keep, safe upgrade preservation, changed decision-point warning, incompatible selection/outcome/trigger handling, and coordinator/Admin-only authorization.

### Task 6: Frontend vertical slice

**Files:**
- Create: custom case, capability request, change request, and version upgrade API/pages/components/tests under `frontend/src/features/`.
- Modify: `frontend/src/app/router.tsx`, project/test-case navigation, and execution views.

**Behavior:** Implement create/edit custom case, request submit/review, revision status/link, New Version Available badge, diff, Keep, Upgrade, and decision-point warning. Keep UI state aligned with backend authorization and errors.

**Tests:** Vitest coverage for Tester self-assignment, custom submit, capability/change request flows, upgrade badge/diff/keep/upgrade/warning.

### Task 7: Full verification and Batch 4 handoff

**Files:**
- Modify: `IMPLEMENTATION_STATUS.md`.
- Test: backend integration suites and frontend test suite.

- [ ] Run `mvn clean test`.
- [ ] Run `mvn clean verify` against PostgreSQL 16 Testcontainers.
- [ ] Run frontend typecheck, lint, tests, and build.
- [ ] Confirm Flyway reaches the highest Batch 4 migration on an empty database.
- [ ] Confirm only intended files are changed and the two pre-existing untracked documents remain untouched.
- [ ] Generate `QA_TEST_PROMPT` with actual SHA, counts, changed files, limitations, and Suites 1–13.
