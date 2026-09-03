# Batch 2 — Project Planning & Generation Implementation Plan

> For agentic workers: use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax.

Goal: Deliver Phase 9–14 from Project creation through generation, version-bound Test Plan, Tester assignment, and My Tests visibility.

Architecture: Add focused project, generation, and execution modules that reuse existing user, dictionary, capability-library, and testcase policies. Services own transactions and resource authorization; PostgreSQL constraints enforce invariants; React Query pages consume explicit REST DTOs.

Tech Stack: Java 21, Spring Boot 3.3, Spring Data JPA, PostgreSQL 16 Testcontainers, Flyway, JUnit 5, AssertJ, React, TypeScript, Ant Design, React Query, Vitest.

Spec: docs/superpowers/specs/2026-09-04-batch2-project-planning-generation-design.md

## Global Constraints

- Work only on dev/v1-implementation from 6252c0478d038fe6bb418b1229b00de56cd70018; never modify main or push during DEV.
- Preserve Phase 0–8 lifecycle, Draft authorization, Published immutability, and Phase 8 DAG behavior.
- Do not implement Evidence, Notes, execution mutations, Decision Selection, Branch Outcome, Progressive Runtime, Trigger runtime, Custom Test Cases, Capability Update Requests, Change Requests, Version Upgrade, Excel, Audit, deployment, or Production Security.
- Keep MEDIUM-B deferred. Resolve only the current valid Published Version when adding a recommendation; never choose an active DAG template.
- Every behavior starts with a failing automated test. PostgreSQL 16 Testcontainers cover database constraints and transaction invariants.

## File Map

Backend domain files are created under project/, generation/, and execution/. Each module has entity, repository, dto, service, and controller packages. Shared error codes remain in common/exception/ErrorCode.java. Migrations V010–V014 are grouped by domain. Frontend files are created under features/project, features/generation, features/testplan, and features/my-tests; shared navigation/router/types are modified only for these flows. Contract and status text live in docs/batch2-api-contract.md and IMPLEMENTATION_STATUS.md.

### Task 1: Contract, migrations, and domain vocabulary

Files:
- Create: docs/batch2-api-contract.md
- Create: backend/src/main/resources/db/migration/V010__project_core.sql
- Create: backend/src/main/resources/db/migration/V011__project_capability.sql
- Create: backend/src/main/resources/db/migration/V012__generation_rules.sql
- Create: backend/src/main/resources/db/migration/V013__generation_runtime.sql
- Create: backend/src/main/resources/db/migration/V014__project_test_plan.sql
- Modify: backend/src/main/java/com/company/casehub/common/exception/ErrorCode.java
- Create: enums under backend/src/main/java/com/company/casehub/project/entity, generation/entity, and execution/entity
- Test: backend/src/test/java/com/company/casehub/integration/MigrationIT.java

Interfaces:
- V010 tables: projects, project_standards, project_coordinators.
- V011 tables: project_capabilities.
- V012 tables: generation_rules, generation_condition_groups, generation_conditions, generation_rule_outputs.
- V013 tables: generation_runs, generation_recommendations, generation_recommendation_rules, project_test_case_preferences.
- V014 tables: project_test_cases, project_test_case_sources, project_test_case_assignees.
- Enums: ProjectStatus DRAFT/ACTIVE/COMPLETED/ARCHIVED; GenerationMode FULL/PROGRESSIVE; ProjectCapabilityValue YES/NO/UNKNOWN; ProjectCapabilitySource CUSTOMER_PROVIDED/TESTER_DISCOVERED/DOCUMENT/AUTOMATIC_DETECTION/COORDINATOR_INPUT/DERIVED/OTHER; GenerationRuleStatus ENABLED/DISABLED; GenerationRuleMode FULL/PROGRESSIVE_INITIAL/BOTH; GroupOperator AND/OR; ConditionTargetType CAPABILITY/STANDARD_TASK_TYPE; CapabilityOperator EQ_YES/EQ_NO/EQ_UNKNOWN/NE_NO/NE_YES/PRESENT; StandardTaskTypeOperator ANY; GenerationTriggerType PROJECT_INITIAL/CAPABILITY_UPDATE/STANDARD_CHANGE/MANUAL_REGENERATE; RecommendationStatus NEW/ADDED/IGNORED; ProjectTestCaseSourceType INITIAL/GENERATED/PROGRESSIVE/MANUAL/CUSTOM.

