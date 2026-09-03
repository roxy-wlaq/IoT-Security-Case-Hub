# Batch 2 API Contract

Batch 2 covers Project Planning and Generation (Phases 9–14). All endpoints are
under `/api/v1`, use the existing session/CSRF contract, and enforce backend
resource authorization.

## Project Core

`POST /projects` creates a `DRAFT` Project. The server generates `projectNumber`;
the request contains `projectName`, `deviceName`, `generationMode`, standard/task
type IDs, and a primary coordinator user ID. `GET /projects` lists Projects visible
to the caller. `GET/PUT /projects/{projectId}` reads or updates a Project.
`PUT /projects/{projectId}/status` changes status among `DRAFT`, `ACTIVE`,
`COMPLETED`, and `ARCHIVED`. Admin has global access; Coordinators have access only
to assigned Projects.

## Project Capability

`GET /projects/{projectId}/capabilities` returns the global Capability Tree with
Project values. `PUT /projects/{projectId}/capabilities/{capabilityId}` accepts
`value` (`YES`, `NO`, `UNKNOWN`), `source`, and `comment`. A missing row is
`UNKNOWN`. Child `YES` derives ancestors as `YES`; Parent `YES` does not change a
Child; Parent `NO` changes effective matching only and never rewrites stored
children.

## Generation Rules

Admin writes Generation Rules at `/generation-rules`; authenticated users with
read permission may read them. A rule has `ruleCode`, `name`, `description`,
`mode` (`FULL`, `PROGRESSIVE_INITIAL`, `BOTH`), `status` (`ENABLED`, `DISABLED`),
one Root Group, at most one Child Group level, conditions, and Master Test Case
outputs. Conditions target `CAPABILITY` or `STANDARD_TASK_TYPE`; capability
operators are `EQ_YES`, `EQ_NO`, `EQ_UNKNOWN`, `NE_NO`, `NE_YES`, and `PRESENT`;
Standard/Task Type uses `PRESENT`/`ANY` according to the request DTO contract.

## Generation

`POST /projects/{projectId}/generation-runs` creates a run with `mode`
(`FULL` or `PROGRESSIVE_INITIAL`) and a trigger type. `GET
/projects/{projectId}/generation-runs/{runId}/recommendations` returns one
recommendation per Master Test Case and all matched rule names as
`recommendedBecause`. `POST .../recommendations/{recommendationId}/add` adds or
reuses the Project Test Case and binds the current valid Published Version.
`POST .../recommendations/{masterId}/ignore` persists a Project-local Ignore;
`DELETE .../recommendations/{masterId}/ignore` restores it.

`PROGRESSIVE_INITIAL` recommends `ENTRY` cases only. Generation never executes
Decision Points, NEXT_CASE, NEXT_CASES, triggers, or Progressive Runtime.
Disabled rules affect new runs only.

## Project Test Plan

`GET /projects/{projectId}/test-plan` lists Project Test Cases.
`POST /projects/{projectId}/test-plan` manually adds a Master Test Case.
`POST .../test-plan/{projectTestCaseId}/remove` and `/restore` preserve and reuse
the same row. Each Master-based row stores both `masterTestCaseId` and the exact
`testCaseVersionId`; `(projectId, masterTestCaseId)` is permanently unique.
Sources are `INITIAL`, `GENERATED`, `PROGRESSIVE`, `MANUAL`, and `CUSTOM`; this
Batch uses `INITIAL`, `GENERATED`, and `MANUAL`.

`POST .../test-plan/{projectTestCaseId}/assignees` assigns one TESTER.
`POST .../test-plan/assignees/bulk` assigns multiple cases transactionally.
An assignee must have the `TESTER` role. A Coordinator who also executes in a
future phase must still be a TESTER and an explicit assignee.

## My Tests

`GET /my-projects` lists Projects where the caller is a Coordinator or is assigned
to at least one Project Test Case. `GET /my-cases` lists the caller's assigned
cases. `GET /projects/{projectId}/cases` lists all cases for a visible member.
Unassigned cases are read-only. `POST /project-test-cases/{id}/viewed` sets only
the current User's assignment `firstViewedAt`; null means the response includes
`new=true`.

## Authorization and Errors

Project configuration, capability writes, generation, recommendation actions,
plan mutations, and assignments require Project Coordinator access or Admin.
Rule writes require Admin. Testers may read after becoming Project Members.
Frontend visibility is not an authorization boundary.

Requests return the existing standard error envelope. Relevant stable codes are
`PROJECT_NOT_FOUND`, `PROJECT_ACCESS_FORBIDDEN`, `PROJECT_PRIMARY_COORDINATOR_CONFLICT`,
`PROJECT_STANDARD_INVALID`, `PROJECT_CAPABILITY_INVALID`,
`GENERATION_RULE_INVALID`, `GENERATION_RULE_ACCESS_FORBIDDEN`,
`GENERATION_RECOMMENDATION_NOT_FOUND`, `PROJECT_TEST_CASE_NOT_FOUND`,
`PROJECT_TEST_CASE_DUPLICATE`, `PROJECT_TEST_CASE_VERSION_INVALID`,
`PROJECT_TEST_CASE_ASSIGNEE_INVALID`, and `PROJECT_TEST_CASE_ACCESS_FORBIDDEN`.

## Explicit Exclusions

MEDIUM-B remains deferred. This Batch does not create or execute Project DAG
runtime, Decision Selection, Branch Outcome, Evidence, Notes, execution status
mutations, Custom Test Cases, Capability Update Requests, Change Requests,
Version Upgrade, Floating runtime, or audit/deployment features.
