# Phase 7 Review Fix Round 3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close HIGH-04, HIGH-05, MEDIUM-04, and MEDIUM-05 without changing the Phase 7 schema boundary or starting Phase 8.

**Architecture:** Keep authorization centralized in `TestCaseAccessPolicy`, with separate edit, submit, and rejected-source decisions consumed by controller, service, and `AllowedActions`. Keep List and Detail selection deterministic by aligning the SQL selected-version ranking with the service default-version selector.

**Tech Stack:** Java 21, Spring Boot, Spring Security method authorization, Spring Data JPA, PostgreSQL JDBC queries, JUnit 5, Mockito, AssertJ, MockMvc, Maven, React/TypeScript/Vite.

**Spec:** `docs/superpowers/specs/2026-09-03-phase7-review-fix-round3-design.md`

## Global Constraints

- Flyway highest migration remains `V008`; do not modify V001–V008 or create V009.
- Phase 8 remains `NOT STARTED`; do not add DecisionPoint, Transition, DAG, React Flow, or Phase 8 migration logic.
- Published business content remains immutable, including for ADMIN.
- Reject remains `status=REVIEW`, `revision_closed=true`, and an append-only `REJECT` record.
- Publish/Create Revision/Deprecate continue using Master `PESSIMISTIC_WRITE` locking.
- Version major/minor remain server-controlled; review records remain append-only.
- Do not push to GitHub; the final output is a `QA_TEST_PROMPT`.

### Task 1: Record and verify the design boundary

**Files:**
- Create: `docs/superpowers/specs/2026-09-03-phase7-review-fix-round3-design.md`
- Create: `docs/superpowers/plans/2026-09-03-phase7-review-fix-round3.md`

- [x] **Step 1: Save the approved design and plan.**
- [ ] **Step 2: Confirm the baseline branch and preserve unrelated untracked files.**

Run: `git status --short --branch && git diff -- backend/src/main/resources/db/migration`
Expected: branch `dev/v1-implementation`; the existing untracked files remain; no migration diff.

### Task 2: Split HIGH-04 policy decisions with failing tests first

**Files:**
- Modify: `backend/src/test/java/com/company/casehub/testcase/service/TestCaseAccessPolicyTest.java`
- Modify: `backend/src/test/java/com/company/casehub/testcase/controller/TestCaseLifecycleControllerRbacTest.java`
- Modify: `backend/src/test/java/com/company/casehub/testcase/service/TestCaseLifecycleServiceTest.java`
- Modify: `backend/src/main/java/com/company/casehub/testcase/service/TestCaseAccessPolicy.java`
- Modify: `backend/src/main/java/com/company/casehub/testcase/controller/TestCaseLifecycleController.java`
- Modify: `backend/src/main/java/com/company/casehub/testcase/service/TestCaseLifecycleService.java`

- [ ] **Step 1: Replace the old positive contributor-submit policy test with a failing matrix test.**

Assert that an open Draft contributor returns true from the edit decision, false from the submit decision, and that an owner with `test_case:submit_review` returns true from submit. Assert that a non-member returns false for both.

- [ ] **Step 2: Run the focused policy test and confirm the expected failure.**

Run: `mvn -q -Dtest=TestCaseAccessPolicyTest test`
Expected: FAIL because the current shared `canEditOrSubmit` behavior still makes the contributor submittable.

- [ ] **Step 3: Add the controller regression before changing the controller.**

Change `testerContributorCanSubmitReview` into a denial test. Stub the edit helper true and verify the request still receives 403 and `lifecycleService.submitReview` is never called; this proves the edit helper cannot bypass Submit authorization.

- [ ] **Step 4: Run the controller test and confirm the expected failure.**

Run: `mvn -q -Dtest=TestCaseLifecycleControllerRbacTest test`
Expected: FAIL because the current `@PreAuthorize` expression still contains the edit helper fallback.

- [ ] **Step 5: Add the service regression before changing the service.**

