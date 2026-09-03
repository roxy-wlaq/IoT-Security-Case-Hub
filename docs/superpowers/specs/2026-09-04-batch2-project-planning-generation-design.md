# Batch 2 — Project Planning & Generation Vertical Slice Design

**Date:** 2026-09-04  
**Repository:** `https://github.com/roxy-wlaq/IoT-Security-Case-Hub`  
**Branch:** `dev/v1-implementation`  
**Base SHA:** `6252c0478d038fe6bb418b1229b00de56cd70018`

## Goal

Build the Phase 9–14 vertical slice that lets an authorized Coordinator create a
Project, configure standards and capabilities, run rule-based generation, add
recommendations to a version-bound Project Test Plan, assign Testers, and let those
Testers see the Project and their cases in My Tests.

## Scope

This design covers:

- Phase 9 Project Core
- Phase 10 Project Capability
- Phase 11 Generation Rule Administration
- Phase 12 Generation Runtime and recommendations
- Phase 13 Project Test Plan
- Phase 14 My Tests

It does not implement Phase 15+ execution evidence, execution status mutations,
Decision Selection, Branch Outcome, Progressive Runtime, NEXT_CASE/NEXT_CASES
runtime, Trigger processing, Custom Test Cases, Capability Update Requests,
Change Requests, Version Upgrade, exports, audit, deployment, or production
security work.

Phase 8 remains FINAL PASS. Phase 8 DAG data may be displayed or read only where
an existing contract explicitly requires it, but this Batch never chooses an
active DAG template and never executes a Decision Point. MEDIUM-B remains a
deferred design debt to resolve before Progressive Runtime; this Batch does not
choose between a current Published or Draft graph.

## Frozen Domain Semantics

### Project

`Project` stores `projectNumber`, `projectName`, `deviceName`, `generationMode`,
`status`, `createdBy`, timestamps, and relations to `ProjectStandard` and
`ProjectCoordinator`. Project number is generated server-side. Generation mode is
`FULL` or `PROGRESSIVE`; Project status is `DRAFT`, `ACTIVE`, `COMPLETED`, or
`ARCHIVED`.

`ProjectCoordinator` stores `(project_id, user_id, is_primary)`. V1 allows and
displays one primary Coordinator, enforced by a partial unique database index.
Admin has global Project access. A Coordinator can manage only Projects for which
they are a Coordinator. No authorization decision is delegated to the frontend.

### Project Capability

`ProjectCapability` stores one value per `(project_id, capability_id)` and includes
`value`, `source`, `isDerived`, `evidenceReference`, `comment`, `updatedBy`, and
`updatedAt`. Values are `YES`, `NO`, and `UNKNOWN`; `UNKNOWN` is distinct from both
other values. Sources are the frozen values `CUSTOMER_PROVIDED`,
`TESTER_DISCOVERED`, `DOCUMENT`, `AUTOMATIC_DETECTION`, `COORDINATOR_INPUT`,
`DERIVED`, and `OTHER`.

`CapabilityEngine` defines effective values: an absent row is `UNKNOWN`; Child
`YES` derives required ancestors as `YES` with `source=DERIVED` and
`isDerived=true`; Parent `YES` does not alter Child; Parent `NO` makes descendants
not applicable during rule matching but does not rewrite their stored values.

### Generation Rules

`GenerationRule` stores `ruleCode`, `name`, `mode`, `status`, `description`, and
ownership/timestamps. Status is `ENABLED` or `DISABLED`; mode is `FULL`,
`PROGRESSIVE_INITIAL`, or `BOTH`.

Each rule has condition groups with `AND` or `OR` operators and conditions. The
structure is limited to one Root Group plus at most one Child Group level. Targets
are `CAPABILITY` and `STANDARD_TASK_TYPE`. Capability operators are
`EQ_YES`, `EQ_NO`, `EQ_UNKNOWN`, `NE_NO`, `NE_YES`, and `PRESENT`; Standard/Task
Type conditions use `ANY`. Rule outputs relate rules to many Master Test Cases,
and duplicate outputs for one rule are rejected.

`ConditionEvaluator` and `GroupEvaluator` are separate Java services. Repositories
load the structured rule graph; Java evaluates it so AND/OR and UNKNOWN semantics
are unit-testable without encoding the DSL in SQL.

### Generation Runtime

