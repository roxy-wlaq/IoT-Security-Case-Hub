import { test, expect } from './fixtures';

test('Full Profile: project, plan, assignment, execution, note and evidence remain integrated', async ({ page, state, login }) => {
  const fixture = state.fixtures.fullProfile;
  if (!fixture?.projectTestCaseId) throw new Error('fullProfile.projectTestCaseId is required');
  await login(page, 'testerA');
  await page.goto(`/my-tests/${fixture.projectTestCaseId}`);
  await expect(page.getByText('执行测试')).toBeVisible();
  await expect(page.getByText('NOT_STARTED')).toBeVisible();
  await page.getByRole('button', { name: '开始执行' }).click();
  await expect(page.getByText('IN_PROGRESS')).toBeVisible();
  await page.getByRole('tab', { name: /Evidence/ }).click();
  await page.locator('input[type="file"]').setInputFiles('e2e/fixtures/evidence.txt');
  await expect(page.getByText('evidence.txt')).toBeVisible();
  await page.getByRole('tab', { name: /Notes/ }).click();
  await page.getByPlaceholder('记录执行 Note').fill(`full-profile-${Date.now()}`);
  await page.locator('.ant-space-compact button').click();
  await expect(page.getByText(/full-profile-/)).toBeVisible();
  await page.getByRole('tab', { name: 'Decision Points' }).click();
  await page.getByRole('checkbox').first().check();
  await page.locator('button.ant-btn-primary').click();
  await expect(page.getByText('COMPLETED')).toBeVisible();
});
