import { expect, request, type APIRequestContext, type APIResponse } from '@playwright/test';
import type { E2ESessionCookie, E2EUser } from './types';

export async function createApiContext(cookies?: E2ESessionCookie[]): Promise<APIRequestContext> {
  return request.newContext({
    baseURL: process.env.E2E_BASE_URL ?? 'https://127.0.0.1:8443',
    ignoreHTTPSErrors: process.env.E2E_IGNORE_HTTPS_ERRORS !== 'false',
    storageState: cookies ? { cookies } : undefined,
  });
}

export async function loginApi(api: APIRequestContext, user: E2EUser): Promise<void> {
  const csrf = await api.get('/api/v1/auth/csrf');
  await expect(csrf).toBeOK();
  const csrfBody = (await csrf.json()) as { token: string; headerName: string };
  const login = await api.post('/api/v1/auth/login', {
    data: { username: user.username, password: user.password },
    headers: { [csrfBody.headerName]: csrfBody.token },
  });
  await expect(login).toBeOK();
}

export async function csrfHeader(api: APIRequestContext): Promise<Record<string, string>> {
  const csrf = await api.get('/api/v1/auth/csrf');
  await expect(csrf).toBeOK();
  const body = (await csrf.json()) as { token: string; headerName: string };
  return { [body.headerName]: body.token };
}

export async function expectStatus(response: APIResponse, status: number): Promise<void> {
  expect(response.status(), await response.text()).toBe(status);
}
