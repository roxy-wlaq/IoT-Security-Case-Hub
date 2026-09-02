# Phase 6 Master Test Case Foundation Implementation Plan

> Status: ✅ Implemented and verified on 2026-09-02. The checklist below records the execution plan; the final verification evidence is recorded in `IMPLEMENTATION_STATUS.md`.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Phase 6 Master Test Case library foundation, including versioned drafts, steps, tags, tools, standard mappings, attachment metadata, search, visibility, and Draft editing.

**Architecture:** Add a focused `testcase` module following the existing modular-monolith DTO/Entity/Repository/Service/Controller pattern. Use one transactional draft aggregate service for create/update and a query service for visibility-aware list/detail/history reads. Add a PostgreSQL Flyway migration with explicit foreign keys and partial uniqueness; add React Query/API/form components without implementing lifecycle, DAG, project, generation, or storage behavior.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring Data JPA, Flyway, PostgreSQL 16, JUnit 5/Mockito/MockMvc, React 18, TypeScript strict, Ant Design, React Hook Form, Zod, TanStack Query, Vitest, React Testing Library.

**Spec:** `IoT-Security-Case-Hub_Phase6-API-Contract_V1.0.md` (with `IoT-Security-Case-Hub_Final-Technical-Review_V1.0.md` taking precedence), plus the frozen Database/Data Model documents.

## Global Constraints

- Start migration numbering at `V006`; never modify `V001`–`V005`.
- Phase 6 creates only `DRAFT` versions; schema supports `DRAFT`, `REVIEW`, `PUBLISHED`, `DEPRECATED`.
- `MasterTestCase` stores stable identity only; version content belongs to `TestCaseVersion`.
- Tags bind to Master; Tools and Standard Mappings bind to Version; Attachments bind to Version and are metadata-only.
- Published, Review, and Deprecated versions are immutable through the Draft endpoint.
- Do not implement Publish, Return, Reject, Deprecated, Review Workflow, Decision Point, DAG, Project, Generation, Execution, Evidence, or StorageService.
- Controllers expose Request/Response DTOs, never JPA entities.
- All write behavior follows TDD: write and observe a failing test before production code.
- Preserve `IoT-Security-Case-Hub_Phase6-API-Contract_V1.0.md` as the user-provided untracked file.

### Task 1: Establish the migration and backend aggregate contract

**Files:**
- Create: `backend/src/main/resources/db/migration/V006__master_test_case_library.sql`
- Create: backend testcase entity, enum, repository, DTO, and exception files under `backend/src/main/java/com/company/casehub/testcase/`
- Test: `backend/src/test/java/com/company/casehub/migration/V006MasterTestCaseSchemaTest.java`

**Interfaces:**
- Produces tables `master_test_cases`, `test_case_versions`, `test_steps`, `test_case_tags`, `test_case_tools`, `test_case_standard_mappings`, and `test_case_attachments` under schema `casehub`.
- Produces enums `TestCaseVersionStatus`, `SelectionMode`, and `ProgressiveRole` with the exact JSON/database values required by the contract.
- Produces DTO records for create/update requests and all response shapes used by later service/controller tasks.

- [ ] **Step 1: Write the failing static migration contract test**

Assert that `V006__master_test_case_library.sql` exists only after the test is written and that the migration text contains the required table names, checks, foreign-key actions, `(master_test_case_id, version_major, version_minor)` uniqueness, current-version partial index, and no lifecycle tables.

- [ ] **Step 2: Run the migration contract test to verify it fails**

Run: `mvn -q -Dtest=V006MasterTestCaseSchemaTest test`

Expected: FAIL because the V006 migration and test class do not yet exist.

- [ ] **Step 3: Add the minimal V006 migration and aggregate model**

Create Java-generated UUID tables with `created_at`/`updated_at`; enforce `case_code` case-insensitive uniqueness, version/status/selection/progressive checks, Draft-only current-version invariant, stable step sequence, relation uniqueness, attachment metadata uniqueness, and the exact FK delete behavior from Database Schema §11. Add DTO records with Bean Validation for required fields and enum values.

- [ ] **Step 4: Run the migration contract test and compile**

Run: `mvn -q -Dtest=V006MasterTestCaseSchemaTest test` and `mvn -q -DskipTests compile`

Expected: PASS and `BUILD SUCCESS`.

- [ ] **Step 5: Commit the schema/model foundation**

Run: `git add backend/src/main/resources/db/migration/V006__master_test_case_library.sql backend/src/main/java/com/company/casehub/testcase backend/src/test/java/com/company/casehub/migration/V006MasterTestCaseSchemaTest.java && git commit -m "feat(phase6): add master test case schema foundation"`

