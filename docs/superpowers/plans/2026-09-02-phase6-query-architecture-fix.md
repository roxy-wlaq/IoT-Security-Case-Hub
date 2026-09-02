# Phase 6 Query Architecture Final Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the in-memory test-case list query with a PostgreSQL-backed one-Master/one-List-Version query whose filters, sorting, pagination, and count share exactly the same semantics.

**Architecture:** Add a PostgreSQL `TestCaseLibraryQueryRepository` backed by `NamedParameterJdbcTemplate`. Its shared CTE SQL first filters visible Version rows using same-row Version predicates and `MasterMatch OR VersionMatch`, then selects one Version per Master, applies Master filters, and executes page/count operations. `TestCaseQueryService` hydrates only the returned page IDs and reconstructs responses in the repository row order.

**Tech Stack:** Java 21, Spring Boot 3.3, Spring Data JPA, PostgreSQL 16, `NamedParameterJdbcTemplate`, Testcontainers, JUnit 5, AssertJ, Mockito.

**Spec:** `docs/superpowers/specs/2026-09-02-phase6-query-architecture-fix-design.md`

## Global Constraints

- Do not start Phase 7.
- Do not create V008 or modify database Schema/migrations.
- Preserve `GET /api/v1/test-cases` parameters and response shape.
- Apply every Version-level condition to the same Version row: visibility, status, tool IDs, standard task type IDs, and VersionMatch of `q`.
- Implement `q` as `MasterMatch(caseCode/tag name) OR VersionMatch(caseName/testPurpose/step title/content/tool name)`; MasterMatch must not require the selected Version to match `q`.
- The page and count queries must share identical `candidate_versions`, `selected_versions`, and Master `categoryId/tagIds` semantics.
- Every supported ORDER BY must end with `selected_versions.master_id ASC`.
- Do not call `masterRepository.findAll()` from `TestCaseQueryService.list()`.
- Rebuild response order from repository `(masterId, versionId)` rows, never from `findAllById()` iteration order.
- Do not change frontend source or frontend API contract.

---

### Task 1: Add red tests for database query delegation and ordered hydration

**Files:**
- Modify: `backend/src/test/java/com/company/casehub/testcase/service/TestCaseQueryServiceTest.java`
- Create: `backend/src/main/java/com/company/casehub/testcase/repository/TestCaseLibraryQueryRepository.java`

**Interfaces:**
- The new repository interface produces `TestCaseLibraryQueryRepository.PageResult`, containing ordered `Row(masterId, versionId)` records and `totalElements`.
- `TestCaseQueryService` will consume `PageResult` in the next task.

- [ ] **Step 1: Write the first failing structural test against the current service.**

Add `listDoesNotLoadAllMastersForQuery` using the current two-argument service setup. Stub `masterRepository.findAll()` to return an empty list so the call completes, invoke `service.list(...)`, and then assert `verify(masterRepository, never()).findAll()`. This must fail because the current implementation still calls `findAll()`.

- [ ] **Step 2: Define the repository result/request types needed by the remaining tests.**

Add an interface with immutable nested records:

```java
public interface TestCaseLibraryQueryRepository {
    PageResult search(Query query);

    record Query(String q, UUID categoryId, List<UUID> tagIds, List<UUID> toolIds,
                 List<UUID> standardTaskTypeIds, TestCaseVersionStatus status,
                 UUID principalId, boolean admin, int page, int size, Sort.Order order) {}

    record Row(UUID masterId, UUID versionId) {}

    record PageResult(List<Row> rows, long totalElements) {}
}
```

Use defensive immutable copies in the implementation task if the repository accepts mutable request lists.

- [ ] **Step 3: Adapt the service unit test fixtures to the repository result contract.**

Mock `TestCaseLibraryQueryRepository`, `MasterTestCaseRepository`, and `TestCaseVersionRepository`. Return deliberately reversed JPA batch-load lists while returning the desired database order from the library repository:

```java
when(libraryRepository.search(any())).thenReturn(new PageResult(
        List.of(new Row(masterB.getId(), versionB.getId()), new Row(masterA.getId(), versionA.getId())), 2));
when(masterRepository.findAllById(any())).thenReturn(List.of(masterA, masterB));
when(versionRepository.findAllById(any())).thenReturn(List.of(versionA, versionB));
```

