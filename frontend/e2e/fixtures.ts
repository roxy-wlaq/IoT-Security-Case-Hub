import { test as base, expect, type Page } from '@playwright/test';
import { loginApi } from './support/api';
import { loadE2EState } from './support/state';
import type { E2EState, E2EUserKey } from './support/types';

type E2EFixtures = {
  state: E2EState;
  login: (page: Page, user: E2EUserKey) => Promise<void>;
};

export const test = base.extend<E2EFixtures>({
  state: async ({ browserName }, runFixture) => {
    void browserName;
    await runFixture(loadE2EState());
  },
  login: async ({ state }, runFixture) => {
    await runFixture(async (page, userKey) => {
      const user = state.users[userKey];
      if (!user) throw new Error(`Missing E2E user fixture: ${userKey}`);
      const cookies = state.sessions?.[userKey];
      if (!cookies?.length) throw new Error(`Missing E2E session fixture: ${userKey}`);
      await page.context().addCookies(cookies);
      await page.goto('/');
      await expect(page).not.toHaveURL(/\/login$/, { timeout: 30_000 });
    });
  },
});

export { expect };

export async function sessionApi(user: { username: string; password: string }) {
  const api = await (await import('./support/api')).createApiContext();
  await loginApi(api, user);
  return api;
}
