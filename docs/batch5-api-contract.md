# Batch 5 API Contract — Reporting & Governance

## Project Excel Export

```text
GET /api/v1/projects/{projectId}/export.xlsx
```

The request requires `export:project` and the existing project-level view policy. A caller with the global permission but without access to the project receives the same denial as any other protected project read. The export reads all project, plan, and evidence metadata inside one PostgreSQL `REPEATABLE_READ` read-only transaction.

The response is an XLSX attachment. It contains no Evidence bytes, storage keys, filesystem paths, temp/trash paths, credentials, session identifiers, CSRF values, or secrets.

### Workbook sheets and columns

`Project Summary` columns, in order:

```text
Project Number | Project Name | Device Name | Generation Mode | Status |
Created By | Created At | Standards
```

`Test Cases` columns, in order:

```text
Project Test Case ID | Backing Type | Master Test Case ID |
Custom Test Case ID | Case Code | Case Name | Plan Sources |
Bound Version ID | Version | Execution Status | Relation Status |
Removed | Assignees | Evidence Count
```

`Backing Type` is the PTC backing identity (`MASTER` or `CUSTOM`). `Plan Sources` preserves the existing `ProjectTestCaseSourceType` provenance values (`INITIAL`, `GENERATED`, `PROGRESSIVE`, `MANUAL`, `CUSTOM`, as applicable). Master Test Case, Test Case Version, and Project Test Case IDs remain separate. Removed cases remain present with `Removed=true`.

`Evidence Index` columns, in order:

```text
Evidence ID | Project Test Case ID | Original Filename | Content Type |
File Size | SHA-256 | Uploaded By | Created At
```

### Excel text safety

The output contract is implemented by `ExcelCellSafety.text(value)`:

```text
null                         -> ""
normal text                  -> unchanged
=SUM(A1:A2)                 -> '=SUM(A1:A2)
+123                        -> '+123
-123                        -> '-123
@command                    -> '@command
```

Only the workbook cell value is escaped; the stored database value is unchanged. The prefix for formula-significant text is exactly one ASCII apostrophe (`'`).

## Audit Query

```text
GET /api/v1/audit-logs
```

The endpoint requires `audit:read` and the `ADMIN` role. It is read-only and supports `page`, `size`, `action`, `resourceType`, `resourceId`, `actorUsername`, `from`, and `to`. Results use the existing `PagedResponse` shape and deterministic ordering:

```text
occurredAt DESC, id DESC
```

No application update or delete API exists for Audit records.

## Audit records

Required actions are:

```text
LOGIN
ROLE_CHANGE
PROJECT_CREATE
PROJECT_ARCHIVE
TEST_CASE_PUBLISH
TEST_CASE_DEPRECATE
GENERATION_RULE_UPDATE
CAPABILITY_LIBRARY_UPDATE
EVIDENCE_DELETE
```

`LOGIN_FAILURE` is an additional authentication-history action. Business events and successful `LOGIN` require `actorId` and `actorUsername`. `LOGIN_FAILURE` permits a null `actorId` and requires the normalized submitted username; it never fabricates a user identifier. `resourceType`, action, occurrence timestamp, and safe actor/resource context answer who did what to which resource and when.

Audit detail sanitization is recursive through nested Maps, Collections, and arrays. Keys or values containing password, credential, session, CSRF, secret, token, or equivalent authentication material are not persisted. Evidence content is never persisted in Audit.

Business mutation and Audit insertion share one transaction. Login Audit uses an isolated transaction and a failure cannot weaken authentication. V018 is the only Batch 5 migration; V017 remains unchanged and no Phase 27 schema is included.

## Delivery workflow

```text
DEV Batch 5 complete
→ QA_TEST_PROMPT
→ QA
→ QA FAIL: DEV_FIX_PROMPT → DEV → QA again
→ QA PASS: DEV_PUSH_PROMPT → DEV Push → report pushed full SHA
→ user reads GitHub remote delta for Static Review
→ Static Review FAIL: DEV_FIX_PROMPT
→ Static Review PASS: PM_NEXT_STAGE_PROMPT
```
