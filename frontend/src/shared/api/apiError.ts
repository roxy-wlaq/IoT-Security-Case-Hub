import { AxiosError } from 'axios';
import type { ApiErrorBody } from '@/shared/types/auth';
import type { RoleCode } from '@/shared/types/auth';

/**
 * Phase 3 需要区分的业务错误码。
 * 语义见 Lead 冻结的契约表与 Security & RBAC Detail V1.0。
 */
export const API_ERROR_CODES = {
  INVALID_CREDENTIALS: 'AUTH_INVALID_CREDENTIALS',
  LOGIN_TEMPORARILY_BLOCKED: 'AUTH_LOGIN_TEMPORARILY_BLOCKED',
  UNAUTHENTICATED: 'AUTH_UNAUTHENTICATED',
  UNAUTHORIZED: 'AUTH_UNAUTHORIZED',
  PASSWORD_CHANGE_REQUIRED: 'AUTH_PASSWORD_CHANGE_REQUIRED',
  VALIDATION_FAILED: 'VALIDATION_FAILED',
  USER_DISABLED: 'USER_DISABLED',
  NETWORK_ERROR: 'NETWORK_ERROR',
  UNKNOWN_ERROR: 'UNKNOWN_ERROR',
} as const;

export type ApiErrorCode = (typeof API_ERROR_CODES)[keyof typeof API_ERROR_CODES];

/** 错误码 → 中文文案（禁止直接把后端英文 message 暴露给用户） */
export const API_ERROR_MESSAGES: Record<string, string> = {
  [API_ERROR_CODES.INVALID_CREDENTIALS]: '用户名或密码错误',
  [API_ERROR_CODES.LOGIN_TEMPORARILY_BLOCKED]: '登录尝试过多，请 15 分钟后重试',
  [API_ERROR_CODES.UNAUTHENTICATED]: '登录状态已失效，请重新登录',
  [API_ERROR_CODES.UNAUTHORIZED]: '没有权限执行该操作',
  [API_ERROR_CODES.PASSWORD_CHANGE_REQUIRED]: '请先修改密码后再继续',
  [API_ERROR_CODES.VALIDATION_FAILED]: '请求参数校验失败',
  [API_ERROR_CODES.USER_DISABLED]: '账号已被禁用，请联系管理员',
  [API_ERROR_CODES.NETWORK_ERROR]: '网络异常，请检查连接后重试',
  [API_ERROR_CODES.UNKNOWN_ERROR]: '请求失败，请稍后重试',
};

/** 统一的应用内错误对象，Axios 响应拦截器会把所有失败转换成它 */
export class ApiError extends Error {
  readonly code: string;
  readonly status: number;
  readonly traceId: string;
  readonly details: Record<string, unknown>;

  constructor(params: { code: string; message: string; status: number; traceId?: string; details?: Record<string, unknown> }) {
    super(params.message);
    this.name = 'ApiError';
    this.code = params.code;
    this.status = params.status;
    this.traceId = params.traceId ?? '';
    this.details = params.details ?? {};
    // 保证 instanceof 在 ES5 目标下也成立
    Object.setPrototypeOf(this, ApiError.prototype);
  }

  /** 面向用户的中文文案；未知错误码回落到后端 message */
  get userMessage(): string {
    return resolveErrorMessage(this.code, this.message);
  }

  /** 5xx / 网络错误才向用户展示 traceId 以便排障 */
  get isReportable(): boolean {
    return this.code === API_ERROR_CODES.NETWORK_ERROR || this.code === API_ERROR_CODES.UNKNOWN_ERROR || this.status >= 500;
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

export function resolveErrorMessage(code: string, fallbackMessage?: string): string {
  return API_ERROR_MESSAGES[code] ?? fallbackMessage ?? API_ERROR_MESSAGES[API_ERROR_CODES.UNKNOWN_ERROR]!;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function parseErrorBody(data: unknown): Partial<ApiErrorBody> {
  return isRecord(data) ? (data as Partial<ApiErrorBody>) : {};
}

/** 把任意未知异常规范化为 ApiError */
export function toApiError(error: unknown): ApiError {
  if (isApiError(error)) {
    return error;
  }

  if (error instanceof AxiosError) {
    if (!error.response) {
      return new ApiError({
        code: API_ERROR_CODES.NETWORK_ERROR,
        message: error.message || 'Network request failed',
        status: 0,
      });
    }

    const body = parseErrorBody(error.response.data);
    const status = error.response.status;
    const code = body.code ?? defaultCodeForStatus(status);

    return new ApiError({
      code,
      message: body.message ?? error.message,
      status,
      traceId: body.traceId,
      details: body.details,
    });
  }

  return new ApiError({
    code: API_ERROR_CODES.UNKNOWN_ERROR,
    message: error instanceof Error ? error.message : String(error),
    status: 0,
  });
}

function defaultCodeForStatus(status: number): string {
  switch (status) {
    case 400:
      return API_ERROR_CODES.VALIDATION_FAILED;
    case 401:
      return API_ERROR_CODES.UNAUTHENTICATED;
    case 403:
      return API_ERROR_CODES.UNAUTHORIZED;
    default:
      return API_ERROR_CODES.UNKNOWN_ERROR;
  }
}

/** 判定当前用户是否拥有某个 permission（Security & RBAC Detail V1.0 §34：ADMIN 拥有全部权限） */
export function hasPermission(user: { permissions?: string[]; roles?: string[] } | null | undefined, permission: string | undefined): boolean {
  if (!permission) {
    return true; // 未声明权限要求 → 所有已登录用户可访问
  }
  if (!user) {
    return false;
  }
  if (isAdmin(user)) {
    return true;
  }
  return Boolean(user.permissions?.includes(permission));
}

export function hasAnyPermission(user: { permissions?: string[]; roles?: string[] } | null | undefined, permissions: string[] | undefined): boolean {
  if (!permissions || permissions.length === 0) {
    return true;
  }
  if (!user) {
    return false;
  }
  return permissions.some((permission) => hasPermission(user, permission));
}

export function hasAllPermissions(user: { permissions?: string[]; roles?: string[] } | null | undefined, permissions: string[] | undefined): boolean {
  if (!permissions || permissions.length === 0) {
    return true;
  }
  if (!user) {
    return false;
  }
  return permissions.every((permission) => hasPermission(user, permission));
}

export function hasAnyRole(user: { roles?: string[] } | null | undefined, roles: string[] | undefined): boolean {
  if (!roles || roles.length === 0) {
    return true;
  }
  if (!user) {
    return false;
  }
  return roles.some((role) => userHasRole(user, role));
}

export function userHasRole(user: { roles?: string[] } | null | undefined, role: RoleCode | string): boolean {
  if (!user) {
    return false;
  }
  // A role must be a real, assigned role. ADMIN is granted every *permission*
  // (see hasPermission) but it does NOT automatically satisfy an arbitrary
  // *role* requirement (Phase 0-3 review, MEDIUM-03).
  return Boolean(user.roles?.includes(role));
}

function isAdmin(user: { roles?: string[] }): boolean {
  return Boolean(user.roles?.includes('ADMIN'));
}