- [ ] **Step 4: Write the failing ordered-hydration test.**

Add `listPreservesDatabaseOrderAfterBatchHydration`. Assert the response case codes are `CASE-B`, `CASE-A`, even though `findAllById()` returns A then B. Assert both returned summaries use the exact version case names/statuses referenced by their repository rows.

- [ ] **Step 5: Run the focused tests and verify the expected red failure.**

Run:

```bash
mvn -q -Dtest=TestCaseQueryServiceTest test
```

Expected result: the structural and ordered-hydration tests fail because `TestCaseQueryService` still has the old list path; fix only compilation issues in the test/interface setup, not production behavior, before proceeding.

### Task 2: Implement the shared PostgreSQL candidate and selection query

**Files:**
- Create: `backend/src/main/java/com/company/casehub/testcase/repository/PostgresTestCaseLibraryQueryRepository.java`
- Modify: `backend/src/main/java/com/company/casehub/testcase/repository/TestCaseLibraryQueryRepository.java`
- Test: `backend/src/test/java/com/company/casehub/integration/TestCaseDraftIT.java`

**Interfaces:**
- Implements `TestCaseLibraryQueryRepository.search(Query)`.
- Returns ordered page rows and the count without loading all Master rows.

- [ ] **Step 1: Add the common SQL fragment builder.**

Build one candidate/selection CTE string reused by both page and count SQL:

```sql
WITH candidate_versions AS (
    SELECT m.id AS master_id, v.id AS version_id,
           v.version_major, v.version_minor, v.status,
           v.is_current_version, v.case_name, v.created_at, v.updated_at,
           m.case_code
    FROM casehub.master_test_cases m
    JOIN casehub.test_case_versions v ON v.master_test_case_id = m.id
    WHERE visibility_predicate
      AND version_status_predicate
      AND same_version_tool_predicate
      AND same_version_standard_predicate
      AND q_predicate
      AND master_category_predicate
      AND master_tag_filter_predicate
), selected_versions AS (
    SELECT DISTINCT ON (master_id) *
    FROM candidate_versions
    ORDER BY master_id,
             (status = 'PUBLISHED' AND is_current_version) DESC,
             version_major DESC,
             version_minor DESC,
             version_id ASC
)
```

Build optional predicates only from validated values. Bind all IDs as named parameters; never interpolate user-provided values. Keep `categoryId` and `tagIds` in this shared CTE so the count cannot omit them.

- [ ] **Step 2: Implement same-row Version predicates.**

Use predicates shaped as correlated `EXISTS` subqueries tied to `v.id`:

```sql
AND (:status IS NULL OR v.status = :status)
AND (:admin = TRUE OR v.status = 'PUBLISHED'
     OR (v.status = 'DRAFT' AND v.created_by = :principal_id))
AND (NOT tool_filter_enabled OR EXISTS (
     SELECT 1 FROM casehub.test_case_tools ct
     WHERE ct.test_case_version_id = v.id AND ct.tool_id IN (:tool_ids)))
AND (NOT standard_filter_enabled OR EXISTS (
     SELECT 1 FROM casehub.test_case_standard_mappings cm
     WHERE cm.test_case_version_id = v.id
       AND cm.standard_task_type_id IN (:standard_ids)))
```

For `q`, use one OR expression:

```sql
AND (
    :q_pattern IS NULL
    OR m.case_code ILIKE :q_pattern
    OR EXISTS (SELECT 1 FROM casehub.test_case_tags mt
               JOIN casehub.tags t ON t.id = mt.tag_id
               WHERE mt.master_test_case_id = m.id AND t.name ILIKE :q_pattern)
    OR v.case_name ILIKE :q_pattern
    OR v.test_purpose ILIKE :q_pattern
    OR EXISTS (SELECT 1 FROM casehub.test_steps s
               WHERE s.test_case_version_id = v.id
                 AND (s.title ILIKE :q_pattern OR s.content ILIKE :q_pattern))
    OR EXISTS (SELECT 1 FROM casehub.test_case_tools vt
               JOIN casehub.tools tool ON tool.id = vt.tool_id
               WHERE vt.test_case_version_id = v.id AND tool.name ILIKE :q_pattern)
)
```

