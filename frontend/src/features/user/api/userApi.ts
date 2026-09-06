import { httpClient } from '@/shared/api/httpClient';
import type { PagedResponse } from '@/shared/types/testCase';

/**
 * 管理员用户管理 API（冻结契约）。
 *
 *   GET    /api/v1/users?q=&enabled=&role=&page=&size=
 *   POST   /api/v1/users
 *   PUT    /api/v1/users/{id}
 *   PUT    /api/v1/users/{id}/roles
 *   POST   /api/v1/users/{id}/enable
 *   POST   /api/v1/users/{id}/disable
 *   POST   /api/v1/users/{id}/password-reset
 *   GET    /api/v1/roles
 */

export interface UserSummary {
  id: string;
  username: string;
  displayName: string;
  enabled: boolean;
  mustChangePassword: boolean;
  roles: string[];
}

export interface UserListParams {
  q?: string;
  enabled?: boolean;
  role?: string;
  page?: number;
  size?: number;
}

export interface Role {
  code: string;
  name?: string;
  description?: string;
}

export async function listUsers(params: UserListParams = {}): Promise<PagedResponse<UserSummary>> {
  return (await httpClient.get<PagedResponse<UserSummary>>('/users', { params })).data;
}

export interface UserCreatePayload {
  username: string;
  displayName: string;
  /** 缺省时由后端生成初始密码，并在响应中一次性返回 */
  password?: string;
  roles: string[];
}

export async function createUser(payload: UserCreatePayload): Promise<UserSummary & { generatedPassword: string | null }> {
  return (await httpClient.post<UserSummary & { generatedPassword: string | null }>('/users', payload)).data;
}

export async function updateUser(id: string, payload: { displayName: string }): Promise<UserSummary> {
  return (await httpClient.put<UserSummary>(`/users/${id}`, payload)).data;
}

export async function updateUserRoles(id: string, roles: string[]): Promise<UserSummary> {
  return (await httpClient.put<UserSummary>(`/users/${id}/roles`, { roles })).data;
}

export async function enableUser(id: string): Promise<UserSummary> {
  return (await httpClient.post<UserSummary>(`/users/${id}/enable`)).data;
}

export async function disableUser(id: string): Promise<UserSummary> {
  return (await httpClient.post<UserSummary>(`/users/${id}/disable`)).data;
}

export interface PasswordResetResult {
  generated: boolean;
  /** 仅在后端生成临时密码时返回，且只返回这一次 */
  password: string | null;
  mustChangePassword: boolean;
}

export async function resetUserPassword(id: string, password?: string): Promise<PasswordResetResult> {
  return (await httpClient.post<PasswordResetResult>(`/users/${id}/password-reset`, { password })).data;
}

export async function listRoles(): Promise<Role[]> {
  return (await httpClient.get<Role[]>('/roles')).data;
}