- [ ] Step 1: Write failing MigrationIT assertions for empty-database V010–V014 application, required tables/columns/checks/indexes, Project+Master uniqueness including removed rows, and one primary Coordinator.
- [ ] Step 2: Run cd backend && mvn -q -Dtest=MigrationIT test and confirm failure because the new migrations are absent.
- [ ] Step 3: Implement UUID FKs in casehub, explicit enum CHECK constraints, partial unique primary-Coordinator index, permanent Project+Master unique constraint, and no Phase 15+ tables.
- [ ] Step 4: Run cd backend && mvn -q -Dit.test=MigrationIT verify and confirm PostgreSQL 16 migration success.
- [ ] Step 5: Write docs/batch2-api-contract.md with endpoints, payloads, permissions, version binding, Ignore, NEW, and excluded runtime behavior.
- [ ] Step 6: Run git diff --check, stage only Task 1 files, and commit feat(batch2): add project generation schema contracts.

### Task 2: Project Core backend

Files:
- Create: project/entity/ProjectEntity.java, ProjectStatus.java, GenerationMode.java, ProjectCoordinatorEntity.java, ProjectStandardEntity.java
- Create: project/repository/ProjectRepository.java, ProjectCoordinatorRepository.java, ProjectStandardRepository.java
- Create: project/dto/ProjectCreateRequest.java, ProjectUpdateRequest.java, ProjectResponse.java, ProjectSummaryResponse.java
- Create: project/service/ProjectAccessPolicy.java, ProjectService.java, project/controller/ProjectController.java
- Test: project/service/ProjectServiceTest.java and integration/ProjectIT.java

Interfaces:
- ProjectService.create(request, principal): ProjectResponse
- ProjectService.list(principal): List<ProjectSummaryResponse>
- ProjectService.get(projectId, principal): ProjectResponse
- ProjectService.update(projectId, request, principal): ProjectResponse
- ProjectService.changeStatus(projectId, status, principal): ProjectResponse
- ProjectAccessPolicy.canManage(projectId, principal) and canView(projectId, principal)

- [ ] Step 1: Write failing unit tests for server-generated project number, DRAFT default, standard mapping, primary Coordinator, Admin global access, unrelated Coordinator denial, and disabled standard rejection.
- [ ] Step 2: Run mvn -q -Dtest=ProjectServiceTest test and confirm the expected missing-service failure.
- [ ] Step 3: Implement entities, DTO validation, repositories, transactional service, REST endpoints under /api/v1/projects, permission-code gates, and ProjectAccessPolicy resource checks.
- [ ] Step 4: Write ProjectIT for create/read/update/status, FK and uniqueness behavior, standard mappings, and RBAC; run mvn -q -Dit.test=ProjectIT verify.
- [ ] Step 5: Run mvn -q -DskipITs package and commit feat(batch2): implement project core.

### Task 3: Project Capability and CapabilityEngine

Files:
- Create: project/entity/ProjectCapabilityEntity.java, ProjectCapabilityValue.java, ProjectCapabilitySource.java
- Create: project/repository/ProjectCapabilityRepository.java
- Create: project/dto/ProjectCapabilityRequest.java, ProjectCapabilityResponse.java, EffectiveCapabilityResponse.java
- Create: project/service/CapabilityEngine.java, ProjectCapabilityService.java, ProjectCapabilityController.java
- Test: project/service/CapabilityEngineTest.java, ProjectCapabilityServiceTest.java, integration/ProjectCapabilityIT.java

Interfaces:
- CapabilityEngine.resolveEffectiveValue(projectId, capabilityId): EffectiveCapability
- CapabilityEngine.recalculateDerivedParents(projectId, capabilityId): void
- ProjectCapabilityService.setValue(projectId, capabilityId, request, principal): ProjectCapabilityResponse

- [ ] Step 1: Write failing tests for absent row UNKNOWN, UNKNOWN distinct from YES/NO, Child YES deriving ancestors, Parent YES preserving Child, Parent NO non-applicability without rewriting stored children, and Coordinator-only writes.
- [ ] Step 2: Run focused tests and verify failure before implementation.
- [ ] Step 3: Implement persistence and engine. Derived rows use DERIVED and isDerived=true; explicit values are never overwritten.
- [ ] Step 4: Write ProjectCapabilityIT for (project, capability) uniqueness, FK behavior, derived persistence, and cross-project denial; run it against PostgreSQL 16.
- [ ] Step 5: Run focused tests and commit feat(batch2): add project capability engine.

### Task 4: Generation Rule administration and evaluators

Files:
- Create: entities and enums under generation/entity/
- Create: repositories and DTOs under generation/repository/ and generation/dto/
- Create: generation/service/GenerationRuleService.java, ConditionEvaluator.java, GroupEvaluator.java, GenerationConditionContext.java
- Create: generation/controller/GenerationRuleController.java
- Test: GenerationRuleServiceTest.java, GenerationEvaluatorTest.java, GenerationRuleIT.java