This makes `q=BLE&status=DRAFT` return the Draft when the Master code matches, even if the Draft name does not.

- [ ] **Step 3: Implement the page SQL and deterministic whitelisted ORDER BY.**

Parse the existing sort whitelist before the repository call. Convert only the four allowed properties to fixed SQL fragments:

```text
caseName  -> selected_versions.case_name
updatedAt -> selected_versions.updated_at
createdAt -> selected_versions.created_at
caseCode  -> selected_versions.case_code
```

Append the required tie-breaker to every direction:

```sql
ORDER BY selected_versions.<allowed_column> <ASC|DESC> NULLS LAST,
         selected_versions.master_id ASC
LIMIT :size OFFSET :offset
```

Select `master_id`, `version_id`; do not select duplicate joined rows. Compute `offset` using a long value and bind `size`/`offset` as numeric parameters.

- [ ] **Step 4: Implement the count SQL using the exact shared CTE.**

Construct count SQL from the same candidate/selection CTE string and finish with:

```sql
SELECT COUNT(*) FROM selected_versions
```

Do not add a separate count predicate path. The shared CTE must include the exact `categoryId` and `tagIds` Master filters used by the page query.

- [ ] **Step 5: Map JDBC rows and execute page/count queries.**

Use `NamedParameterJdbcTemplate.query(...)` for rows and `queryForObject(...)` for count. Map UUID columns with `ResultSet#getObject(column, UUID.class)`. Return an empty `PageResult` when no rows match, retaining the count value.

- [ ] **Step 6: Run the new repository compilation/tests.**

Run:

```bash
mvn -q -Dtest=TestCaseDraftIT test
```

Expected result at this checkpoint: existing service tests may still fail until Task 3 wires the repository; SQL compilation and integration failures should be fixed before changing test expectations.

### Task 3: Switch TestCaseQueryService to database results and preserve response semantics

**Files:**
- Modify: `backend/src/main/java/com/company/casehub/testcase/service/TestCaseQueryService.java`
- Modify: `backend/src/test/java/com/company/casehub/testcase/service/TestCaseQueryServiceTest.java`
- Modify: `backend/src/main/java/com/company/casehub/testcase/dto/TestCaseSummaryResponse.java`

**Interfaces:**
- Service constructor consumes `MasterTestCaseRepository`, `TestCaseVersionRepository`, and `TestCaseLibraryQueryRepository`.
- `list()` sends parsed request data to `search()` and hydrates only page IDs.

- [ ] **Step 1: Replace the in-memory list path with repository delegation.**

Keep existing validation and status/sort parsing. Build a `TestCaseLibraryQueryRepository.Query`, call `search(query)`, and remove `masterRepository.findAll()`, Java filtering, Java sorting, and `subList()` pagination from `list()`.

- [ ] **Step 2: Batch-load only page entities.**

Load IDs from rows with `findAllById()`, index both returned lists by ID, then emit content by iterating the original repository rows:

```java
Map<UUID, MasterTestCaseEntity> masters = masterRepository.findAllById(masterIds).stream()
        .collect(Collectors.toMap(MasterTestCaseEntity::getId, Function.identity()));
Map<UUID, TestCaseVersionEntity> versions = versionRepository.findAllById(versionIds).stream()
        .collect(Collectors.toMap(TestCaseVersionEntity::getId, Function.identity()));
List<TestCaseSummaryResponse> content = result.rows().stream()
        .map(row -> TestCaseSummaryResponse.from(masters.get(row.masterId()), versions.get(row.versionId())))
        .toList();
```

Throw an internal consistency exception if a returned ID cannot be hydrated instead of silently substituting another Version.

- [ ] **Step 3: Build `PageImpl` metadata from database count.**

Use `PageRequest.of(max(page, 0), size)` and `new PageImpl<>(content, pageable, result.totalElements())`. Do not calculate total from content and do not apply Java slicing.

- [ ] **Step 4: Ensure Summary uses Version.updatedAt.**