Stub `canSubmitReview` false and the old shared predicate true for a contributor, invoke `submitReview`, and assert `ForbiddenOperationException` plus no save/record. This proves the service cannot accept contributor membership through the edit path.

- [ ] **Step 6: Run the service test and confirm the expected failure.**

Run: `mvn -q -Dtest=TestCaseLifecycleServiceTest test`
Expected: FAIL because the service still calls `canEditOrSubmit`.

- [ ] **Step 7: Implement separate policy methods and helpers.**

Implement `canEditDraft(version, principal)` as `ADMIN || owner || contributor`, and `canSubmitReview(version, principal)` as `has test_case:submit_review && (ADMIN || owner)`. Keep status/open checks at callers. Implement `canSubmitReviewById` as the controller resource helper, independently from `canEditDraftById`, and keep the permission check in the controller expression.

- [ ] **Step 8: Wire controller, service, and AllowedActions to the independent decisions.**

Use `@PreAuthorize("hasAuthority('test_case:submit_review') and @testCaseAccessPolicy.canSubmitReviewById(#masterId, principal)")` for Submit Review, call `canSubmitReview` in the service, and compute `editDraft`/`submitReview` independently. Keep update-Draft on `canEditDraftById` and route it to `canEditDraft`.

- [ ] **Step 9: Run the focused HIGH-04 tests and confirm green.**

Run: `mvn -q -Dtest=TestCaseAccessPolicyTest,TestCaseLifecycleControllerRbacTest,TestCaseLifecycleServiceTest test`
Expected: PASS with the contributor edit/submit matrix enforced.

### Task 3: Lock HIGH-05 source semantics with failing tests first

**Files:**
- Modify: `backend/src/test/java/com/company/casehub/testcase/service/TestCaseLifecycleServiceTest.java`
- Modify: `backend/src/test/java/com/company/casehub/integration/TestCaseLifecycleIT.java`
- Modify: `backend/src/main/java/com/company/casehub/testcase/service/TestCaseLifecycleService.java`
- Modify: `backend/src/main/java/com/company/casehub/testcase/service/TestCaseAccessPolicy.java`

- [ ] **Step 1: Add a failing test for an unrelated user using an omitted source when current PUBLISHED exists.**

Assert that the revision is created from the current public PUBLISHED source, regardless of whether the caller is its owner, because the explicit and omitted public-source forms must be consistent.

- [ ] **Step 2: Add a failing test for authorized and unauthorized rejected sources.**

Assert that owner/contributor/admin can use a closed REVIEW source when permitted, while an unrelated user receives the existing invisible/not-found behavior for explicit source and a source-invalid denial for fallback selection.

- [ ] **Step 3: Run the focused revision tests and confirm the expected failure.**

Run: `mvn -q -Dtest=TestCaseLifecycleServiceTest,TestCaseLifecycleIT test`
Expected: FAIL on the public PUBLISHED omitted-source case because the current resolver applies a private membership predicate to it.

- [ ] **Step 4: Implement `canUseRejectedRevisionSource`.**

Return true only for ADMIN, source owner, or source contributor. Do not use edit or submit methods for this decision.

- [ ] **Step 5: Rewrite `resolveRevisionSource`.**

For explicit source: require visibility, allow PUBLISHED directly, allow REVIEW only when closed and `canUseRejectedRevisionSource` is true, otherwise reject. For omitted source: select current PUBLISHED directly; otherwise find the newest closed REVIEW source that `canUseRejectedRevisionSource` allows, or reject.

- [ ] **Step 6: Run the focused revision tests and confirm green.**

Run: `mvn -q -Dtest=TestCaseLifecycleServiceTest,TestCaseLifecycleIT test`
Expected: PASS for explicit/omitted PUBLISHED consistency and authorized/unauthorized rejected-source cases.

### Task 4: Align MEDIUM-04 List/Detail selection with failing tests first

