import { httpClient } from '@/shared/api/httpClient';
import type { ChangePasswordRequest, CurrentUser, LoginRequest } from '@/shared/types/auth';

/**
 * Lead 冻结的认证 API。禁止在本文件之外发明新的 /api/v1 端点。
 */
const AUTH_BASE = '/auth';

export async function login(payload: LoginRequest): Promise<CurrentUser> {
  const response = await httpClient.post<CurrentUser>(`${AUTH_BASE}/login`, payload);
  return response.data;
}

export async function logout(): Promise<void> {
  await httpClient.post(`${AUTH_BASE}/logout`);
}

export async function fetchCurrentUser(): Promise<CurrentUser> {
  const response = await httpClient.get<CurrentUser>(`${AUTH_BASE}/me`);
  return response.data;
}

export async function changePassword(payload: ChangePasswordRequest): Promise<void> {
  await httpClient.post(`${AUTH_BASE}/change-password`, payload);
}
