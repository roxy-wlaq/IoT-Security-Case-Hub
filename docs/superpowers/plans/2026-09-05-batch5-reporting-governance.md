# Batch 5 Reporting & Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver Phase 25 Project Excel Export and Phase 26 Admin Audit as one QA handoff without changing approved operational semantics.

**Architecture:** A protected `export` service reads one project-scoped operational snapshot and writes fixed metadata-only sheets with Apache POI SXSSF. An append-only `audit` service records required governance events from authoritative business mutation services and exposes only a paginated Admin query API.

**Tech Stack:** Java 21, Spring Boot 3.3, Spring Data JPA, PostgreSQL 16/Testcontainers, Flyway, Apache POI SXSSF, React, TypeScript, TanStack Query, Ant Design, Vitest.

**Spec:** `docs/superpowers/specs/2026-09-05-batch5-reporting-governance-design.md`

## Global Constraints

- Preserve existing Project, Test Case, Execution, Progressive Runtime, Change Management, and Version Upgrade semantics.
- Use `dev/v1-implementation`; do not modify `main`.
- Treat existing uncommitted Audit files and service edits as user-confirmed starting material; never stage unrelated files.
- V017 is immutable; Batch 5 persistence may add only V018 Audit schema.
- Export requires `export:project` plus existing project resource access.
- Audit is append-only and read-only to application users; no update/delete API.
- User-controlled Excel text beginning with `=`, `+`, `-`, or `@` must be emitted as safe text without changing stored data.
- Critical persistence verification uses PostgreSQL 16 Testcontainers, never H2 as the sole proof.
- QA owns formal PASS/FAIL; DEV reports actual development checks and hands off `QA_TEST_PROMPT`.

---

### Task 1: Freeze Batch 5 contracts and dependencies

