# V1.0.0 Release Candidate Package

## Package Identity

| Field | Value |
| --- | --- |
| Product | IoT-Security-Case-Hub |
| Recommended version | V1.0.0 |
| Repository | `roxy-wlaq/IoT-Security-Case-Hub` |
| Candidate branch | `dev/v1-implementation` |
| Current approved pushed baseline | `9a7bdd67d323be281d7851bca56010fe468a2151` |
| Final Candidate SHA | **Assigned only after final QA PASS and exact DEV push** |
| Migration | V001–V018 |
| Database | PostgreSQL 16 |
| Backend | Java 21 / Spring Boot 3.x |
| Frontend | React / TypeScript / Vite |
| Authentication | Server-side Session + CSRF |
| Deployment | Nginx + backend + PostgreSQL + file storage |
| Blocking issues declared by DEV | 0; final gates pending |

## Required Gates

```text
QA consolidated Phase 30 PASS
→ DEV_PUSH_PROMPT
→ push exact QA-validated SHA
→ Static Review PASS
→ PM final release decision
```

The final SHA must be the full 40-character remote SHA returned after the QA-approved push. This package intentionally does not invent that value and does not claim a Git tag or GitHub Release exists.

## Included Evidence

- Final implementation ledger: `IMPLEMENTATION_STATUS.md`
- Release readiness matrix and scan record: `docs/release-readiness-v1.0.0.md`
- Operator deployment contract: `deploy/README.md`
- Production Compose: `deploy/docker-compose.yml`
- Backup / restore / upgrade / rollback scripts under `deploy/scripts/`
- Phase 29 browser acceptance contract: `docs/e2e-acceptance.md`
- E2E safety guard: `deploy/scripts/e2e-project-guard.sh`

## Release Boundaries

- No Phase 31 feature is included.
- No new business migration is included; V018 remains the ceiling.
- No merge to `main`, tag, GitHub Release, or production deployment is performed automatically.
