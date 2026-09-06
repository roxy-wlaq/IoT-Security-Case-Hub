# IoT-Security-Case-Hub — Final Implementation Status

> 本文件是 V1 最终收口台账。它记录已实现范围、证据状态和未完成的最终门禁；不预先宣布 V1 Release Ready。

## Current Release Gate

| 项目 | 状态 |
| --- | --- |
| Branch | `dev/v1-implementation` |
| Phase 0–29 | FINAL PASS（按已批准候选记录） |
| Phase 30 | FINAL RELEASE REVIEW / READY FOR FINAL QA, STATIC REVIEW, PM GATE |
| Phase 30 Base / Current pushed candidate | `9a7bdd67d323be281d7851bca56010fe468a2151` |
| Recommended version | `V1.0.0`（建议，不代表已创建 tag/release） |
| Highest Flyway migration | `V018__audit_records.sql` |
| V019 | 不存在 |
| Blocking issues declared by DEV | 0（最终 QA/Static Review/PM 尚未完成） |

只有 QA PASS、Static Review PASS、远端 SHA 核对一致且 PM 完成最终门禁后，才可写入 `V1 RELEASE READY`。

## Final Batch Ledger

| 范围 | 内容 | 状态 |
| --- | --- | --- |
| Phase 0–7 | Core foundation / authentication / RBAC / lifecycle | FINAL PASS |
| Batch 1 / Phase 8 | Decision Point / DAG / Logic Graph | FINAL PASS |
| Batch 2 / Phase 9–14 | Project Planning & Generation | FINAL PASS |
| Batch 3 / Phase 15–20 | Execution Stack / Evidence / Progressive / Graph | FINAL PASS |
| Batch 4 / Phase 21–24 | Customization & Change Management | FINAL PASS |
| Batch 5 / Phase 25–26 | Excel Export & Audit Governance | FINAL PASS |
| Batch 6 / Phase 27–28 | Production Security / Deployment / Backup / Restore | FINAL PASS |
| Batch 7 / Phase 29 | Playwright E2E Acceptance | FINAL PASS |
| Phase 30 | V1 Release Review / Finalization | FINAL QA pending |

Phase 29 approved SHA: `9a7bdd67d323be281d7851bca56010fe468a2151`。

## Requirement Coverage

| Capability | Implementation / evidence | Status |
| --- | --- | --- |
| Authentication / Session / CSRF | Spring Security server-side Session, BCrypt, CSRF token contract | IMPLEMENTED + VERIFIED |
| RBAC / resource authorization | Permission gates plus project/resource policy | IMPLEMENTED + VERIFIED |
| Dictionary | Standard, task type, category, tag, tool APIs/UI | IMPLEMENTED + VERIFIED |
| Capability Library | Hierarchy, cycle validation, enable/disable | IMPLEMENTED + VERIFIED |
| Master Test Case | Draft/version/history/visibility | IMPLEMENTED + VERIFIED |
| Lifecycle | Draft → Review → Published → Deprecated, immutable published versions | IMPLEMENTED + VERIFIED |
| Decision Point / DAG | Cardinality, duplicate target, cycle validation, graph UI | IMPLEMENTED + VERIFIED |
| Project | Project membership, visibility, management boundaries | IMPLEMENTED + VERIFIED |
| Project Capability | Effective values and project capability operations | IMPLEMENTED + VERIFIED |
| Generation Rule | Conditions, outputs, recommendations | IMPLEMENTED + VERIFIED |
| Generation Runtime | FULL / PROGRESSIVE_INITIAL generation | IMPLEMENTED + VERIFIED |
| Project Test Plan | Version-bound PTC plan | IMPLEMENTED + VERIFIED |
| My Tests | Assignment, first-view state, execution entry | IMPLEMENTED + VERIFIED |
| Evidence / Storage | Persistent storage, metadata authorization, trash recovery | IMPLEMENTED + VERIFIED |
| Notes | Execution and project test case notes | IMPLEMENTED + VERIFIED |
| Execution | Start, evidence, decision, finish lifecycle | IMPLEMENTED + VERIFIED |
| Progressive Runtime | Trigger and predecessor runtime | IMPLEMENTED + VERIFIED |
| CONNECTED / FLOATING | Relation state and update behavior | IMPLEMENTED + VERIFIED |
| Project Logic Graph | React Flow graph and read-only runtime graph | IMPLEMENTED + VERIFIED |
| Project Custom Test Case | Custom definitions, cardinality, TESTER assignment | IMPLEMENTED + VERIFIED |
| Capability Update Request | Request review and derived capability/generation changes | IMPLEMENTED + VERIFIED |
| Test Case Change Request | Review, revision draft, lifecycle integration | IMPLEMENTED + VERIFIED |
| Version Upgrade | Availability, diff, keep/upgrade binding | IMPLEMENTED + VERIFIED |
| Excel Export | Read-only export with formula-safe cells | IMPLEMENTED + VERIFIED |
| Audit | Append-only records, authorization, sanitization, deterministic paging | IMPLEMENTED + VERIFIED |
| Production Security | HTTPS, headers, CORS, Session cookie, CSRF, rate limit, Actuator | IMPLEMENTED + VERIFIED |
| Deployment | Nginx + backend + PostgreSQL + persistent file storage | IMPLEMENTED + VERIFIED |
| Backup | PostgreSQL custom dump + file archive + manifest/checksums | IMPLEMENTED + VERIFIED |
| Restore | Preflight, matching DB/files, health validation | IMPLEMENTED + VERIFIED |
| Upgrade / Rollback | Backup-first upgrade and image/data rollback distinction | IMPLEMENTED + VERIFIED |
| E2E Acceptance | Real Chromium through HTTPS Nginx and PostgreSQL | IMPLEMENTED + VERIFIED |

