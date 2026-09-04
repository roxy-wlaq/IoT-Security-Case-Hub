# Batch 4 API Contract — Customization & Change Management

Scope: Phases 21–24, implemented from base `a1b385ae8d20afb8767c838207ac35d23f21bead`. Batch 4 adds Flyway `V017__customization_change_management.sql` only.

## Custom Test Cases

`GET/POST /api/v1/projects/{projectId}/custom-test-cases`, `PUT /{customId}`, `POST /{customId}/assignees/{userId}`, and `POST /{customId}/submit-to-library` manage project-scoped custom cases. A custom case has steps, decision points, transitions, targets, selection/evidence/remark rules, and a project test-plan row with `source=CUSTOM`; it is never a `MasterTestCase` until explicitly submitted to the library. A Tester is automatically assigned on create. Coordinator/Admin may adjust assignees. Library submission creates a `MasterTestCase` plus DRAFT version 1.0 and records the submitter as contributor when needed. Custom-to-custom targets cannot be copied into the global library and are rejected with `CUSTOM_CASE_LIBRARY_TARGET_INVALID`.

Custom and library cases share the execution endpoints under `/api/v1/project-test-cases/{id}/execution`, including selection, evidence, notes, completion, progressive transitions, trigger creation, and project-scoped target rows.

## Capability Update Requests

`GET /api/v1/projects/{projectId}/capability-update-requests`, `POST /{capabilityId}`, and `POST /{requestId}/approve|reject`. Requests snapshot current/proposed values, reason, evidence reference, submitter, reviewer, and status `PENDING|APPROVED|REJECTED`. Tester submits; Coordinator/Admin reviews. Approval updates the project capability, recalculates derived parents, and runs Generation with `triggerType=CAPABILITY_UPDATE`; recommendations are advisory and are not auto-added.

## Test Case Change Requests

`GET/POST /api/v1/test-cases/{masterId}/change-requests` and `POST /{requestId}/approve|reject`. Submission must name a concrete Published `sourceVersionId`. Approval uses the existing revision lifecycle, creating a DRAFT with `basedOnVersionId` and `changeRequestId`; the submitter is recorded as contributor. The contributor may edit the draft but cannot submit it for review. Publish/return/reject/deprecate remain governed by the existing lifecycle.

## Version Upgrade

`GET /api/v1/project-test-cases/{projectTestCaseId}/version`, then Coordinator/Admin-only `POST /keep` or `/upgrade`. Availability reports bound/current Published version IDs and field/logic diff. No automatic upgrade occurs. Keep retains the old binding. Upgrade changes only the existing PTC's version binding, preserving PTC ID, evidence, notes, execution state, assignments, sources, selections, outcomes, and triggers. Logic changes produce a warning; historical decision references remain attached to immutable old versions while future execution uses the new bound version.

All mutating endpoints retain session authentication, CSRF protection, permission-code checks, and project/resource-level authorization.
