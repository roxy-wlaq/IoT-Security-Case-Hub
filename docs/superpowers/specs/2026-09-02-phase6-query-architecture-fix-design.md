# Phase 6 Query Architecture Final Fix — Design

**Date:** 2026-09-02  
**Scope:** Phase 6 test-case list query only  
**Branch:** `dev/v1-implementation`

## Goal

Move test-case list filtering, List Version selection, sorting, pagination, and counting into PostgreSQL while preserving the existing API and guaranteeing that every returned Summary is built from the exact Version selected by the database.

## Constraints

- Do not start Phase 7.
- Do not create V008 or modify database Schema.
- Keep the existing `GET /api/v1/test-cases` API parameters and response shape.
- Keep the frozen search fields: Master `caseCode` and tag name; Version `caseName`, `testPurpose`, step title/content, and tool name.
- Do not introduce Elasticsearch or change frontend API behavior.

## Confirmed List Version Semantics

For each Master, the database produces zero or one List Version. The List Version is the Version used for all list Summary fields and list ordering.

### Candidate predicate

The query starts from one Version row at a time. Every Version-level condition is evaluated against that same Version row:

```text
visible(version, principal)
AND statusMatches(version, requestedStatus)
AND toolMatches(version, requestedToolIds)
AND standardMatches(version, requestedStandardTaskTypeIds)
AND (
    requestedQ is empty
    OR masterMatch(master, requestedQ)
    OR versionMatch(version, requestedQ)
)
```

`masterMatch` is `caseCode ILIKE :q OR EXISTS master tag.name ILIKE :q`. `versionMatch` is `caseName ILIKE :q OR testPurpose ILIKE :q OR EXISTS matching step title/content OR EXISTS matching tool name`.

The `MasterMatch OR VersionMatch` expression is evaluated as written. If `masterMatch` is true, a Version does not need to match `q` itself; it only needs to satisfy the other Version-level conditions. This preserves cases such as `q=BLE&status=DRAFT`, where the Master code matches and only the Draft satisfies status.

### Version selection

After the candidate predicate has been applied, exactly one Version is selected per Master:

1. Current `PUBLISHED` Version first.
2. Otherwise `version_major DESC, version_minor DESC`.
3. A deterministic `id` tie-breaker is used.

The same candidate CTE and selection ordering are used by both the page query and the count query. The count is therefore the number of Masters produced by the same one-Master/one-Version selection semantics.

## Database Query Architecture

Add a `TestCaseLibraryQueryRepository` abstraction with a PostgreSQL implementation using `NamedParameterJdbcTemplate`.

The SQL is organized into reusable query fragments or equivalent shared construction:

```text
candidate_versions
    -> visible Version rows
    -> same-row status/tool/standard conditions
    -> MasterMatch OR same-row VersionMatch

selected_versions
    -> one selected Version per Master
    -> current PUBLISHED priority, then version number DESC, then id

page query
    -> category/tag Master conditions
    -> whitelisted ORDER BY selected Version/Master field
    -> LIMIT :size OFFSET :offset

count query
    -> same candidate_versions and selected_versions semantics
    -> COUNT(*)
```

The page query returns ordered `masterId`, `versionId`, and `totalElements` (or an equivalent page result). It performs all filtering, List Version selection, ordering, and database pagination before returning rows to Java.

`sort` is parsed against the existing whitelist before being converted into a fixed SQL `ORDER BY` fragment. No request value is interpolated without whitelist validation. Version-backed sort fields use the selected Version: `caseName`, `updatedAt`, and `createdAt`; `caseCode` uses the Master.

## Service Hydration Contract

`TestCaseQueryService.list()` delegates the list operation to `TestCaseLibraryQueryRepository` and must never call `masterRepository.findAll()`.

The service batch-loads only the page's Master and Version IDs with `findAllById()`. Since JPA does not guarantee the order of `findAllById()`, it reconstructs a lookup map and emits Summary objects by iterating the Repository's ordered `(masterId, versionId)` rows. The database order is therefore preserved exactly.

`TestCaseSummaryResponse.from(master, version)` remains the single mapping path. It receives the selected Version and uses that Version for `caseName`, `status`, `versionLabel`, and `updatedAt`.

## Testing Strategy

### Unit tests

Use a mocked `TestCaseLibraryQueryRepository` only to verify the service hydration contract and exact ordered reconstruction. The behavior tests assert returned Summary values, not merely invocation counts.

Required service coverage:

- Ordered repository rows remain ordered after unordered `findAllById()` results.
- `list()` never falls back to `masterRepository.findAll()`.

### PostgreSQL integration tests

All query behavior tests use the existing Testcontainers PostgreSQL harness and Flyway V001–V007:

- `masterQueryMatchDoesNotRestrictVersionCandidate`: Master `caseCode` and Published Version match `q`; Draft does not. `q=BLE&status=DRAFT` returns the Draft.
- `versionQueryMatchAndStatusMustReferToSameVersion`: when Master does not match, the same Version must satisfy both `q` and `status`.
- `databasePaginationIsStable`: page 0 and page 1 use database `LIMIT/OFFSET`, contain no duplicate or missing Masters, and report the correct total.
- `multiVersionCaseNameSortSemantics`: current Published names Zulu and Beta sort as Beta/Zulu and Zulu/Beta, with Summary fields aligned to those Versions.
- `toolAndStandardFilterReturnSameListVersion`: Version tool and standard filters return the matching Version's Summary.

## Documentation and Verification

Update `docs/phase6-api-contract.md` only to clarify the exact `q` Master/Version OR behavior while retaining the existing List Version Selection Semantics section.

Update `IMPLEMENTATION_STATUS.md` with the heading `Phase 6 Query Architecture Final Fix`, recording HIGH ×2, the final fix SHA, and test results.

Run:

```text
mvn clean test
mvn verify
npm run typecheck
npm run lint
npm run test
npm run build
```

No frontend source changes are expected.
