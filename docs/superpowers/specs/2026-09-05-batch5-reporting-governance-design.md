# Batch 5 Reporting & Governance Design

## Scope

Batch 5 implements Phase 25 Excel Project Export and Phase 26 Audit. It starts from Batch 4 commit `4248c1cf7967c90a4f5cd27e7bcd21fa40cd1517` on `dev/v1-implementation`. Existing uncommitted Audit work is treated as the starting point and must be preserved, reviewed, and completed rather than discarded.

The batch is an observer/governance addition. It must not redefine Project, Test Case, Execution, Progressive Runtime, Change Management, or Version Upgrade state and must not implement Phase 27+ production-readiness work.

## Architecture

Project export is a protected read operation in a dedicated `export` module. The service reuses the existing project resource policy, materializes the approved project/test-plan/evidence metadata view under one read-only transaction, and writes three fixed sheets with Apache POI `SXSSFWorkbook`. The workbook contains metadata only; it never includes evidence bytes, storage paths, credentials, or secrets.

Audit is an append-only governance module in a dedicated `audit` package. A single `AuditService.record` call is made from each authoritative mutation service. Business mutations and their audit records share the caller transaction; login audit uses an isolated transaction and is failure-isolated from authentication. The query API is read-only and Admin-only.

## API Contract

### Project export

`GET /api/v1/projects/{projectId}/export.xlsx`

The endpoint requires `export:project` and a successful existing project-view check. It returns an Excel-compatible attachment with a stable filename based on the project number. An inaccessible project is denied even if the caller has the export permission.

### Audit query

`GET /api/v1/audit-logs`

The endpoint requires `audit:read` and is additionally restricted to the existing `ADMIN` role. Query parameters are `page`, `size`, `action`, `resourceType`, `resourceId`, `actorUsername`, `from`, and `to`. Results use the existing `PagedResponse` shape and are ordered by `occurredAt DESC`.

There are no application update or delete endpoints for audit records.

## Workbook Contract

The workbook contains exactly these sheets and column orders:

### Project Summary

1. Project Number
2. Project Name
3. Device Name
4. Generation Mode
5. Status
6. Created By
7. Created At
8. Standards

### Test Cases

1. Project Test Case ID
2. Source
3. Master Test Case ID
4. Custom Test Case ID
5. Case Code
6. Case Name
7. Bound Version ID
8. Version
9. Execution Status
10. Relation Status
11. Removed
12. Assignees
13. Evidence Count

`Source` is `MASTER` or `CUSTOM`. Master identity, Test Case Version identity, and Project Test Case identity remain separate. A Custom row has a Custom Test Case ID and no bound master/version identity. Removed rows remain visible with `Removed = true` so the export is an operational snapshot rather than a silent filter.

### Evidence Index

1. Evidence ID
2. Project Test Case ID
3. Original Filename
4. Content Type
5. File Size
6. SHA-256
7. Uploaded By
8. Created At

The sheet excludes `storage_key`, filesystem paths, trash/temp paths, and file bytes.

Any user-controlled text beginning with `=`, `+`, `-`, or `@` is emitted as a safe text cell using the chosen Excel text-escaping contract. The database value is unchanged.

## Audit Contract

The frozen event catalog is:

`LOGIN`, `ROLE_CHANGE`, `PROJECT_CREATE`, `PROJECT_ARCHIVE`, `TEST_CASE_PUBLISH`, `TEST_CASE_DEPRECATE`, `GENERATION_RULE_UPDATE`, `CAPABILITY_LIBRARY_UPDATE`, and `EVIDENCE_DELETE`.

`LOGIN_FAILURE` may remain as an additional security-history event if it is already present in the current uncommitted implementation, but it is not substituted for the required successful `LOGIN` event.

Every record answers who, what, resource, and when using actor id/username, action, resource type/id/label, occurrence timestamp, and safe JSON detail. Sensitive authentication material, session identifiers, CSRF values, credentials, and Evidence contents are excluded both at call sites and by service-level detail-key sanitization.

Audit rows have no normal CRUD lifecycle. Resource references are soft references so history survives resource deletion; no retention or cleanup policy is introduced in this batch.

## Transaction and Authorization Guarantees

For business mutations, the audit insert is part of the same transaction as the successful mutation. A failure rolls back the mutation rather than silently losing a required record. Login audit runs in `REQUIRES_NEW` and is caught/logged so an audit storage failure cannot weaken or reject an otherwise successful authentication.

Project export requires both the existing export permission and project resource access. Audit query is Admin-only. Frontend guards are UX only and cannot widen backend access.

## Persistence Boundary

`V018__audit_records.sql` is the only new persistence migration for Batch 5. It creates the append-only audit table, action check constraint, actor check constraint, and query indexes. V017 is immutable. No Phase 27+ schema is added.

## Verification Contract

Backend tests must inspect workbook contents, not only HTTP status, and must cover authorization, all sheets, the mixed master/custom plan, version/status semantics, evidence metadata, formula safety, and a realistic large-row SXSSF export. PostgreSQL/Testcontainers tests cover V018, audit persistence, filters/pagination, event uniqueness, immutability, and transaction behavior.

Frontend tests cover export request handling, project permission visibility, Audit page rendering, pagination, filtering, and Admin guard. The full Phase 0–24 regression remains required. The known `TestCaseDetailLifecycle` timeout is reported separately if it remains; it is not a blanket exemption for new failures.
