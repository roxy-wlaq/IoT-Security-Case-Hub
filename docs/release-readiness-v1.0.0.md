# IoT-Security-Case-Hub V1.0.0 Release Readiness

Status: **READY FOR FINAL QA / STATIC REVIEW / PM RELEASE GATE**

This is a release evidence package, not a release approval. No Git tag, GitHub Release, merge to `main`, or production deployment is created by this document.

## Candidate and Runtime Contract

| Item | Contract |
| --- | --- |
| Repository | `roxy-wlaq/IoT-Security-Case-Hub` |
| Branch | `dev/v1-implementation` |
| Phase 30 base / current pushed candidate | `9a7bdd67d323be281d7851bca56010fe468a2151` |
| Recommended version | `V1.0.0` |
| Database | PostgreSQL 16 |
| Backend | Java 21 / Spring Boot 3.x |
| Frontend | React / TypeScript / Vite / Ant Design |
| Authentication | Server-side Session + CSRF |
| Deployment | Nginx + backend + PostgreSQL + persistent file storage |
| Migration ceiling | V018; no V019 |

## Requirement Coverage Matrix

| Area | Status | Evidence / boundary |
| --- | --- | --- |
| Authentication / Session / CSRF | IMPLEMENTED + VERIFIED | Spring Security Session, BCrypt, CSRF cookie/header contract |
| RBAC / resource authorization | IMPLEMENTED + VERIFIED | Permission and resource-level service policies |
| Dictionary / Capability Library | IMPLEMENTED + VERIFIED | Phase 4/5 API/UI and hierarchy validation |
| Master Test Case / Lifecycle | IMPLEMENTED + VERIFIED | Draft, review, publish, immutable version, revision |
| Decision Point / DAG | IMPLEMENTED + VERIFIED | Cardinality, duplicate targets, cycle checks, graph UI |
| Project / Project Capability | IMPLEMENTED + VERIFIED | Project visibility, management, effective capability values |
| Generation Rule / Runtime | IMPLEMENTED + VERIFIED | Conditions, recommendations, FULL and PROGRESSIVE_INITIAL |
| Project Test Plan / My Tests | IMPLEMENTED + VERIFIED | Version-bound PTC, TESTER assignment, assignment execution |
| Evidence / Storage / Notes | IMPLEMENTED + VERIFIED | Persistent file storage, authorization, transaction recovery |
| Execution / Progressive Runtime | IMPLEMENTED + VERIFIED | Start, decisions, outcomes, triggers, predecessor runtime |
| CONNECTED / FLOATING / Logic Graph | IMPLEMENTED + VERIFIED | Relations and React Flow read-only/runtime graph |
| Custom Test Case | IMPLEMENTED + VERIFIED | Custom definition, transition rules, TESTER-only assignment |
| Capability / Test Case Change Requests | IMPLEMENTED + VERIFIED | Review locks, mutation, revision lifecycle |
| Version Upgrade | IMPLEMENTED + VERIFIED | Availability, diff, keep/upgrade binding |
| Excel Export | IMPLEMENTED + VERIFIED | Read-only, formula-safe export |
| Audit | IMPLEMENTED + VERIFIED | Append-only persistence, authorization, sanitization, stable paging |
| Production Security | IMPLEMENTED + VERIFIED | HTTPS, headers, CORS, cookie, CSRF, rate limit, Actuator |
| Deployment / Persistence | IMPLEMENTED + VERIFIED | Nginx-only entry, internal backend/PostgreSQL, volumes |
| Backup / Restore | IMPLEMENTED + VERIFIED | DB dump + file archive + manifest/checksums + preflight |
| Upgrade / Rollback | IMPLEMENTED + VERIFIED | Backup-first upgrade; image rollback vs data restore distinction |
| E2E Acceptance | IMPLEMENTED + VERIFIED | Real Chromium through HTTPS Nginx/Spring/PostgreSQL |

## Security Negative Checklist

The release review found no occurrence in tracked release files of:

- JWT migration or browser-stored authentication tokens;
- test login bypass or test-only production endpoint;
- wildcard credentialed CORS;
- public PostgreSQL or public Evidence static directory;
- committed TLS private key or recognizable provider/API token signature;
- functional hardcoded production credential.

`change-me-please` and `${VARIABLE}` values remain placeholders in `.env.example`; production startup requires runtime replacement/supply. Bootstrap Admin creation requires `CASEHUB_BOOTSTRAP_ADMIN_PASSWORD` and does not silently use a built-in password.

## Documentation Contracts

Production HTTPS is unambiguous:

```text
SPRING_PROFILES_ACTIVE=prod
```

Explicit local HTTP debugging only:

```text
SPRING_PROFILES_ACTIVE=prod,http
```

Bootstrap Admin V1 contract:

```text
CASEHUB_BOOTSTRAP_ADMIN_PASSWORD is runtime supplied
created only when no ADMIN exists
must_change_password=false
no forced first-login reset in V1
operator rotates/removes the bootstrap value operationally
```

## Migration and Recovery Review

- Fresh-install migration chain is ordered `V001` through `V018`; no `V019` exists.
- Applied migrations are immutable and PostgreSQL 16 is the target database.
- Backup contains `database.dump`, `file-storage.tar`, `manifest.txt`, and SHA-256 values.
- Restore checks confirmation, backup structure, database identity, and checksums before destructive work.
- Upgrade creates a pre-upgrade backup before image deployment and health verification.
- Minor rollback uses previous immutable images when schema-compatible.
- Major/incompatible rollback restores the pre-upgrade database and file storage before starting the previous release.
- Flyway Community is not treated as a down-migration mechanism.

## E2E Evidence Review

Previously approved Phase 29 evidence covers two consecutive serial Chromium runs with 15/15 passing tests. The required groups are:

```text
Full Profile
Progressive Bluetooth
Multi-predecessor
Floating
Capability Request
Change Request
Version Upgrade
Permission
```

Auxiliary coverage includes Auth/Session/CSRF, Evidence, Export, Audit, and Custom Test Case. The E2E teardown safety contract is preserved: only `casehub-e2e` or `casehub-e2e-*` is accepted, and `casehub` is rejected before `down --volumes`.

## Scan Evidence

| Check | Status | Result |
| --- | --- | --- |
| `git diff --check` | PASS | No whitespace errors in the Phase 30 delta |
| `docker compose -f deploy/docker-compose.yml config --quiet` | PASS | Exit 0; only expected unset-secret warnings without a local `.env` |
| Migration enumeration | PASS | First `V001__init_schema.sql`, last `V018__audit_records.sql` |
| Tracked key/token signature scan | PASS | No private-key or recognizable provider-token signature found |
| TODO/FIXME/HACK scan | PASS | Only implementation-plan statement “No TODO blocking” found |
| E2E project safety check | PASS | Unsafe `casehub` rejected; safe `casehub-e2e-review` Compose config accepted |
| Typecheck / lint | PASS | Previous approved evidence; not rerun for documentation-only delta |
| Backend unit / IT / frontend tests | NOT RERUN — PREVIOUS APPROVED EVIDENCE | Final QA must rerun the consolidated gate |

The credential-assignment scan found only source-code parameter names, test fixtures, documentation, or placeholders; it did not identify a real committed credential.

## Known Issues Register

| ID | Severity | Description | Impact / workaround | V1 blocking | Target |
| --- | --- | --- | --- | --- | --- |
| KI-01 | Non-blocking | Two prior Vitest tests reached the 5-second environment timeout | QA may rerun with an environment-specific timeout diagnosis | NO | Post-V1 test hardening |
| KI-02 | Non-blocking | V1 bootstrap Admin does not force first-login password reset | Use strong runtime password, rotate it operationally, remove env value | NO | Post-V1 optional reset workflow |

No unresolved Blocker, High, or blocking Medium is recorded here. Any new final-QA discovery must be reclassified rather than silently deferred.

## Final Gate

```text
Phase 30 DEV finalization
→ consolidated QA
→ DEV_PUSH_PROMPT if QA PASS
→ exact SHA push
→ Static Review
→ PM final release decision
```
