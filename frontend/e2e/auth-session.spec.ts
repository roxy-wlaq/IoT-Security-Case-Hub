import { test, expect } from './fixtures';
import { createApiContext, csrfHeader, expectStatus } from './support/api';

test.describe('authentication, server session and CSRF', () => {
  test('normal login establishes a session and logout invalidates it', async ({ page, state }) => {
    await page.goto('/login');
    await page.locator('input[autocomplete="username"]').fill(state.users.testerA.username);
    await page.locator('input[autocomplete="current-password"]').fill(state.users.testerA.password);
    await page.locator('button[type="submit"]').click();
    await expect(page).not.toHaveURL(/\/login$/, { timeout: 30_000 });
    await expect(page.getByRole('menu').getByText('我的测试')).toBeVisible();
    await page.getByRole('button', { name: /退出/ }).click();
    await expect(page).toHaveURL(/\/login$/);
  });

  test('a mutation without the application CSRF header is rejected', async ({ state }) => {
    const api = await createApiContext(state.sessions?.admin);
    const response = await api.post('/api/v1/projects', {
      data: { projectName: `csrf-negative-${Date.now()}`, deviceName: 'E2E', generationMode: 'FULL', standardTaskTypeIds: [state.fixtures.standardTaskTypeId] },
    });
    await expectStatus(response, 403);
    await api.dispose();
  });

  test('the valid CSRF contract permits an authorized mutation', async ({ state }) => {
    const api = await createApiContext(state.sessions?.coordinator);
    const response = await api.post('/api/v1/projects', {
      headers: await csrfHeader(api),
      data: { projectName: `csrf-positive-${Date.now()}`, deviceName: 'E2E', generationMode: 'FULL', standardTaskTypeIds: [state.fixtures.standardTaskTypeId] },
    });
    await expect(response).toBeOK();
    await api.dispose();
  });
});
