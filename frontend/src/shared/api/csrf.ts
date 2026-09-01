import type { InternalAxiosRequestConfig } from 'axios';

/**
 * CSRF 支持（Security & RBAC Detail V1.0 §11-§15）：
 *   - Spring Security CookieCsrfTokenRepository 下发 `XSRF-TOKEN`（HttpOnly = false）
 *   - 前端状态修改请求通过 `X-XSRF-TOKEN` Header 回传
 *   - 会话完全依赖 HttpOnly 的 JSESSIONID Cookie，禁止使用 JWT / LocalStorage
 *
 * 本模块刻意不 import axios 实例，避免与 httpClient.ts 形成循环依赖；
 * bootstrap 使用原生 fetch（credentials: 'include'）以确保 Cookie 落盘。
 */

export const CSRF_COOKIE_NAME = 'XSRF-TOKEN';
export const CSRF_HEADER_NAME = 'X-XSRF-TOKEN';

const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

const MUTATING_METHODS: readonly string[] = ['post', 'put', 'patch', 'delete'];

let inFlight: Promise<void> | null = null;

export function readCookie(name: string): string | null {
  if (typeof document === 'undefined') {
    return null;
  }
  const prefix = `${encodeURIComponent(name)}=`;
  const match = document.cookie
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith(prefix));

  return match ? decodeURIComponent(match.slice(prefix.length)) : null;
}

export function hasCsrfCookie(): boolean {
  return readCookie(CSRF_COOKIE_NAME) !== null;
}

export function isMutatingMethod(method: string | undefined): boolean {
  return MUTATING_METHODS.includes((method ?? 'get').toLowerCase());
}

/** 把 Cookie 中的 token 注入请求头；Cookie 不存在时保持原样（由 ensureCsrfToken 负责补取） */
export function attachCsrfHeader<T extends InternalAxiosRequestConfig>(config: T): T {
  const token = readCookie(CSRF_COOKIE_NAME);
  if (token && config.headers) {
    config.headers.set(CSRF_HEADER_NAME, token);
  }
  return config;
}

/** 应用启动时调用一次，强制 Spring Security 创建 CSRF Token Cookie */
export function bootstrapCsrf(): Promise<void> {
  if (!inFlight) {
    inFlight = requestCsrfToken().catch((error: unknown) => {
      inFlight = null;
      throw error;
    });
  }
  return inFlight;
}

/** 写操作前的兜底：Cookie 缺失时补取一次 */
export async function ensureCsrfToken(): Promise<void> {
  if (hasCsrfCookie()) {
    return;
  }
  await bootstrapCsrf();
}

/** 测试用：重置 bootstrap 状态 */
export function resetCsrfState(): void {
  inFlight = null;
}

async function requestCsrfToken(): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/auth/csrf`, {
    method: 'GET',
    credentials: 'include',
    headers: { Accept: 'application/json' },
  });

  if (!response.ok) {
    throw new Error(`CSRF bootstrap failed with HTTP ${response.status}`);
  }
}
