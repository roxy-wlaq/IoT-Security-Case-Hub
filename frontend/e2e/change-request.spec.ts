import { test, expect } from './fixtures';

test('Change Request: request page is available for a concrete published source version', async ({ page, state, login }) => {
  const fixture = state.fixtures.changeRequest;
  if (!fixture) throw new Error('changeRequest fixture is required');
  await login(page, 'testerA');
  await page.goto(`/test-cases/${fixture.masterTestCaseId}/change-requests`);
  await expect(page.getByText(/Change Request|变更请求/)).toBeVisible();
});
