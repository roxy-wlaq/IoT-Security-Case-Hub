import axios from 'axios';
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { AxiosError } from 'axios';
import { type ApiError, toApiError } from '@/shared/api/apiError';
import { CSRF_COOKIE_NAME, CSRF_HEADER_NAME, attachCsrfHeader, ensureCsrfToken, isMutatingMethod } from '@/shared/api/csrf';

/**
 * 全局 Axios 实例。
 *
 * - withCredentials：必须开启，会话依赖 HttpOnly 的 JSESSIONID Cookie
 * - xsrfCookieName / xsrfHeaderName：XSRF-TOKEN → X-XSRF-TOKEN
 * - 响应拦截器统一把失败转换为 ApiError
 * - 401 清空当前用户缓存并跳转登录页（/auth/me 的探测性 401 除外，否则匿名访问会被无意义重定向）
 */

const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

/** 这些端点返回 401 属于"未登录"的正常探测结果，不触发全局跳转 */
const SILENT_UNAUTHORIZED_PATHS: readonly string[] = ['/auth/me', '/auth/csrf'];

export type UnauthorizedHandler = (error: ApiError) => void;

let unauthorizedHandler: UnauthorizedHandler | null = null;

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  unauthorizedHandler = handler;
}

export const httpClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  xsrfCookieName: CSRF_COOKIE_NAME,
  xsrfHeaderName: CSRF_HEADER_NAME,
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

httpClient.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  if (isMutatingMethod(config.method)) {
    await ensureCsrfToken();
  }
  return attachCsrfHeader(config);
});

httpClient.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error: AxiosError | unknown) => {
    const apiError = toApiError(error);

    if (apiError.status === 401 && unauthorizedHandler && !isSilentUnauthorizedPath(error)) {
      unauthorizedHandler(apiError);
    }

    return Promise.reject(apiError);
  },
);

function isSilentUnauthorizedPath(originalError: AxiosError | unknown): boolean {
  const url = originalError instanceof AxiosError ? originalError.config?.url : undefined;
  return typeof url === 'string' && SILENT_UNAUTHORIZED_PATHS.some((path) => url.includes(path));
}