Interfaces:
- GenerationRuleService.create/update/list/get/enable/disable
- ConditionEvaluator.matches(GenerationCondition, GenerationContext): boolean
- GroupEvaluator.matches(GroupNode, GenerationContext): boolean

- [ ] Step 1: Write failing tests for AND/OR, Root plus one Child Group, deeper nesting rejection, all capability operators including PRESENT, Standard/Task Type ANY, enabled/disabled state, rule modes, multiple outputs, and duplicate-output rejection.
- [ ] Step 2: Run focused tests and confirm failure.
- [ ] Step 3: Implement graph persistence, validation, Admin-only mutation API, read policy, and Java evaluators; do not encode the DSL in SQL.
- [ ] Step 4: Write GenerationRuleIT for FK/unique constraints and disabled-rule persistence; run it on PostgreSQL 16.
- [ ] Step 5: Run focused tests and commit feat(batch2): implement generation rule administration.

### Task 5: Generation runtime, recommendations, and Ignore

Files:
- Create: generation/model/GenerationContext.java and GenerationResult.java
- Create: generation/service/GenerationEngine.java, GenerationRunService.java, GenerationRecommendationService.java
- Create: generation/repository runtime repositories, DTOs, and controllers
- Test: GenerationEngineTest.java and integration/GenerationRuntimeIT.java

Interfaces:
- GenerationEngine.run(GenerationContext): GenerationResult
- GenerationRecommendationService.list(projectId, runId, principal)
- GenerationRecommendationService.add(projectId, recommendationId, principal)
- GenerationRecommendationService.ignore(projectId, masterId, principal)
- GenerationRecommendationService.restoreIgnore(projectId, masterId, principal)

- [ ] Step 1: Write failing tests for FULL, PROGRESSIVE_INITIAL ENTRY-only selection, NORMAL exclusion, multi-rule Master deduplication, all Recommended Because rules, disabled-rule exclusion, Project-local Ignore, regeneration, and no DAG-runtime invocation.
- [ ] Step 2: Run focused tests and confirm failure.
- [ ] Step 3: Implement context loading through CapabilityEngine, enabled-rule filtering, evaluator calls, deterministic deduplication, recommendation persistence, and Project-local Ignore preference.
- [ ] Step 4: Implement Add as one transaction calling the Test Plan service; resolve exactly one current valid Published Version and store both Master and Version IDs.
- [ ] Step 5: Write GenerationRuntimeIT for run persistence, causes, Ignore isolation, disabled rules, Add rollback, and version binding.
- [ ] Step 6: Run focused tests and commit feat(batch2): add generation runtime recommendations.

### Task 6: Project Test Plan and Tester assignment

Files:
- Create: execution/entity/ProjectTestCaseEntity.java, ProjectTestCaseSourceEntity.java, ProjectTestCaseAssigneeEntity.java, ProjectTestCasePreferenceEntity.java
- Create: execution/repository/ProjectTestCaseRepository.java, SourceRepository.java, AssigneeRepository.java, PreferenceRepository.java
- Create: execution/dto/ProjectTestCaseResponse.java, AssigneeRequest.java, BulkAssignRequest.java
- Create: execution/service/ProjectTestPlanService.java and execution/controller/ProjectTestPlanController.java
- Test: ProjectTestPlanServiceTest.java and integration/ProjectTestPlanIT.java

Interfaces:
- ProjectTestPlanService.addMasterCase(projectId, masterId, source, principal): ProjectTestCaseResponse
- remove(projectTestCaseId, principal), restore(projectTestCaseId, principal)
- list(projectId, principal), assign(projectTestCaseId, userId, principal), bulkAssign(projectTestCaseIds, userIds, principal)
- resolveCurrentPublishedVersion(masterId): TestCaseVersionEntity

- [ ] Step 1: Write failing tests for Add/version binding, manual Add, duplicate Add, Remove/Restore reuse, source uniqueness, TESTER-only assignees, bulk deduplication, and Coordinator authorization.
- [ ] Step 2: Run focused tests and confirm failure.
- [ ] Step 3: Implement aggregate persistence, locking/re-read idempotency, database uniqueness, role validation, DTOs, transactions, and REST API.
- [ ] Step 4: Write ProjectTestPlanIT for unique/FK constraints, version binding, rollback, role validation, Remove/Restore, and Recommendation Add.
- [ ] Step 5: Run focused tests and commit feat(batch2): implement project test plan.

### Task 7: My Tests backend

