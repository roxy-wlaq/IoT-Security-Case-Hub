import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import bcrypt from 'bcryptjs';
import { request } from '@playwright/test';

const projectName = process.env.E2E_COMPOSE_PROJECT ?? 'casehub-e2e';
const composeFiles = (process.env.E2E_COMPOSE_FILES ?? 'deploy/docker-compose.yml deploy/docker-compose.e2e.yml').split(' ').filter(Boolean);
const composeArgs = composeFiles.flatMap((file) => ['-f', file]);
const postgresDb = process.env.POSTGRES_DB ?? 'casehub';
const postgresUser = process.env.POSTGRES_USER ?? 'casehub';
const password = process.env.E2E_PASSWORD ?? 'E2ePassword!2026';
const statePath = process.env.E2E_STATE_FILE ?? path.resolve(process.cwd(), 'e2e/.state.json');

const users = {
  admin: { username: process.env.E2E_ADMIN_USERNAME ?? 'e2e-admin', displayName: 'E2E Admin', roles: ['ADMIN'] },
  coordinator: { username: process.env.E2E_COORDINATOR_USERNAME ?? 'e2e-coordinator', displayName: 'E2E Coordinator', roles: ['TEST_COORDINATOR'] },
  testerA: { username: process.env.E2E_TESTER_A_USERNAME ?? 'e2e-tester-a', displayName: 'E2E Tester A', roles: ['TESTER'] },
  testerB: { username: process.env.E2E_TESTER_B_USERNAME ?? 'e2e-tester-b', displayName: 'E2E Tester B', roles: ['TESTER'] },
  unauthorizedTester: { username: process.env.E2E_UNAUTHORIZED_USERNAME ?? 'e2e-outsider', displayName: 'E2E Outsider', roles: ['TESTER'] },
};

function sqlLiteral(value) {
  return `'${String(value).replaceAll("'", "''")}'`;
}

function runPsql(sql) {
  const args = ['compose', '--project-name', projectName, ...composeArgs, 'exec', '-T', 'postgres', 'psql', '-v', 'ON_ERROR_STOP=1', '-U', postgresUser, '-d', postgresDb, '-At'];
  return execFileSync('docker', args, { input: sql, encoding: 'utf8', stdio: ['pipe', 'pipe', 'inherit'] }).trim();
}

const passwordHash = await bcrypt.hash(password, 12);
const rows = Object.values(users).map((user) => `
INSERT INTO casehub.users (id, username, display_name, password_hash, enabled, must_change_password)
VALUES (gen_random_uuid(), ${sqlLiteral(user.username)}, ${sqlLiteral(user.displayName)}, ${sqlLiteral(passwordHash)}, TRUE, FALSE)
ON CONFLICT (username) DO UPDATE SET display_name = EXCLUDED.display_name, password_hash = EXCLUDED.password_hash, enabled = TRUE, must_change_password = FALSE;
${user.roles.map((role) => `
INSERT INTO casehub.user_roles (id, user_id, role_id)
SELECT gen_random_uuid(), u.id, r.id FROM casehub.users u CROSS JOIN casehub.roles r
WHERE u.username = ${sqlLiteral(user.username)} AND r.code = ${sqlLiteral(role)}
ON CONFLICT (user_id, role_id) DO NOTHING;`).join('\n')}`).join('\n');
runPsql(rows);

const ids = runPsql(`SELECT username || '=' || id FROM casehub.users WHERE username IN (${Object.values(users).map((user) => sqlLiteral(user.username)).join(',')});`)
  .split('\n').filter(Boolean).reduce((result, row) => { const [username, id] = row.split('='); result[username] = id; return result; }, {});
