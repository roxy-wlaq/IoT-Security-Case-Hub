import { test, expect } from './fixtures';

test('Version Upgrade: the same Project Test Case exposes keep/upgrade choices', async ({ page, state, login }) => {
  const fixture = state.fixtures.versionUpgrade;
  if (!fixture) throw new Error('versionUpgrade fixture is required');
  await login(page, 'coordinator');
  await page.goto(`/my-tests/${fixture.projectTestCaseId}/version`);
  await expect(page.getByText('Version Upgrade')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Keep old version' })).toBeVisible();
});