Keep `caseName`, `status`, `versionLabel`, and `updatedAt` sourced from the selected Version in `TestCaseSummaryResponse.from(master, version)`. The existing response fix must remain intact.

- [ ] **Step 5: Run the focused unit tests to verify green.**

Run:

```bash
mvn -q -Dtest=TestCaseQueryServiceTest test
```

Expected result: all service tests pass, including ordered hydration and no-full-load structural tests.

### Task 4: Add and pass the complete PostgreSQL query contract suite

**Files:**
- Modify: `backend/src/test/java/com/company/casehub/integration/TestCaseDraftIT.java`

**Interfaces:**
- Tests call the public `TestCaseQueryService.list()` API against Testcontainers PostgreSQL.

- [ ] **Step 1: Add `masterQueryMatchDoesNotRestrictVersionCandidate`.**

Create one Master with `caseCode=BLE-001`, a Published Version named `BLE Pairing`, and a newer/current Draft named `New Draft`. Query `q=BLE&status=DRAFT`. Assert exactly one row with Draft `status`, `versionLabel`, and `caseName`.

- [ ] **Step 2: Add `versionQueryMatchAndStatusMustReferToSameVersion`.**

Create a Master whose code/tags do not match the query, then create two visible Versions: one matching `q` but with the wrong status and another with the requested status but without the query term. Assert the result is empty. Add a matching Version and assert its `caseName`, status, and version label are returned together.

- [ ] **Step 3: Add `databasePaginationIsStable`.**

Create at least five Masters in the same category with distinct codes and identical case names, query with `size=2` and a supported sort, and assert page 0/page 1 have no overlap, their union equals the expected Masters, and both pages report the same correct `totalElements`. Repeat with a tied `caseName` ordering to exercise the Master ID tie-breaker.

- [ ] **Step 4: Preserve and tighten multi-version and relation-filter tests.**

Keep `multiVersionCaseNameSortSemantics` asserting Beta/Zulu and Zulu/Beta plus aligned `caseName`, `versionLabel`, and `status`. Add or rename the relation test to `toolAndStandardFilterReturnSameListVersion`, and assert the returned Summary belongs to the Version carrying the matching tool/standard mapping.

- [ ] **Step 5: Run the PostgreSQL suite and inspect SQL-backed behavior.**

Run:

```bash
mvn -q -Dtest=TestCaseDraftIT test
```

Expected result: all query contract tests pass against PostgreSQL 16; logs show Flyway stops at V007 and no migration is added.

### Task 5: Clarify contract/status and perform full verification

**Files:**
- Modify: `docs/phase6-api-contract.md`
- Modify: `IMPLEMENTATION_STATUS.md`

- [ ] **Step 1: Clarify q OR semantics in the API contract.**

In `List Version Selection Semantics`, explicitly state that `q` is `MasterMatch OR VersionMatch`, and that a Master-level match does not require the selected Version to match `q`; status/tool/standard conditions still apply to the selected Version row.

- [ ] **Step 2: Record final Phase 6 status.**

Add `Phase 6 Query Architecture Final Fix` with HIGH ×2, the two findings (`q Master/Version OR semantics` and `DB-side filter/sort/pagination`), fix SHA, test totals, PostgreSQL IT evidence, Count Contract, Stable Pagination Contract, and confirmation that V008 was not created.

- [ ] **Step 3: Run all requested verification commands.**

Run from `backend/`:

```bash
mvn clean test
mvn verify
```

Run from `frontend/`:

```bash
npm run typecheck
npm run lint
npm run test
npm run build
```

Confirm `git diff --check`, no migration files beyond V007, and no frontend source changes.

- [ ] **Step 4: Commit implementation and documentation separately.**

Use focused commits:

```bash
git add backend/src/main/java backend/src/test/java
git commit -m "fix(phase6): move case library query to PostgreSQL"
git add docs/phase6-api-contract.md IMPLEMENTATION_STATUS.md
git commit -m "docs(phase6): record query architecture final fix"
```

- [ ] **Step 5: Push only the requested branch.**

```bash
git push origin dev/v1-implementation
```

Verify the remote branch points to the final local SHA and stop for review. Do not implement Phase 7 capabilities.
