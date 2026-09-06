import { test, expect } from './fixtures';

test('Floating: removing a selected path preserves the target and exposes its relation state', async ({ page, state, login }) => {
  const fixture = state.fixtures.floating;
  if (!fixture) throw new Error('floating fixture is required');
  await login(page, 'testerA');
  await page.goto(`/my-tests/${fixture.targetProjectTestCaseId}`);
  await expect(page.getByText('执行测试')).toBeVisible();
  await page.getByRole('tab', { name: 'Project Logic Graph' }).click();
  const targetNode = page.getByTestId(`rf__node-${fixture.targetProjectTestCaseId}`);
  await expect(targetNode).toBeVisible();
  await expect(targetNode).toContainText(/FLOATING|CONNECTED/);
});