async function apiFixtureState() {
  const api = await request.newContext({
    baseURL: process.env.E2E_BASE_URL ?? 'https://127.0.0.1:8443',
    ignoreHTTPSErrors: true,
  });
  async function mutate(method, url, data) {
    const csrfResponse = await api.get('/api/v1/auth/csrf');
    if (!csrfResponse.ok()) throw new Error(`CSRF bootstrap failed: ${csrfResponse.status()}`);
    const csrf = await csrfResponse.json();
    const response = await api.fetch(url, { method, data, headers: { [csrf.headerName]: csrf.token } });
    if (!response.ok()) throw new Error(`${method} ${url} failed (${response.status()}): ${await response.text()}`);
    return response.status() === 204 ? null : response.json();
  }
  async function login(username) {
    const csrfResponse = await api.get('/api/v1/auth/csrf');
    const csrf = await csrfResponse.json();
    const response = await api.post('/api/v1/auth/login', { data: { username, password }, headers: { [csrf.headerName]: csrf.token } });
    if (!response.ok()) throw new Error(`E2E admin login failed (${response.status()}): ${await response.text()}`);
  }
  const admin = users.admin;
  await login(admin.username);
  const standards = await (await api.get('/api/v1/standard-task-types?enabled=true')).json();
  const standard = standards[0] ?? await mutate('POST', '/api/v1/standard-task-types', { code: `E2E_STD_${Date.now()}`, name: 'E2E Standard', type: 'STANDARD', description: 'Disposable E2E fixture', enabled: true });
  const categories = await (await api.get('/api/v1/categories/tree?enabled=true')).json();
  const category = categories[0] ?? await mutate('POST', '/api/v1/categories', { code: `E2E_CAT_${Date.now()}`, name: 'E2E Category', parentId: null, description: 'Disposable E2E fixture', sortOrder: 1, enabled: true });
  const capabilityTree = await (await api.get('/api/v1/capabilities/tree')).json();
  const firstCapability = (function findNode(nodes) {
    for (const node of nodes) {
      if (!node.children?.length) return node;
      const child = findNode(node.children);
      if (child) return child;
    }
    return null;
  })(capabilityTree);
  const libraryCapability = firstCapability ?? await mutate('POST', '/api/v1/capabilities', {
    parentId: null, code: `E2E_BLUETOOTH_${Date.now()}`, name: 'E2E Bluetooth', description: 'Disposable E2E fixture', sortOrder: 1,
  });
  async function createCase(code, name, progressiveRole, evidenceRequired = false) {
    return mutate('POST', '/api/v1/test-cases', {
      caseCode: code, categoryId: category.id, caseName: name, testPurpose: 'Phase 29 E2E', preconditions: '',
      selectionMode: 'SINGLE', evidenceRequired, evidenceRequirement: evidenceRequired ? 'Evidence required for acceptance' : '',
      remarkRequirement: '', progressiveRole, steps: [{ title: 'E2E step', content: `Execute ${code}` }], tagIds: [], toolIds: [],
      standardMappings: [{ standardTaskTypeId: standard.id, mappingNote: 'E2E fixture' }],
    });
  }
  const suffix = Date.now();
  const full = await createCase(`E2E_FULL_${suffix}`, 'E2E Full Profile', null, true);
  const entry = await createCase(`E2E_ENTRY_${suffix}`, 'E2E Progressive Entry', 'ENTRY');
  const predecessorB = await createCase(`E2E_PRE_B_${suffix}`, 'E2E Multi Predecessor B', 'NORMAL');
  const normal = await createCase(`E2E_NORMAL_${suffix}`, 'E2E Progressive Normal', 'NORMAL');
  const predecessorA = await createCase(`E2E_PRE_A_${suffix}`, 'E2E Multi Predecessor A', 'NORMAL');
  async function addDecision(masterId, versionId, targetMasterTestCaseId, transitionType = 'NEXT_CASE') {
    return mutate('POST', `/api/v1/test-cases/${masterId}/versions/${versionId}/decision-points`, {
      name: 'Continue', description: 'E2E runtime edge', displayOrder: 1, transitionType,
      targetMasterTestCaseIds: targetMasterTestCaseId ? [targetMasterTestCaseId] : [],
    });
  }
  await addDecision(full.id, full.draftVersion.id, null, 'PASS');
  await addDecision(entry.id, entry.draftVersion.id, normal.id);
  await addDecision(predecessorA.id, predecessorA.draftVersion.id, normal.id);
  await addDecision(predecessorB.id, predecessorB.draftVersion.id, normal.id);
  async function publish(master) {
    await mutate('POST', `/api/v1/test-cases/${master.id}/draft/submit-review`, {});
    const versions = await (await api.get(`/api/v1/test-cases/${master.id}/versions`)).json();
    const review = versions.find((version) => version.status === 'REVIEW');
    await mutate('POST', `/api/v1/test-cases/${master.id}/versions/${review.id}/publish`, {});
    return review;
  }
  const fullVersion = await publish(full);
  await publish(entry); await publish(predecessorA); await publish(predecessorB); await publish(normal);
  const project = await mutate('POST', '/api/v1/projects', {
    projectName: `E2E Project ${suffix}`, deviceName: 'E2E Device', generationMode: 'PROGRESSIVE', standardTaskTypeIds: [standard.id],
    primaryCoordinatorId: ids[users.coordinator.username],
  });
  async function addToPlan(master, source = 'MANUAL') {
    return mutate('POST', `/api/v1/projects/${project.id}/test-plan?masterTestCaseId=${master.id}&source=${source}`, null);
  }
  const fullPtc = await addToPlan(full); const entryPtc = await addToPlan(entry); const aPtc = await addToPlan(predecessorA); const bPtc = await addToPlan(predecessorB); const normalPtc = await addToPlan(normal);
  async function assign(ptc, userKey) {
    await mutate('POST', `/api/v1/projects/${project.id}/test-plan/${ptc.id}/assignees`, { userId: ids[users[userKey].username] });
  }
  await assign(fullPtc, 'testerA'); await assign(entryPtc, 'testerA'); await assign(aPtc, 'testerA'); await assign(bPtc, 'testerB'); await assign(normalPtc, 'testerA');
  const capabilities = await (await api.get(`/api/v1/projects/${project.id}/capabilities`)).json();
  const capability = capabilities.find((item) => !item.derived) ?? { capabilityId: libraryCapability.id };
  const testerApi = await request.newContext({ baseURL: process.env.E2E_BASE_URL ?? 'https://127.0.0.1:8443', ignoreHTTPSErrors: true });
  const testerCsrf = await testerApi.get('/api/v1/auth/csrf'); const testerToken = await testerCsrf.json();
  await testerApi.post('/api/v1/auth/login', { data: { username: users.testerA.username, password }, headers: { [testerToken.headerName]: testerToken.token } });
  const custom = await (async () => {
    const csrf = await testerApi.get('/api/v1/auth/csrf'); const token = await csrf.json();
    const response = await testerApi.post(`/api/v1/projects/${project.id}/custom-test-cases`, { headers: { [token.headerName]: token.token }, data: {
      caseCode: `E2E_CUSTOM_${suffix}`, caseName: 'E2E Custom Case', testPurpose: 'Phase 29', preconditions: '', selectionMode: 'SINGLE',
      evidenceRequired: false, evidenceRequirement: '', remarkRequirement: '', steps: [{ sequenceNo: 1, title: 'Custom step', content: 'Execute custom step' }], decisionPoints: [],
    }});
    if (!response.ok()) throw new Error(`Custom fixture failed (${response.status()}): ${await response.text()}`);
    return response.json();
  })();
  const sessions = {
    admin: (await api.storageState()).cookies,
    testerA: (await testerApi.storageState()).cookies,
  };
  for (const [key, user] of Object.entries(users)) {
    if (sessions[key]) continue;
    const sessionApi = await request.newContext({ baseURL: process.env.E2E_BASE_URL ?? 'https://127.0.0.1:8443', ignoreHTTPSErrors: true });
    await loginSession(sessionApi, user.username);
    sessions[key] = (await sessionApi.storageState()).cookies;
    await sessionApi.dispose();
  }
  await testerApi.dispose(); await api.dispose();
  return {
    standardTaskTypeId: standard.id, categoryId: category.id,
    fullProfile: { projectId: project.id, masterTestCaseId: full.id, projectTestCaseId: fullPtc.id },
    progressiveBluetooth: { projectId: project.id, entryProjectTestCaseId: entryPtc.id, nextProjectTestCaseId: normalPtc.id },
    multiPredecessor: { projectId: project.id, predecessorAProjectTestCaseId: aPtc.id, predecessorBProjectTestCaseId: bPtc.id, targetProjectTestCaseId: normalPtc.id },
    floating: { projectId: project.id, sourceProjectTestCaseId: entryPtc.id, targetProjectTestCaseId: normalPtc.id },
    capabilityRequest: { projectId: project.id, capabilityId: capability.capabilityId },
    changeRequest: { masterTestCaseId: full.id, sourceVersionId: fullVersion.id },
    versionUpgrade: { projectTestCaseId: fullPtc.id, oldVersionId: fullVersion.id, newVersionId: fullVersion.id },
    customCase: { projectId: project.id, customTestCaseId: custom.id },
    sessions,
  };

  async function loginSession(sessionApi, username) {
    const csrfResponse = await sessionApi.get('/api/v1/auth/csrf');
    const csrf = await csrfResponse.json();
    const response = await sessionApi.post('/api/v1/auth/login', { data: { username, password }, headers: { [csrf.headerName]: csrf.token } });
    if (!response.ok()) throw new Error(`E2E session login failed for ${username} (${response.status()})`);
  }
}

const fixtureJson = process.env.E2E_FIXTURE_JSON;
const generated = fixtureJson ? JSON.parse(fixtureJson) : process.env.E2E_FIXTURE_FILE
  ? JSON.parse(fs.readFileSync(process.env.E2E_FIXTURE_FILE, 'utf8'))
  : await apiFixtureState();
const { sessions, ...fixtures } = generated;
const state = {
  users: Object.fromEntries(Object.entries(users).map(([key, user]) => [key, { username: user.username, password, id: ids[user.username] }])),
  fixtures,
  sessions,
};
fs.mkdirSync(path.dirname(statePath), { recursive: true });
fs.writeFileSync(statePath, `${JSON.stringify(state, null, 2)}\n`, { mode: 0o600 });
console.log(`E2E bootstrap complete. State: ${statePath}`);
