# Batch 3 — Complete Execution Stack Contract

Batch 3 covers Phases 15–20. Runtime data is stored as project execution
instances; the library DAG is only used when a new target Project Test Case is
created.

## Storage and Evidence

The database stores Evidence metadata only. `LocalStorageService` uses the
`casehub.storage-root` directory with `temp/`, `final/`, and `trash/` areas.
Uploads are written to temp, hashed with SHA-256, moved to final, and then
committed as metadata. Deletes move the object to trash before the metadata
transaction; a failed metadata delete attempts to restore the object, while a
failed purge remains eligible for cleanup.

Project members can list and download Evidence. Only an assigned user can
upload or delete shared Evidence.

## Notes

Project members can read Notes. Only an assigned user can create a Note, and
only its author can update or delete it.

## Execution

`POST /api/v1/project-test-cases/{id}/execution/start` changes
`NOT_STARTED` to `IN_PROGRESS`. Complete requires an assigned user, required
Evidence, and a valid non-empty Decision Point selection. `SINGLE` requires
exactly one selection; `MULTIPLE` requires one or more. Completion persists
the selections and `BranchOutcome` rows, then changes the source to
`COMPLETED`. Reopen changes `COMPLETED` back to `IN_PROGRESS` without deleting
historical Project Test Case rows.

All execution mutation paths lock the current Project Test Case with
`PESSIMISTIC_WRITE` and execute in one transaction.

## Progressive Runtime

`NEXT_CASE` requires exactly one target; `NEXT_CASES` requires at least one.
Each target is reused by `(project_id, master_test_case_id)` when present.
Otherwise its current Published Version is resolved at creation time. An
existing target never changes its bound `testCaseVersionId`.

Runtime-created targets receive the `PROGRESSIVE` source, inherit the union of
source assignees, and receive one unique Trigger per source PTC, Decision
Point, and target PTC. The Trigger records the source PTC's bound version.

## Relation and Graph

Root Initial/Generated/Manual instances are `CONNECTED`. Non-root instances
with at least one incoming runtime Trigger are `CONNECTED`; otherwise they are
`FLOATING`. Reopening and completing a source removes only its old active
runtime effects; target rows remain and are recalculated, so an orphaned target
becomes `FLOATING` instead of being deleted.

`GET /api/v1/projects/{projectId}/logic-graph` returns Project Test Case nodes
and active runtime Trigger edges. It never reconstructs the graph from the
global Master DAG. The React Flow view marks the current, completed, floating,
and root states and labels Trigger edges by Decision Point.

## Security boundary

Test Plan management remains separate from execution. A Tester with global
`project_test_case:add` still cannot add a Project Test Case without Admin or
Project Coordinator resource access. Execution, Note creation, and Evidence
upload require assignment to the specific Project Test Case.

Migration range: V016 only; no Phase 21+ schema is included.
