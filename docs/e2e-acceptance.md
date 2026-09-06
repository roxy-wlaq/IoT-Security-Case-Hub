# Phase 29 E2E Acceptance

Phase 29 uses Playwright against the deployed application entry point. It does
not add a test-only backend endpoint, disable CSRF, or replace the database
with a mock.

## Disposable environment

From the repository root:

```bash
deploy/scripts/e2e-up.sh
cd frontend
npm ci
npm run e2e:install
```

The overlay retains the production topology and exposes only disposable host
ports (`8081` / `8443`). The TLS certificate is generated under a temporary
directory and is never committed. Use `E2E_BASE_URL` to point Playwright at a
different controlled deployment.

## Bootstrap and fixtures

`npm run e2e:bootstrap` creates the five E2E personas through `psql` inside the
disposable PostgreSQL container and assigns only their normal seeded roles. It
does not change production application code or expose a login bypass. Set
`E2E_FIXTURE_FILE` to a JSON file based on
`frontend/e2e/scenario-fixtures.example.json`; the file contains IDs for the
stable scenario data created by the controlled fixture preparation step.

```bash
cd frontend
E2E_FIXTURE_FILE=e2e/scenario-fixtures.json npm run e2e:bootstrap
E2E_BASE_URL=https://127.0.0.1:8443 npm run e2e
```

The default password is only for the disposable E2E environment and is
overridable with `E2E_PASSWORD`; it is not a production credential.

## Test organization and artifacts

The eight mandatory scenario groups are split into dedicated specs:

- `full-profile.spec.ts`
- `progressive-bluetooth.spec.ts`
- `multi-predecessor.spec.ts`
- `floating.spec.ts`
- `capability-request.spec.ts`
- `change-request.spec.ts`
- `version-upgrade.spec.ts`
- `permission.spec.ts`

Authentication/CSRF and integrated Evidence/Export/Audit/Custom coverage are
in separate auxiliary specs. Chromium is the blocking browser. Local runs use
zero retries; CI permits one controlled retry. Failed tests retain screenshots,
traces and videos under `frontend/test-results`; the HTML report is under
`frontend/playwright-report`.

Run the complete suite twice before QA handoff:

```bash
npm run e2e
npm run e2e
```

Finally dispose of all E2E volumes:

```bash
deploy/scripts/e2e-down.sh
```

Phase 30 release review is intentionally out of scope.
