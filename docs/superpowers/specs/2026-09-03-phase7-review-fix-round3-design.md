# Phase 7 Review Fix Round 3 Design

## Goal

Close the static-review findings HIGH-04, HIGH-05, MEDIUM-04, and MEDIUM-05 while preserving the Phase 7 lifecycle invariants and keeping Phase 8 unstarted.

## Design

### Authorization boundaries

`TestCaseAccessPolicy` will expose independent decisions for editing an open Draft and submitting an open Draft. Draft editing remains a resource decision: ADMIN, the Draft owner, or a revision contributor may edit. Submit Review is a separate decision: the principal must have `test_case:submit_review` and be an ADMIN or the Draft owner; contributor membership alone never grants submit authority.

The update-Draft controller keeps its edit-specific resource helper. The Submit Review controller uses only the submit permission-code gate, and `TestCaseLifecycleService.submitReview` independently calls the submit-specific policy. `AllowedActions` uses the same independent edit and submit decisions so a contributor is represented as editable but not submittable.

### Revision source authorization

Revision-source use is a distinct resource decision. An explicit current PUBLISHED source remains usable under the existing public-source contract. An explicit closed REVIEW source is first required to be visible and then must satisfy the dedicated rejected-source resource rule (ADMIN, owner, or contributor). When the source is omitted, the resolver chooses the current PUBLISHED version without adding a private-resource restriction; only if no current PUBLISHED exists does it choose the newest authorized closed REVIEW source. No edit or submit predicate is reused for this decision.

### Default version selection

List and Detail share one semantic ordering: current PUBLISHED first, then the newest eligible version by major/minor. If no current PUBLISHED exists because it was deprecated, the newest visible/eligible version is the primary/default version, while the historical PUBLISHED version remains in history and `currentVersion` stays null. The SQL selector and service detail selector will express the same ordering.

### Documentation and invariants

The Phase 7 API contract and Security/RBAC detail will describe the final authorization and source-selection behavior. V001–V008 remain byte-for-byte unchanged, no V009 is added, Published Immutable remains enforced, Reject remains REVIEW plus `revision_closed=true` plus a REJECT record, review history remains append-only, version numbering remains server-controlled under the Master `PESSIMISTIC_WRITE` lock, and Phase 8 remains unimplemented.

## Verification

Focused policy, controller, service, integration, query, and documentation tests will be added or updated first. After they pass, run `mvn clean test`, `mvn clean verify`, frontend typecheck/lint/test/build, migration-boundary checks, and the Phase 7 regression suite. Do not push from this work.