**Files:**
- Create: `docs/superpowers/specs/2026-09-05-batch5-reporting-governance-design.md`
- Create: `docs/superpowers/plans/2026-09-05-batch5-reporting-governance.md`
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/java/com/company/casehub/export/package-info.java`
- Modify: `docs/batch5-api-contract.md`
- Test: `backend/src/test/java/com/company/casehub/migration/Batch5ContractDocumentationTest.java`

**Interfaces:**
- Produces Apache POI `SXSSFWorkbook` availability and the documented endpoint/sheet/event contract consumed by Tasks 2–8.
- Does not add any Phase 27+ dependency or migration.

- [ ] **Step 1: Write the documentation contract test.** Assert the Batch 5 document names the export endpoint, all three sheet names, fixed column headings, export permission, Admin audit restriction, required event catalog, V018, and no Phase 27 scope.
- [ ] **Step 2: Run the contract test and observe the expected failure** because the Batch 5 document and POI dependency are not yet present in the committed tree.
- [ ] **Step 3: Add the smallest POI dependencies** (`poi-ooxml` at the repository-approved compatible version) and update the export package description; do not add unrelated reporting libraries.
- [ ] **Step 4: Write `docs/batch5-api-contract.md`** from the approved spec, including the exact workbook column order, historical/removed semantics, formula safety, Audit fields, authorization, and transaction guarantee.
- [ ] **Step 5: Run the contract test and the backend compile.** Expected: PASS and Java compilation succeeds under JDK 21.
- [ ] **Step 6: Commit only the contract/dependency files.**

```bash
git add backend/pom.xml backend/src/main/java/com/company/casehub/export/package-info.java docs/batch5-api-contract.md docs/superpowers/specs/2026-09-05-batch5-reporting-governance-design.md docs/superpowers/plans/2026-09-05-batch5-reporting-governance.md backend/src/test/java/com/company/casehub/migration/Batch5ContractDocumentationTest.java
git commit -m "docs(batch5): freeze reporting and audit contracts"
```

### Task 2: Complete Audit persistence and read model

**Files:**
- Modify: `backend/src/main/java/com/company/casehub/audit/entity/AuditAction.java`
- Modify: `backend/src/main/java/com/company/casehub/audit/entity/AuditRecordEntity.java`
- Modify: `backend/src/main/java/com/company/casehub/audit/repository/AuditRecordRepository.java`
- Modify: `backend/src/main/java/com/company/casehub/audit/service/AuditService.java`
- Modify: `backend/src/main/java/com/company/casehub/audit/controller/AuditController.java`
- Modify: `backend/src/main/resources/db/migration/V018__audit_records.sql`
- Test: `backend/src/test/java/com/company/casehub/audit/AuditServiceTest.java`
- Test: `backend/src/test/java/com/company/casehub/integration/Batch5AuditPersistenceIT.java`

**Interfaces:**
- `AuditService.record(AuditAction, UserPrincipal, String, UUID, String, Map<String,Object>)` writes one append-only row in the caller transaction.
- `AuditService.recordLoginSuccess(...)` and `recordLoginFailure(...)` use `REQUIRES_NEW` and never persist password/session/CSRF/credential data.
- `AuditService.query(AuditLogQuery)` returns the existing `PagedResponse<AuditLogResponse>` ordered by `occurredAt DESC`.

- [ ] **Step 1: Add failing unit tests** for sensitive-detail removal, required actor validation, page-size clamping, and stable descending order.
- [ ] **Step 2: Run the unit tests and verify they fail** against the incomplete current implementation or expose any contract mismatch.
- [ ] **Step 3: Add the minimal entity/repository/service corrections** including immutable field handling, action/resource indexes, bounded page size, and detail sanitization.
- [ ] **Step 4: Add the PostgreSQL integration test** that migrates V001–V018, writes records, verifies persistence and filters, confirms no update/delete application path exists, and asserts sensitive keys are absent.
- [ ] **Step 5: Run the targeted unit and IT tests** with JDK 21 and Testcontainers; expected: PASS.
- [ ] **Step 6: Commit the Audit persistence/read-model slice.**

```bash
git add backend/src/main/java/com/company/casehub/audit backend/src/main/resources/db/migration/V018__audit_records.sql backend/src/test/java/com/company/casehub/audit backend/src/test/java/com/company/casehub/integration/Batch5AuditPersistenceIT.java
git commit -m "feat(batch5): add append-only audit persistence"
```

### Task 3: Instrument authoritative Audit event paths

**Files:**
- Modify: `backend/src/main/java/com/company/casehub/auth/service/AuthenticationService.java`
- Modify: `backend/src/main/java/com/company/casehub/auth/service/BootstrapUserService.java`
- Modify: `backend/src/main/java/com/company/casehub/project/service/ProjectService.java`
- Modify: `backend/src/main/java/com/company/casehub/testcase/service/TestCaseLifecycleService.java`
- Modify: `backend/src/main/java/com/company/casehub/generation/service/GenerationRuleService.java`
- Modify: `backend/src/main/java/com/company/casehub/capability/service/CapabilityService.java`
- Modify: `backend/src/main/java/com/company/casehub/evidence/service/EvidenceService.java`
- Test: `backend/src/test/java/com/company/casehub/integration/Batch5AuditEventIT.java`

**Interfaces:**
- Each successful mutation emits one `AuditAction` from its service-level authoritative path.
- Login audit is isolated from authentication failure; all business mutation audit writes participate in the business transaction.

- [ ] **Step 1: Write integration tests** that execute Login, role mutation/bootstrap, Project create/archive, Publish, Deprecated, Generation Rule update, Capability Library update, and Evidence delete, then assert exactly one matching event per successful operation.
- [ ] **Step 2: Run the tests and verify failures** identify missing or duplicate event hooks rather than test setup errors.
- [ ] **Step 3: Wire the current AuditService into the authoritative service methods** after the state mutation succeeds but before the transaction returns; do not add controller-level duplicates.
- [ ] **Step 4: Ensure failed/denied mutations do not create success Audit rows** and successful business mutations roll back if their same-transaction Audit insert fails.
- [ ] **Step 5: Run the event integration suite and the related existing lifecycle/generation/capability/evidence tests.** Expected: PASS.
- [ ] **Step 6: Commit the event instrumentation slice.**

```bash
git add backend/src/main/java/com/company/casehub/auth/service/AuthenticationService.java backend/src/main/java/com/company/casehub/auth/service/BootstrapUserService.java backend/src/main/java/com/company/casehub/project/service/ProjectService.java backend/src/main/java/com/company/casehub/testcase/service/TestCaseLifecycleService.java backend/src/main/java/com/company/casehub/generation/service/GenerationRuleService.java backend/src/main/java/com/company/casehub/capability/service/CapabilityService.java backend/src/main/java/com/company/casehub/evidence/service/EvidenceService.java backend/src/test/java/com/company/casehub/integration/Batch5AuditEventIT.java
git commit -m "feat(batch5): audit governed mutations"
```

### Task 4: Implement the Project export read model and workbook writer

**Files:**
- Create: `backend/src/main/java/com/company/casehub/export/dto/ProjectExportRow.java`
- Create: `backend/src/main/java/com/company/casehub/export/dto/ProjectExportSnapshot.java`
- Create: `backend/src/main/java/com/company/casehub/export/service/ProjectExportService.java`
- Create: `backend/src/main/java/com/company/casehub/export/service/ExcelCellSafety.java`
- Create: `backend/src/main/java/com/company/casehub/export/controller/ProjectExportController.java`
- Modify: `backend/src/main/java/com/company/casehub/project/service/ProjectAccessPolicy.java`
- Test: `backend/src/test/java/com/company/casehub/export/ExcelCellSafetyTest.java`
- Test: `backend/src/test/java/com/company/casehub/integration/Batch5ProjectExportIT.java`

**Interfaces:**
- `ProjectExportService.writeProjectExport(UUID projectId, UserPrincipal principal, OutputStream output)` performs the authorized read and writes the workbook.
- `ExcelCellSafety.text(String value)` returns the safe cell representation without changing the stored value.
- `ProjectExportController` returns an attachment from `/api/v1/projects/{projectId}/export.xlsx` and does not expose storage internals.

- [ ] **Step 1: Write failing tests** for formula-like text escaping and for workbook sheets/headers/data from a mixed master/custom project.
- [ ] **Step 2: Run the targeted tests and verify they fail** because the writer/controller do not yet exist.
- [ ] **Step 3: Implement `ProjectExportSnapshot`** to load one coherent project view using the existing repositories and `ProjectAccessPolicy.requireView`; map separate Master, Version, ProjectTestCase, Custom, assignee, execution, relation, removed, and Evidence metadata fields.
- [ ] **Step 4: Implement the SXSSF writer** with the exact three sheet names and column order from the spec, fixed header rows, safe text cells, no file bytes, and `dispose()` in a finally block.
- [ ] **Step 5: Implement the controller response** with the correct XLSX media type, attachment filename, `export:project` method security, and resource-level access check.
- [ ] **Step 6: Add the large-row test** using thousands of Test Case/Evidence metadata rows and assert the generated ZIP is readable by `XSSFWorkbook` after writing; do not use `XSSFWorkbook` as the production writer.
- [ ] **Step 7: Run the export unit/IT suite and existing project/evidence tests.** Expected: PASS.
- [ ] **Step 8: Commit the export backend slice.**

```bash
git add backend/src/main/java/com/company/casehub/export backend/src/main/java/com/company/casehub/project/service/ProjectAccessPolicy.java backend/src/test/java/com/company/casehub/export backend/src/test/java/com/company/casehub/integration/Batch5ProjectExportIT.java
git commit -m "feat(batch5): add protected project Excel export"
```

### Task 5: Add frontend Project Export action

**Files:**
- Modify: `frontend/src/features/project/api/projectApi.ts`
- Modify: `frontend/src/features/project/pages/ProjectPage.tsx`
- Modify: `frontend/src/shared/types/project.ts`
- Test: `frontend/src/features/project/__tests__/batch5Export.test.tsx`

**Interfaces:**
- `downloadProjectExport(projectId: string): Promise<Blob>` calls `/projects/{projectId}/export.xlsx` with the existing HTTP/CSRF client.
- Project page renders an Export action only for users with `export:project`; backend remains authoritative.

- [ ] **Step 1: Write failing component/API tests** for request URL, blob download handling, and permission-gated button visibility.
- [ ] **Step 2: Run Vitest and verify the new tests fail** because the API/action is absent.
- [ ] **Step 3: Add the API helper** using the existing client response type and browser download lifecycle; revoke the object URL after triggering download.
- [ ] **Step 4: Add the guarded Project page action** without changing existing project, generation, test-plan, or version controls.
- [ ] **Step 5: Run the focused frontend tests, typecheck, and lint.** Expected: PASS with no new warnings.
- [ ] **Step 6: Commit the frontend export slice.**

```bash
git add frontend/src/features/project/api/projectApi.ts frontend/src/features/project/pages/ProjectPage.tsx frontend/src/shared/types/project.ts frontend/src/features/project/__tests__/batch5Export.test.tsx
git commit -m "feat(batch5): add project export action"
```

### Task 6: Implement Admin Audit frontend page

**Files:**
- Create: `frontend/src/features/audit/api/auditApi.ts`
- Create: `frontend/src/features/audit/pages/AuditPage.tsx`
- Create: `frontend/src/features/audit/__tests__/AuditPage.test.tsx`
- Modify: `frontend/src/app/router.tsx`
- Modify: `frontend/src/shared/config/navigation.ts`
- Modify: `frontend/src/shared/types/auth.ts`

**Interfaces:**
- `listAuditLogs(query: AuditQuery): Promise<PagedResponse<AuditLog>>` calls `/audit-logs` with pagination and optional filters.
- `AuditPage` renders timestamp, actor, action, resource type/id, and detail in a read-only Ant Design table.

- [ ] **Step 1: Write failing tests** for Admin permission/role guard, table rendering, page changes, and action/resource/actor filters.
- [ ] **Step 2: Run Vitest and verify expected failures** for the missing page/API.
- [ ] **Step 3: Implement typed audit API and page state** with debounced filter changes, bounded page size, loading/error/empty states, and no mutation controls.
- [ ] **Step 4: Map `/audit-logs` to the page** while retaining the existing Admin-only navigation metadata and RouteGuard behavior.
- [ ] **Step 5: Run focused frontend tests, typecheck, lint, and build.** Expected: PASS.
- [ ] **Step 6: Commit the Admin Audit UI slice.**

```bash
git add frontend/src/features/audit frontend/src/app/router.tsx frontend/src/shared/config/navigation.ts frontend/src/shared/types/auth.ts
git commit -m "feat(batch5): add admin audit page"
```

### Task 7: Synchronize documentation and migration verification

**Files:**
- Modify: `docs/batch5-api-contract.md`
- Modify: `backend/src/main/resources/db/migration/V018__audit_records.sql`
- Test: `backend/src/test/java/com/company/casehub/integration/MigrationIT.java`
- Test: `backend/src/test/java/com/company/casehub/integration/Batch5DocumentationIT.java`

**Interfaces:**
- V018 remains the highest migration and contains only Audit persistence.
- Documentation exactly reflects the implemented endpoint, columns, events, access control, and transaction behavior.

- [ ] **Step 1: Write failing migration/documentation assertions** for V001–V018 application, V017 immutability, V018 table/constraints/indexes, and contract text.
- [ ] **Step 2: Run them against PostgreSQL and verify any mismatch is visible.**
- [ ] **Step 3: Correct only Batch 5 migration/documentation mismatches**; do not create V019 or Phase 27 tables.
- [ ] **Step 4: Run Flyway migration from an empty PostgreSQL database and query V018 constraints/indexes.** Expected: PASS.
- [ ] **Step 5: Commit the documentation/migration verification slice.**

```bash
git add docs/batch5-api-contract.md backend/src/main/resources/db/migration/V018__audit_records.sql backend/src/test/java/com/company/casehub/integration/MigrationIT.java backend/src/test/java/com/company/casehub/integration/Batch5DocumentationIT.java
git commit -m "docs(batch5): verify audit migration and contracts"
```

### Task 8: Full DEV verification and QA handoff

**Files:**
- Create: `QA_TEST_PROMPT.md` only if the repository convention requires a handoff artifact; otherwise provide it in the final response.
- Modify: none in production code.

**Interfaces:**
- Produces actual command results, final local SHA, migration range, changed-file summary, known timeout status, and a QA Suite 1–11 checklist.

- [ ] **Step 1: Run backend unit tests** with `JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn clean test`; record actual unit count and failures/errors.
- [ ] **Step 2: Run PostgreSQL integration verification** with `JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn clean verify`; record IT count, Flyway highest version, and build result.
- [ ] **Step 3: Run frontend `typecheck`, `lint`, `test`, and `build`;** record all actual results and separately identify the known `TestCaseDetailLifecycle` timeout if it remains.
- [ ] **Step 4: Run `git diff --check`, inspect `git status`, and verify no unrelated user files are staged.**
- [ ] **Step 5: Review the complete Batch 5 delta against the approved spec and confirm no Phase 27+ files were added.**
- [ ] **Step 6: Commit only any DEV-owned verification artifact if required.** Do not claim QA PASS.
- [ ] **Step 7: Produce `QA_TEST_PROMPT` containing Repository, Branch, Batch, Base SHA, local SHA, migration range, complete changed files, workbook contract, Audit contract, APIs, frontend changes, actual test results, QA Suites 1–11, acceptance scenarios, regression scope, known timeout, must-verify/must-not-assume rules, and the QA FAIL/PASS next flow.**

Expected handoff flow:

```text
DEV Batch 5 complete
→ QA_TEST_PROMPT
→ QA
→ QA FAIL: DEV_FIX_PROMPT
→ QA PASS: DEV_PUSH_PROMPT
→ DEV Push
→ STATIC_REVIEW_PROMPT
→ Static Review
→ PM
```
