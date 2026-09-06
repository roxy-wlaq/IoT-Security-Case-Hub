# IoT-Security-Case-Hub V1.0.0

Release status: **Release Candidate preparation**

This document recommends semantic version `V1.0.0`. It does not create or imply a Git tag, GitHub Release, merge to `main`, or production deployment.

## Candidate

- Repository: `roxy-wlaq/IoT-Security-Case-Hub`
- Branch: `dev/v1-implementation`
- Current approved baseline: `9a7bdd67d323be281d7851bca56010fe468a2151`
- Final candidate SHA: pending final QA, push, and Static Review; do not substitute an unpushed SHA
- Database migrations: `V001`–`V018`

## Major Capabilities

- Server-side Session authentication, RBAC, resource authorization, and CSRF.
- Dictionary and Capability Library administration.
- Master Test Case lifecycle, immutable published versions, review history, and revisions.
- Decision Point/DAG validation and Project Logic Graph visualization.
- Project Planning, Capability values, Generation Rules, FULL/PROGRESSIVE runtime, and Project Test Plans.
- Assigned My Tests, Evidence, Notes, execution outcomes, Progressive Runtime, CONNECTED/FLOATING, and relation updates.
- Custom Test Cases, Capability Update Requests, Test Case Change Requests, and version upgrades.
- Formula-safe Excel Export and append-only Audit governance.
- Production HTTPS deployment, persistent storage, backup/restore, upgrade/rollback procedures, and real-browser E2E coverage.

## Security Architecture

- Java 21 / Spring Boot 3.x with PostgreSQL 16.
- Nginx is the only external application entry point; backend and PostgreSQL remain internal.
- Production uses `SPRING_PROFILES_ACTIVE=prod`, Secure + HttpOnly + SameSite=Lax session cookies, CSRF, HSTS, CSP, frame protection, nosniff, Referrer-Policy, and Permissions-Policy.
- `prod,http` is reserved for explicit local HTTP debugging.
- Login throttling uses username and trusted source-IP dimensions.
- Actuator exposes health only in production; secrets are runtime supplied.
- No JWT, localStorage auth token, credentialed wildcard CORS, public PostgreSQL, or public Evidence directory.

## Deployment

Production services are:

```text
Browser → HTTPS Nginx → Spring Boot backend → PostgreSQL 16
                                      ↘ persistent file storage
```

Use immutable image references, for example:

```text
casehub-backend:1.0.0
casehub-nginx:1.0.0
```

Provide database credentials, bootstrap Admin password, and TLS material at runtime. Do not commit `.env`, private keys, dumps, or frontend server secrets.

## Backup and Restore

The backup set contains:

```text
database.dump       PostgreSQL custom-format dump
file-storage.tar    persistent application files
manifest.txt        identity, timestamps, and SHA-256 values
```

Backup uses an explicit write-quiescence procedure. Restore validates confirmation, backup structure, database identity, and checksums before replacing data. Representative restored records and Evidence file hashes must be checked in the final operational acceptance.

## Upgrade and Rollback

Upgrade is backup-first: create and verify the pre-upgrade backup, deploy immutable images/configuration, validate Flyway and health, then run functional smoke checks.

For a schema-compatible minor rollback, restore previous immutable images/configuration. For an incompatible major rollback, restore the pre-upgrade database and file storage before starting the previous immutable release. Flyway is not treated as a down-migration tool.

## Known Non-Blocking Issues

- Two prior Vitest cases reached a 5-second environment timeout; final QA should rerun and classify the environment behavior.
- Bootstrap Admin does not force first-login password reset in V1; operators must use a strong runtime password, rotate it operationally, and remove the bootstrap variable.

Neither item is a release blocker under the current V1 contract. Final QA, Static Review, and PM must still approve the candidate.