**Files:**
- Modify: `backend/src/test/java/com/company/casehub/testcase/service/TestCaseQueryServiceTest.java`
- Modify: `backend/src/test/java/com/company/casehub/integration/TestCaseLifecycleIT.java`
- Modify: `backend/src/main/java/com/company/casehub/testcase/service/TestCaseQueryService.java`
- Modify: `backend/src/main/java/com/company/casehub/testcase/repository/PostgresTestCaseLibraryQueryRepository.java`

- [ ] **Step 1: Add a failing List + Detail regression for a deprecated current PUBLISHED version.**

Create or arrange v1.0 PUBLISHED historical and v1.1 DEPRECATED, with no current PUBLISHED. Assert `currentVersion == null`, List selects v1.1, Detail `visibleVersion` selects v1.1, and v1.0 remains in history.

- [ ] **Step 2: Run the focused query tests and confirm the expected failure.**

Run: `mvn -q -Dtest=TestCaseQueryServiceTest,TestCaseLifecycleIT test`
Expected: FAIL because Detail currently prefers any PUBLISHED version.

- [ ] **Step 3: Implement one service-level selector.**

Select current PUBLISHED first, otherwise the newest visible version. Use it for Detail and keep currentVersion independently restricted to `is_current_version && PUBLISHED`. Preserve admin visibility and history ordering.

- [ ] **Step 4: Align the repository CTE ranking.**

Keep current PUBLISHED as the first rank and make the fallback rank the newest eligible version by major/minor; ensure the selected row is the same semantic primary version the service exposes. Do not change visibility predicates or status enum behavior.

- [ ] **Step 5: Run query and integration regressions and confirm green.**

Run: `mvn -q -Dtest=TestCaseQueryServiceTest,TestCaseLifecycleIT test`
Expected: PASS with List and Detail selecting the same default version.

### Task 5: Synchronize MEDIUM-05 contracts and run full verification

**Files:**
- Modify: `docs/phase7-api-contract.md`
- Modify: `IoT-Security-Case-Hub_Security-RBAC-Detail_V1.0.md`
- Modify: relevant migration/documentation tests only if they assert stale wording

- [ ] **Step 1: Update the API contract.**

Document controller permission `test_case:submit_review`, service resource rule ADMIN/owner, contributor edit-only behavior, explicit and omitted rejected-source semantics, public PUBLISHED consistency, and `createRevision` AllowedActions including authorized rejected sources.

- [ ] **Step 2: Update the Security/RBAC detail.**

Replace the contradictory contributor-submit paragraph with `Contributor Tester -> Edit only -> no Submit via contributor membership`. Preserve the existing role permission seed and historical migration text.

- [ ] **Step 3: Run static boundary checks.**

Run: `rg -n "V009|DecisionPoint|TransitionTarget|canEditOrSubmit|submit_review.*canEditDraftById|status.*REJECTED" backend/src/main docs IoT-Security-Case-Hub_Security-RBAC-Detail_V1.0.md`
Expected: no V009/Phase 8 implementation, no shared submit bypass, no REJECTED enum; any historical references must be reviewed and not be production behavior.

- [ ] **Step 4: Run full backend verification.**

Run: `mvn clean test` and `mvn clean verify` from `backend`.
Expected: all unit and integration tests pass with zero failures.

- [ ] **Step 5: Run full frontend verification.**

Run: `npm run typecheck`, `npm run lint`, `npm run test`, and `npm run build` from `frontend`.
Expected: typecheck succeeds, lint has zero errors, tests pass, and build succeeds.

- [ ] **Step 6: Verify final diff and migration boundary.**

Run: `git diff --check`, `git diff --stat`, `git status --short --branch`, and `find backend/src/main/resources/db/migration -maxdepth 1 -type f -name 'V*.sql' -print | sort`.
Expected: only intended source/test/docs changes; no migration file changes; highest migration V008; no push.

- [ ] **Step 7: Generate the QA handoff.**

Final output must be a complete `QA_TEST_PROMPT` containing branch, base SHA `0095acfb2e622323027a0ec8d15dae8815975f1c`, changed files, focused regressions, full commands, Flyway V008, Phase 8 NOT STARTED, and the FAIL/PASS next-step instructions.
