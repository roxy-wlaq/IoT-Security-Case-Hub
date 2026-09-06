import { test, expect } from './fixtures';

test('Progressive Bluetooth: entry execution creates/reuses the runtime target and graph edge', async ({ page, state, login }) => {
  const fixture = state.fixtures.progressiveBluetooth;
  if (!fixture) throw new Error('progressiveBluetooth fixture is required');
  await login(page, 'testerA');
  await page.goto(`/my-tests/${fixture.entryProjectTestCaseId}`);
  await expect(page.getByText('执行测试')).toBeVisible();
  await page.locator('button').filter({ hasText: '开始执行' }).click();
  const choices = page.getByRole('checkbox');
  if (await choices.count()) await choices.first().check();
  await page.locator('button.ant-btn-primary').click();
  await expect(page.getByText('COMPLETED')).toBeVisible();
  await expect(page.getByText('Project Logic Graph')).toBeVisible();
});
