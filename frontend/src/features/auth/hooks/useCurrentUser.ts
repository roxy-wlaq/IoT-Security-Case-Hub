import { useQuery } from '@tanstack/react-query';
import type { UseQueryResult } from '@tanstack/react-query';
import { fetchCurrentUser } from '@/features/auth/api/authApi';
import { isApiError } from '@/shared/api/apiError';
import type { CurrentUser } from '@/shared/types/auth';

export const currentUserQueryKey = ['auth', 'currentUser'] as const;

/**
 * Current User bootstrap。
 *
 * - `GET /api/v1/auth/me`
 * - 401 视为"未登录"，返回 null 而不是抛错
 * - retry: false，避免未登录时对 /me 反复重试
 */
export function useCurrentUser(): UseQueryResult<CurrentUser | null> {
  return useQuery<CurrentUser | null>({
    queryKey: currentUserQueryKey,
    queryFn: async () => {
      try {
        return await fetchCurrentUser();
      } catch (error) {
        if (isApiError(error) && error.status === 401) {
          return null;
        }
        throw error;
      }
    },
    retry: false,
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
}