## Security Release Review

- Authentication remains server-side HTTP Session; no JWT or localStorage auth token.
- CSRF remains enabled; backend remains final authorization authority.
- Production HTTPS uses `SPRING_PROFILES_ACTIVE=prod`, Secure + HttpOnly + SameSite=Lax `JSESSIONID`.
- `prod,http` is only the explicit local HTTP debugging overlay.
- Production CORS allow-list is empty by default; no wildcard credentialed CORS.
- Nginx provides TLS 1.2/1.3, HSTS, CSP, frame protection, nosniff, Referrer-Policy and Permissions-Policy.
- Login throttling is username/IP aware with five failures and a fifteen-minute block.
- Production Actuator exposure is health-only with details hidden.
- Secrets are runtime supplied; TLS private keys and real `.env` files are not committed.
- PostgreSQL and backend are not publicly exposed; Evidence is not a public static directory.
- Evidence deletion keeps the approved COMMITTED / ROLLED_BACK / UNKNOWN recovery contract.

## Migration and Data Readiness

The ordered chain is `V001` through `V018`; applied migrations are immutable. No `V019` exists or is needed for Phase 30. PostgreSQL 16 is the supported database. Backup/restore treats PostgreSQL metadata and persistent file storage as one recovery set, with manifest identity and SHA-256 preflight checks.

## Evidence Summary

| Area | Status | Evidence |
| --- | --- | --- |
| Backend unit | PASS | Previous approved `mvn clean test`: 241 tests, 0 failures |
| Backend PostgreSQL IT | NOT RERUN — PREVIOUS APPROVED EVIDENCE | Phase 30 delta is documentation/status only |
| Frontend typecheck | PASS | Previous approved `npm run typecheck` |
| Frontend lint | PASS | Previous approved `npm run lint` |
| Frontend Vitest | NOT RERUN — PREVIOUS APPROVED EVIDENCE | Previous run recorded 70 passed / 2 environment timeouts |
| Frontend build | PASS | Previous approved `npm run build` |
| Flyway | PASS | PostgreSQL 16 applied V001–V018; no V019 |
| Production Compose | PASS | Previous approved compose build/config/start evidence |
| HTTPS / security | PASS | Previous approved HTTP 308 redirect, HTTPS health and security-header evidence |
| Backup / Restore | PASS | Previous approved Batch 6 operational evidence |
| Upgrade / Rollback | PASS | Previous approved Batch 6 operational evidence |
| Playwright E2E | PASS | Previous approved Chromium runs: 15/15 twice |
| Static Review | PENDING | Phase 30 delta requires final review |

## Known Issues / Deferred Work

| ID | Severity | Description | Impact / workaround | V1 blocking | Target |
| --- | --- | --- | --- | --- | --- |
| KI-01 | Non-blocking | `TestCaseDetailLifecycle` and `phase6ReviewRound` had 5-second Vitest timeout evidence in the prior run | QA may rerun with environment-specific timeout diagnosis; no production behavior change is implied | NO | Post-V1 test-environment hardening |
| KI-02 | Non-blocking | Bootstrap Admin does not force first-login password change in V1 | Use a strong runtime bootstrap password, rotate operationally, then remove the environment variable | NO | Post-V1 optional forced-reset workflow |

No unresolved Blocker, High, or blocking Medium is being deferred by this ledger. Any new final-QA finding must be classified again as RELEASE BLOCKER, NON-BLOCKING V1 KNOWN ISSUE, or POST-V1 ENHANCEMENT.

## Phase 30 Gate

This document is a DEV preparation artifact. It does not grant final release approval. The remaining workflow is:

```text
DEV finalization
→ QA consolidated Phase 30 gate
→ DEV_PUSH_PROMPT if QA PASS
→ push exact QA SHA
→ Static Review
→ PM final release decision
```
