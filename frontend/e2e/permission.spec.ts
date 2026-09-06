import { test, expect } from './fixtures';

test.describe('permission boundaries through the deployed browser path', () => {
  test('unrelated tester cannot enter the protected project', async ({ page, state, login }) => {
    const projectId = state.fixtures.fullProfile?.projectId;
    if (!projectId) throw new Error('fullProfile.projectId is required for permission acceptance');
    await login(page, 'unauthorizedTester');
    const response = await page.request.get(`/api/v1/projects/${projectId}`);
    expect(response.status()).toBeGreaterThanOrEqual(403);
  });

  test('tester can view My Tests but cannot use coordinator-only project mutations', async ({ page, state, login }) => {
    const projectId = state.fixtures.fullProfile?.projectId;
    if (!projectId) throw new Error('fullProfile.projectId is required for permission acceptance');
    await login(page, 'testerA');
    await page.goto('/my-tests');
    await expect(page.getByRole('heading', { name: '我的测试' })).toBeVisible();
    const response = await page.request.post(`/api/v1/projects/${projectId}/generation/runs`, {
      data: { mode: 'FULL', triggerType: 'MANUAL_REGENERATE' },
    });
    expect(response.status()).toBeGreaterThanOrEqual(403);
  });

  test('non-admin cannot read the Audit API even when navigating directly', async ({ page, login }) => {
    await login(page, 'testerA');
    const response = await page.request.get('/api/v1/audit-logs');
    expect(response.status()).toBeGreaterThanOrEqual(403);
  });
});
