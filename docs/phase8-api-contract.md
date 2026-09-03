# Phase 8 — Decision Point / Master Logic Graph API Contract

> 本契约只覆盖 Master Test Case Version 的 Decision Point、Transition、TransitionTarget 与 DAG 模板。
> 不包含 Project Runtime、ProjectDecisionSelection、BranchOutcome、Generation 或 Progressive Runtime。

## 1. Ownership and lifecycle

`DecisionPoint` belongs to one `TestCaseVersion`; it never belongs directly to `MasterTestCase`.
One version may contain multiple decision points, ordered by unique `displayOrder`.
Each decision point has exactly one `Transition` in V1. A `TransitionTarget` points to a
`MasterTestCase`, not a fixed version; runtime version resolution is deferred to a later phase.

Decision-point writes are allowed only on an open `DRAFT` (`revision_closed=false`) and
reuse the Phase 7 Draft resource gate: ADMIN, the Draft owner, or an assigned revision
contributor. Published, Deprecated, closed Review, and open Review versions are immutable
through these endpoints. Reads use the existing version visibility rule.

## 2. Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/test-cases/{masterId}/versions/{versionId}/decision-points` | Ordered decision points |
| `POST` | same + `/decision-points` | Add a decision point; returns `201` |
| `PUT` | same + `/decision-points/{pointId}` | Replace a decision point and transition |
| `DELETE` | same + `/decision-points/{pointId}` | Delete a decision point; returns `204` |
| `GET` | `/api/v1/test-cases/{masterId}/versions/{versionId}/logic-graph` | Master logic graph read model |

All writes require the existing CSRF protection. No endpoint resolves or creates Project
Test Cases.

## 3. Request and response

```json
{
  "name": "Device reachable",
  "description": "Branch when the device responds",
  "displayOrder": 1,
  "transitionType": "NEXT_CASE",
  "targetMasterTestCaseIds": ["uuid"]
}
```

`transitionType` is one of `NEXT_CASE`, `NEXT_CASES`, `PASS`, `FAIL`, `N_A`.
The response includes `id`, `testCaseVersionId`, `displayOrder`, `name`, `description`,
and a `transition` containing ordered target records (`masterTestCaseId`, `caseCode`).

## 4. Target cardinality and DAG validation

| Transition | Required target count |
|---|---:|
| `PASS` / `FAIL` / `N_A` | 0 |
| `NEXT_CASE` | exactly 1 |
| `NEXT_CASES` | 1 or more |

The backend rejects invalid target counts, missing Master targets, duplicate targets, and
self or multi-node cycles. Branching and converging acyclic graphs are valid. The same
`DagValidationService` runs after graph writes and at Submit Review / Publish boundaries.

## 5. Error codes

| Error code | HTTP | Trigger |
|---|---:|---|
| `TEST_CASE_TRANSITION_TARGET_COUNT_INVALID` | 422 | Transition/cardinality mismatch |
| `TEST_CASE_TRANSITION_TARGET_INVALID` | 400 | Missing or duplicate target |
| `TEST_CASE_DAG_CYCLE_DETECTED` | 422 | Self or reachable graph cycle |
| `TEST_CASE_VERSION_IMMUTABLE` | 409 | Write attempted outside open Draft |
| `TEST_CASE_DRAFT_EDIT_FORBIDDEN` | 403 | Draft resource gate denied |

## 6. Graph response

`MasterLogicGraphResponse` contains `testCaseVersionId`, `rootMasterTestCaseId`,
`nodes[]` (`masterTestCaseId`, `caseCode`, `label`) and `edges[]` (`id`, source/target
Master IDs, transition type, decision-point label). The frontend renders this as the
Master Test Case Logic Graph with React Flow; it does not render execution state.
