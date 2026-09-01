/**
 * 与后端（Agent A）冻结契约完全对应的前端类型。
 *
 * 唯一可依赖的接口（Lead 冻结）：
 *   GET  /api/v1/auth/csrf
 *   POST /api/v1/auth/login
 *   POST /api/v1/auth/logout
 *   GET  /api/v1/auth/me
 *   POST /api/v1/auth/change-password
 */

export type RoleCode = 'ADMIN' | 'TEST_COORDINATOR' | 'TESTER';

export const ROLE_CODES: readonly RoleCode[] = ['ADMIN', 'TEST_COORDINATOR', 'TESTER'];

/** `GET /api/v1/auth/me` / `POST /api/v1/auth/login` 响应体 */
export interface CurrentUser {
  id: string;
  username: string;
  displayName: string;
  enabled: boolean;
  mustChangePassword: boolean;
  roles: string[];
  permissions: string[];
}

/** `GET /api/v1/auth/csrf` 响应体 */
export interface CsrfTokenResponse {
  headerName: string;
  cookieName: string;
  token: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

/** 统一错误响应体（Backend Architecture V1.0 §77） */
export interface ApiErrorBody {
  code: string;
  message: string;
  traceId: string;
  details: Record<string, unknown>;
}
