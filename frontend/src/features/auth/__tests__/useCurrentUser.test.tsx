import { describe, it, expect, vi } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { ApiError } from '@/shared/api/apiError';
import * as authApi from '@/features/auth/api/authApi';

vi.mock('@/features/auth/api/authApi');

function makeWrapper(queryClient: QueryClient) {
  return ({ children }: { children?: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

describe('useCurrentUser', () => {
  it('returns null when /me responds 401 (swallowed, not treated as an error)', async () => {
    vi.mocked(authApi.fetchCurrentUser).mockRejectedValue(
      new ApiError({ code: 'AUTH_UNAUTHENTICATED', message: 'unauthenticated', status: 401 }),
    );
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const { result } = renderHook(() => useCurrentUser(), { wrapper: makeWrapper(queryClient) });

    await waitFor(() => {
      expect(result.current.data).toBeNull();
    });
    expect(result.current.isError).toBe(false);
  });
});
