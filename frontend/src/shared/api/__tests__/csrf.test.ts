import { describe, it, expect } from 'vitest';
import { isMutatingMethod, attachCsrfHeader } from '@/shared/api/csrf';
import { AxiosHeaders } from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';

function makeConfig(): InternalAxiosRequestConfig {
  return { method: 'post', headers: new AxiosHeaders() } as unknown as InternalAxiosRequestConfig;
}

describe('isMutatingMethod', () => {
  it('is true for post/put/patch/delete (case-insensitive)', () => {
    expect(isMutatingMethod('POST')).toBe(true);
    expect(isMutatingMethod('put')).toBe(true);
    expect(isMutatingMethod('PATCH')).toBe(true);
    expect(isMutatingMethod('delete')).toBe(true);
  });

  it('is false for safe methods', () => {
    expect(isMutatingMethod('get')).toBe(false);
    expect(isMutatingMethod('head')).toBe(false);
    expect(isMutatingMethod(undefined)).toBe(false);
  });
});

describe('attachCsrfHeader', () => {
  it('adds X-XSRF-TOKEN when the CSRF cookie is present', () => {
    document.cookie = 'XSRF-TOKEN=abc123';
    const result = attachCsrfHeader(makeConfig());
    expect(result.headers.get('X-XSRF-TOKEN')).toBe('abc123');
  });

  it('leaves the header unset when no CSRF cookie is present', () => {
    document.cookie = 'XSRF-TOKEN=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
    const result = attachCsrfHeader(makeConfig());
    expect(result.headers.get('X-XSRF-TOKEN')).toBeUndefined();
  });
});