Every run creates a `GenerationRun` with Project, mode, trigger type, executor, and
timestamp. Trigger types are `PROJECT_INITIAL`, `CAPABILITY_UPDATE`,
`STANDARD_CHANGE`, and `MANUAL_REGENERATE`. This Batch uses the applicable current
triggers and does not implement Capability Update Request workflow.

`GenerationEngine` builds a `GenerationContext` from the Project standards,
effective capabilities, Project mode, trigger type, and executor. It evaluates
enabled rules, filters rule modes, deduplicates Master Test Case outputs, preserves
all matched rules as `Recommended Because`, resolves the current valid Published
Version only when adding to a plan, and persists one recommendation per Master per
run. `PROGRESSIVE_INITIAL` recommends only `ENTRY` cases; `NORMAL` cases are not
automatically initial recommendations. This is planning logic, not DAG execution.

Recommendations have status `NEW`, `ADDED`, or `IGNORED`. `ProjectTestCasePreference`
persists Project-specific Ignore state for `(project_id, master_test_case_id)`. A
new run does not create a new `NEW` recommendation for an ignored Master in that
Project; another Project is unaffected. Disabling a rule affects new runs only and
does not remove an existing plan case.

### Project Test Plan

`ProjectTestCase` is the Project instance for a Master Test Case. It stores both
`master_test_case_id` and the exact `test_case_version_id` bound at Add time. The
database enforces one row per `(project_id, master_test_case_id)` even when
`removed=true`; Remove marks the existing row and Restore reuses it.

This Batch supports the actual sources `INITIAL`, `GENERATED`, and `MANUAL`; the
frozen enum also preserves `PROGRESSIVE` and `CUSTOM` without implementing their
runtime flows. `ProjectTestCaseSource` is unique per case/source.

`ProjectTestCaseAssignee` is unique per case/User. An assignee must have the TESTER
role. A Coordinator may execute only if they also have TESTER and are assigned, but
this Batch exposes no execution mutation. Bulk assignment is transactional and
deduplicated.

### My Tests

A Project Member is a Project Coordinator or a User assigned to at least one
Project Test Case. A Tester who is a member can view all Project Test Cases, but an
unassigned case is read-only. My Cases filters to the current User's assignments;
All Project Cases includes all cases visible through Project membership.

`firstViewedAt` is stored on the current User's assignment row. A null value drives
the `NEW` badge. Opening/viewing the relevant My Tests case marks only that
assignment row viewed, so one Tester’s badge state cannot alter another’s.

## Architecture

The backend adds focused `project`, `generation`, and `execution` domain services
while reusing existing `user`, `standard`, `capability`, and `testcase` repositories
and policies. Controllers remain thin; services own transactions, resource
authorization, invariant checks, and DTO mapping. PostgreSQL constraints backstop
all uniqueness and ownership rules, while service-level checks provide stable API
errors.

The dependency flow is:

```text
Project Core
  ↓
Project Capability + CapabilityEngine
  ↓
Generation Rule + Evaluators
  ↓
GenerationRun + Recommendation
  ↓
ProjectTestCase + Assignee
  ↓
My Projects / My Cases
```

Generation Add calls the Test Plan service inside one transaction. It resolves the
current valid Published Version at that boundary and binds that exact version. My
Tests queries use Project membership and assignment relations, not frontend-only
filtering.

## Authorization

The implementation uses both permission-code guards and resource-level service
checks. Admin bypasses resource ownership for Project and Generation Rule admin
operations where the existing authorization model permits it. Coordinator writes
require Project coordinator membership. Capability updates, generation runs,
recommendation Add/Ignore, plan changes, and assignment changes require the
appropriate Project coordinator access. Rule administration is Admin-only for
writes; non-admin users receive read-only access only where the API contract
requires it. Testers can read Projects after becoming members and cannot modify
Project configuration or unassigned cases.

All state-changing endpoints retain existing CSRF protection. The backend rejects
cross-Project IDs, disabled or missing dictionary references, non-Tester assignees,
and inaccessible resources with the repository’s standard error response.

## API Surface

The exact endpoint names are finalized in the implementation contract, with these
resource groups:

