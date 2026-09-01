import { describe, it, expect, vi } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useLogin } from '@/features/auth/hooks/useLogin';
import { currentUserQueryKey } from '@/features/auth/hooks/useCurrentUser';
import * as authApi from '@/features/auth/api/authApi';
import type { CurrentUser } from '@/shared/types/auth';

vi.mock('@/features/auth/api/authApi');

const loggedInUser: CurrentUser = {
  id: '1',
  username: 'u',
  displayName: 'U',
  enabled: true,
  mustChangePassword: false,
  roles: ['ADMIN'],
  permissions: ['user:read'],
};

describe('useLogin', () => {
  it('updates the current-user cache on successful login', async () => {
    vi.mocked(authApi.login).mockResolvedValue(loggedInUser);
    vi.mocked(authApi.fetchCurrentUser).mockResolvedValue(loggedInUser);

    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const wrapper = ({ children }: { children?: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => useLogin(), { wrapper });
    result.current.mutate({ username: 'u', password: 'p' });

    await waitFor(() => {
      expect(queryClient.getQueryData(currentUserQueryKey)).toEqual(loggedInUser);
    });
  });
});
