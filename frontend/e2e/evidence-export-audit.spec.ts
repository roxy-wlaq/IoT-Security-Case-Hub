import { test, expect } from './fixtures';

test('integrated governance surfaces: export download and audit page are reachable', async ({ page, state, login }) => {
  const projectId = state.fixtures.fullProfile?.projectId;
  if (!projectId) throw new Error('fullProfile.projectId is required');
  await login(page, 'admin');
  await page.goto('/projects');
  await expect(page.getByRole('heading', { name: '项目管理' })).toBeVisible();
  await page.goto('/audit-logs');
  await expect(page.getByRole('heading', { name: '审计日志' })).toBeVisible();
  const response = await page.request.get(`/api/v1/projects/${projectId}/export.xlsx`);
  expect(response.ok()).toBeTruthy();
  expect(response.headers()['content-type']).toContain('spreadsheet');
});

test('custom project case is browser-visible', async ({ page, state, login }) => {
  const projectId = state.fixtures.customCase?.projectId ?? state.fixtures.fullProfile?.projectId;
  if (!projectId) throw new Error('customCase.projectId or fullProfile.projectId is required');
  await login(page, 'testerA');
  await page.goto(`/projects/${projectId}/custom-cases`);
  await expect(page.getByText('Project Custom Test Cases')).toBeVisible();
  await expect(page.locator('button[type="submit"]')).toBeVisible();
});