- Project list/create/detail/update/status and standards/coordinator management
- Project capability tree read and value update
- Admin Generation Rule list/create/update/enable-disable and rule graph editing
- Project Generation run, run list, recommendation list, Add, Ignore, and Restore
- Project Test Plan list, Add/manual Add, assignment/bulk assignment, Remove, and
  Restore
- My Projects, Project cases, My Cases, and mark-viewed

Request and response DTOs use explicit enums and IDs. Recommendation responses
include the deduplicated Master Test Case plus all matched rule labels. Plan
responses include Master ID, bound Version ID, source types, removed state, and
assignees. My Tests responses expose whether the case is assigned to the current
user and whether it is NEW.

## Persistence and Migration

Migrations begin after V009 and are grouped by coherent domain boundaries. They
create only Phase 9–14 tables, including Project, ProjectStandard,
ProjectCoordinator, ProjectCapability, GenerationRule/Group/Condition/Output,
GenerationRun/Recommendation/RecommendationRule, ProjectTestCasePreference,
ProjectTestCase/Source/Assignee, and the My Tests view state needed for
`firstViewedAt`.

Each table uses the existing UUID/BaseEntity conventions and `casehub` schema.
Foreign keys, CHECK constraints, partial/current indexes, and unique constraints
are explicit. PostgreSQL 16 Testcontainers validate empty-database migration,
foreign keys, Project+Master uniqueness across removed rows, primary Coordinator
uniqueness, Tester assignee constraints where representable, and transaction
rollback behavior.

No Phase 15+ tables are added: specifically no Evidence, Notes, execution status
mutation, ProjectDecisionSelection, BranchOutcome, ProgressiveRuntimeService,
ProjectTestCaseTrigger runtime, Floating runtime, or Custom Test Case tables.

## Frontend

The existing React/TypeScript/Ant Design/React Query conventions are extended with
feature pages and API hooks. Project Overview is the shared entry point for
Capability, Generation, and Test Plan. The UI includes:

- Project list/create/overview and status/coordinator/standard controls
- Capability tree with explicit YES/NO/UNKNOWN display and derived markers
- Admin-only Generation Rule editor with group/condition/output controls
- Generation page with Run, deduplicated recommendations, reasons, Add, Ignore,
  Restore, and regenerate
- Test Plan table with filters, Add, Remove, Restore, assignee selection, and Bulk
  Assign
- My Projects cards, My Cases, All Project Cases, read-only unassigned state, and
  persistent NEW badge behavior

The frontend never treats hidden buttons as authorization. It consumes server
permission/action state and handles backend denial responses.

## Testing Strategy

Every behavior starts with a failing unit or integration test, followed by the
smallest implementation and a green focused test. Backend unit tests cover
evaluators, capability derivation, service invariants, authorization, and DTO
semantics. PostgreSQL Testcontainers cover migrations, constraints, cross-resource
authorization, transaction consistency, version binding, recommendation dedup,
Ignore, Remove/Restore, assignment, and My Tests visibility. Frontend tests cover
the listed pages, API functions/hooks, UNKNOWN behavior, recommendation actions,
plan actions, assignment, and NEW/read-only rendering.

The final verification must execute:

```bash
cd backend
mvn clean test
mvn clean verify

cd ../frontend
npm run typecheck
npm run lint
npm run test
npm run build
```

The full vertical acceptance suite must execute these scenarios: FULL Project,
Progressive Initial planning without NEXT_CASE execution, UNKNOWN capability,
Ignore isolation, Remove/Restore reuse, and Project/Tester access control.

## Known Limitations and Deferred Decisions

- MEDIUM-B remains deferred: no active DAG template selection is introduced.
- Progressive Initial recommendations are supported, but no Progressive Runtime,
  Trigger, assignee inheritance, or NEXT_CASE/NEXT_CASES execution occurs.
- Custom Test Cases are not implemented in this Batch.
- Capability Update Requests are not implemented; direct Coordinator capability
  edits and their generation behavior are separate from that future workflow.
- Execution statuses, evidence, notes, and completion actions remain outside scope.

## Definition of Done

Batch 2 is ready for QA only when all six domain slices, their APIs, migrations,
authorization, frontend flows, synchronized contract documentation, unit tests,
PostgreSQL integration tests, frontend checks, and the complete vertical
acceptance story pass from the approved Phase 8 base. The branch must remain
`dev/v1-implementation`; no changes are made to `main`, and no push occurs during
DEV implementation.
