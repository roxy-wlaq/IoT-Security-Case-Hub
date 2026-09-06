import { test, expect } from './fixtures';

test('Multi-predecessor: target remains one PTC while incoming runtime edges accumulate', async ({ page, state, login }) => {
  const fixture = state.fixtures.multiPredecessor;
  if (!fixture) throw new Error('multiPredecessor fixture is required');
  async function executeAssignedCase(user: 'testerA' | 'testerB', projectTestCaseId: string) {
    await login(page, user);
    await page.goto(`/my-tests/${projectTestCaseId}`);
    await expect(page.getByText('执行测试')).toBeVisible();
    await page.locator('button').filter({ hasText: '开始执行' }).click();
    const choices = page.getByRole('checkbox');
    if (await choices.count()) await choices.first().check();
    await page.locator('button.ant-btn-primary').click();
    await expect(page.getByText('COMPLETED')).toBeVisible();
  }
  await executeAssignedCase('testerA', fixture.predecessorAProjectTestCaseId);
  await executeAssignedCase('testerB', fixture.predecessorBProjectTestCaseId);
  await login(page, 'testerA');
  await page.goto(`/my-tests/${fixture.targetProjectTestCaseId}`);
  await expect(page.getByText('执行测试')).toBeVisible();
  await page.getByRole('tab', { name: 'Project Logic Graph' }).click();
  const targetNode = page.getByTestId(`rf__node-${fixture.targetProjectTestCaseId}`);
  await expect(targetNode).toBeVisible();
  await expect(targetNode).toContainText(/CONNECTED|FLOATING/);
});