### Task 2: Implement transactional Draft creation and editing

**Files:**
- Create/Modify: `backend/src/main/java/com/company/casehub/testcase/service/TestCaseDraftService.java`
- Create/Modify: testcase repositories and resource-reference adapters for Category, Tag, Tool, and StandardTaskType
- Test: `backend/src/test/java/com/company/casehub/testcase/service/TestCaseDraftServiceTest.java`

**Interfaces:**
- `createDraft(CreateDraftRequest, UserPrincipal)` returns a DTO-backed aggregate and creates Master + Version `1.0/DRAFT` + deduplicated relations in one transaction.
- `updateDraft(UUID masterId, UpdateDraftRequest, UserPrincipal)` fully replaces steps/tags/tools/mappings and assigns step sequence numbers from 1.
- Resource checks reject missing/disabled Category, missing Tag/Tool/Standard, duplicate case code, invalid ownership, and non-DRAFT targets with the Phase 6 error codes.

- [ ] **Step 1: Write failing service tests**

Cover creation with stable Master-only identity, initial `DRAFT 1.0`, deduplication and sequence assignment; reject duplicate case code and disabled category; permit creator/admin Draft update; reject non-owner and non-DRAFT update; replace relations atomically.

- [ ] **Step 2: Run the focused service tests to verify RED**

Run: `mvn -q -Dtest=TestCaseDraftServiceTest test`

Expected: FAIL because the service and behavior are absent.

- [ ] **Step 3: Implement the minimum transactional service**

Use `@Transactional`, repository lookups, `ResourceNotFoundException`/`ConflictException`/`ValidationException`/`ForbiddenOperationException`, and explicit relation replacement. Determine ADMIN only from `UserPrincipal#getRoles()`; do not grant Tester Draft creation/editing.

- [ ] **Step 4: Run focused and full backend unit tests**

Run: `mvn -q -Dtest=TestCaseDraftServiceTest test` then `mvn -q test`

Expected: focused tests and the existing suite pass with zero failures.

- [ ] **Step 5: Commit Draft service behavior**

Run: `git add backend/src/main/java/com/company/casehub/testcase backend/src/test/java/com/company/casehub/testcase/service/TestCaseDraftServiceTest.java && git commit -m "feat(phase6): implement transactional draft editing"`

### Task 3: Implement visibility-aware query and REST API

**Files:**
- Create: `backend/src/main/java/com/company/casehub/testcase/service/TestCaseQueryService.java`
- Create: `backend/src/main/java/com/company/casehub/testcase/controller/TestCaseController.java`
- Modify: `backend/src/main/java/com/company/casehub/common/exception/ErrorCode.java`
- Tests: `backend/src/test/java/com/company/casehub/testcase/controller/TestCaseControllerRbacTest.java`, `backend/src/test/java/com/company/casehub/testcase/service/TestCaseQueryServiceTest.java`

**Interfaces:**
- `GET /api/v1/test-cases` returns 0-based `PagedResponse<TestCaseSummaryResponse>` with allow-listed sort fields and q/category/tag/tool/standard/status filters.
- `POST /api/v1/test-cases` returns `201` and requires `test_case:draft_create`.
- `GET /api/v1/test-cases/{masterId}`, `/versions`, and `/versions/{versionId}` apply the contract visibility rules and return 404 when invisible.
- `PUT /api/v1/test-cases/{masterId}/draft` requires `test_case:draft_edit`, ownership/admin, and Draft status.

- [ ] **Step 1: Write failing service/controller tests**

Cover anonymous denial, Tester denial of create, Coordinator create permission, creator-only Draft edit, ADMIN override, non-Draft conflict, own-Draft visibility, Published visibility, hidden Master 404, pagination shape, q search, OR filter dedupe, and invalid sort error.

- [ ] **Step 2: Run the focused tests to verify RED**

Run: `mvn -q -Dtest=TestCaseQueryServiceTest,TestCaseControllerRbacTest test`

Expected: FAIL because the service/controller/error codes are absent.

- [ ] **Step 3: Implement query mapping and controllers**

Use repository Specifications/queries with fetch-safe mapping, visible-version selection (`current published`, then own Draft, then latest visible version), sorted steps/tools, detail `allowedActions`, uniform error handling, and no entity exposure.

- [ ] **Step 4: Run backend unit and slice tests**

Run: `mvn -q test`

Expected: all existing and Phase 6 `*Test` tests pass.