Files:
- Modify: execution/entity/ProjectTestCaseAssigneeEntity.java to persist firstViewedAt
- Create: execution/service/MyTestQueryService.java and execution/controller/MyTestController.java
- Create: execution/dto/MyProjectResponse.java, MyCaseResponse.java and repositories
- Test: MyTestQueryServiceTest.java and integration/MyTestsIT.java

Interfaces:
- MyTestQueryService.listMyProjects(principal)
- listMyCases(principal)
- listProjectCases(projectId, principal)
- markViewed(projectTestCaseId, principal)

- [ ] Step 1: Write failing tests for Coordinator/Tester membership, Assigned-to-Me vs All Project Cases, read-only unassigned cases, NEW when firstViewedAt is null, and per-user mark-viewed isolation.
- [ ] Step 2: Run focused tests and confirm failure.
- [ ] Step 3: Implement deterministic queries, membership authorization, read-only flags, and mark-viewed transaction updating only the current User assignment row.
- [ ] Step 4: Write MyTestsIT for unrelated Tester denial and persistent NEW behavior; run against PostgreSQL 16.
- [ ] Step 5: Run focused tests and commit feat(batch2): add my tests queries.

### Task 8: Project and Capability frontend

Files:
- Create: frontend/src/features/project/api/projectApi.ts, hooks/useProjects.ts, pages/ProjectListPage.tsx, pages/ProjectCreatePage.tsx, pages/ProjectOverviewPage.tsx, components/ProjectCapabilityTree.tsx
- Modify: frontend/src/shared/config/navigation.ts and frontend/src/app/router.tsx
- Test: project API/hooks/pages and ProjectCapabilityTree tests

- [ ] Step 1: Write failing Vitest tests for create/list/overview routes, Project form, explicit UNKNOWN/derived display, and backend-denial rendering.
- [ ] Step 2: Run focused Vitest tests and confirm failure.
- [ ] Step 3: Implement typed clients, React Query hooks, forms, pages, and navigation using server action/permission state.
- [ ] Step 4: Run focused tests, npm run typecheck, and npm run lint.
- [ ] Step 5: Commit feat(batch2): add project capability frontend.

### Task 9: Generation and Test Plan frontend

Files:
- Create: frontend/src/features/generation/api/generationApi.ts, hooks, GenerationRuleAdminPage.tsx, ProjectGenerationPage.tsx
- Create: frontend/src/features/testplan/api/testPlanApi.ts, hooks, ProjectTestPlanTable.tsx
- Modify: ProjectOverviewPage.tsx, router.tsx, navigation.ts
- Test: API, rule editor, recommendation, and Test Plan component tests

- [ ] Step 1: Write failing tests for rule groups/operators, Run Generation, deduped causes, Add, Ignore/Restore, Remove/Restore, assignee selection, and bulk assign.
- [ ] Step 2: Run focused Vitest tests and confirm failure.
- [ ] Step 3: Implement API clients/hooks/components and invalidate Project, generation, plan, and My Tests queries after mutation.
- [ ] Step 4: Run focused tests and all frontend checks.
- [ ] Step 5: Commit feat(batch2): add generation and test plan frontend.

### Task 10: My Tests frontend, contract synchronization, and acceptance

Files:
- Create: frontend/src/features/my-tests/api/myTestsApi.ts, hooks, MyProjectsPage.tsx, MyCasesPage.tsx
- Modify: docs/batch2-api-contract.md and IMPLEMENTATION_STATUS.md
- Test: My Projects, My Cases, NEW badge, and full-stack acceptance tests

- [ ] Step 1: Write failing tests for My Projects, Assigned-to-Me/All Project Cases, NEW badge, mark-viewed, and unassigned read-only cases.
- [ ] Step 2: Run focused tests and confirm failure.
- [ ] Step 3: Implement pages/hooks, update the contract and status, and keep Floating as a UI placeholder only.
- [ ] Step 4: Run cd backend && mvn clean test; cd backend && mvn clean verify; cd frontend && npm run typecheck; npm run lint; npm run test; npm run build.
- [ ] Step 5: Execute FULL, Progressive Initial without NEXT_CASE, UNKNOWN, Ignore isolation, Remove/Restore, and access-control acceptance scenarios.
- [ ] Step 6: Run git diff --check, inspect migrations for V010–V014 and no Phase 15+ tables, preserve pre-existing untracked files, and commit feat(batch2): complete project planning generation vertical slice.
- [ ] Step 7: Generate QA_TEST_PROMPT with the complete changed-file list, actual counts, Flyway highest version, Suite 1–10 checklist, acceptance results, MEDIUM-B limitation, and Push Status NOT PUSHED.
