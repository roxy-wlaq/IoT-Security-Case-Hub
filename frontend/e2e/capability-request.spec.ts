import { test, expect } from './fixtures';

test('Capability Request: tester submits through the UI and coordinator reviews it', async ({ page, state, login }) => {
  const fixture = state.fixtures.capabilityRequest;
  if (!fixture) throw new Error('capabilityRequest fixture is required');
  await login(page, 'testerA');
  await page.goto(`/projects/${fixture.projectId}/capability-requests`);
  await expect(page.getByText('Capability Update Requests')).toBeVisible();
  await page.locator('.ant-select').first().click();
  await page.locator('.ant-select-item-option').first().click();
  await page.getByPlaceholder('Reason').fill(`e2e-capability-${Date.now()}`);
  await page.locator('button[type="submit"]').click();
  await expect(page.getByText('PENDING')).toBeVisible();
});