- [ ] **Step 5: Commit the backend API**

Run: `git add backend/src/main/java/com/company/casehub/common/exception/ErrorCode.java backend/src/main/java/com/company/casehub/testcase backend/src/test/java/com/company/casehub/testcase && git commit -m "feat(phase6): expose test case library api"`

### Task 4: Build the frontend library and Draft editor

**Files:**
- Create: `frontend/src/shared/types/testCase.ts`
- Create: `frontend/src/features/testcase/api/testCaseApi.ts`
- Create: `frontend/src/features/testcase/hooks/useTestCases.ts`
- Create: `frontend/src/features/testcase/schemas/testCaseSchema.ts`
- Create: `frontend/src/features/testcase/pages/TestCaseLibraryPage.tsx`
- Create: `frontend/src/features/testcase/pages/TestCaseDraftPage.tsx`
- Create: `frontend/src/features/testcase/components/TestCaseDraftForm.tsx`
- Modify: `frontend/src/app/router.tsx`, `frontend/src/shared/config/navigation.ts`
- Tests: `frontend/src/features/testcase/__tests__/testCaseApi.test.ts`, `frontend/src/features/testcase/__tests__/TestCaseDraftForm.test.tsx`

**Interfaces:**
- Keep query keys `['testCases']`, `['testCaseDetail', masterId]`, and `['testCaseVersions', masterId]` exactly.
- Model all contract response/request types in `testCase.ts`; API mutations invalidate list and related detail/version keys.
- Render edit/create controls from backend `allowedActions` and current user permissions; no lifecycle buttons.

- [ ] **Step 1: Write failing frontend tests**

Cover API URL/query serialization, mutation invalidation, required case name/selection mode/step content validation, sequence display, and read-only rendering when `allowedActions.editDraft` is false.

- [ ] **Step 2: Run frontend tests to verify RED**

Run: `npm test -- --run src/features/testcase/__tests__/testCaseApi.test.ts src/features/testcase/__tests__/TestCaseDraftForm.test.tsx`

Expected: FAIL because the Phase 6 frontend files are absent.

- [ ] **Step 3: Implement types, API, hooks, schemas, pages, and form**

Use existing `httpClient`, React Query, RHF/Zod, and Ant Design patterns. Support list filtering/pagination, detail display, step add/edit/delete/reorder, tags/tools/standards selection, and create/update Draft only.

- [ ] **Step 4: Run frontend tests and static checks**

Run: `npm test -- --run`, `npm run typecheck`, `npm run lint`, `npm run build`

Expected: all tests pass, typecheck/lint have zero errors/warnings, and Vite build succeeds.

- [ ] **Step 5: Commit the frontend feature**

Run: `git add frontend/src && git commit -m "feat(phase6): add test case library and draft editor"`

### Task 5: Integrate documentation, verify, and publish

**Files:**
- Modify: `IMPLEMENTATION_STATUS.md`
- Preserve: `IoT-Security-Case-Hub_Phase6-API-Contract_V1.0.md`

- [ ] **Step 1: Update status with exact verification evidence**

Record Phase 6 implementation scope, migration V006, tests actually run, and any Testcontainers/runtime limitation without marking unavailable integration checks as passing.

- [ ] **Step 2: Run the complete verification set**

Run: `mvn -q clean test`, `npm run typecheck`, `npm run lint`, `npm test -- --run`, `npm run build`, and `docker compose -f deploy/docker-compose.yml config`.

If Docker/Testcontainers is available, additionally run `mvn -q verify`; otherwise record the exact failure as Pending.

- [ ] **Step 3: Inspect diff and secret safety**

Run: `git status --short`, `git diff --check`, `git diff --stat`, and a tracked-content scan that confirms no token-like credential was added. Do not stage `.env` or credential files.

- [ ] **Step 4: Commit documentation and final integration**

Run: `git add IMPLEMENTATION_STATUS.md docs/superpowers/plans/2026-09-02-phase6-master-test-case.md && git commit -m "docs(phase6): record master test case implementation status"`

- [ ] **Step 5: Push the branch safely**

Verify the remote is `https://github.com/roxy-wlaq/IoT-Security-Case-Hub.git`, then push `dev/v1-implementation` using an ephemeral credential helper sourced from the provided environment secret. Never place the token in a file, command output, commit, remote URL, or response.

- [ ] **Step 6: Verify the remote branch**

Run `git ls-remote origin refs/heads/dev/v1-implementation` and compare it with local `git rev-parse HEAD`; report the resulting commit only.
