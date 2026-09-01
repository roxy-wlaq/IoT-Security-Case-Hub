import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { UseMutationResult } from '@tanstack/react-query';
import { logout } from '@/features/auth/api/authApi';
import { currentUserQueryKey } from '@/features/auth/hooks/useCurrentUser';
import type { ApiError } from '@/shared/api/apiError';

export interface UseLogoutOptions {
  onLoggedOut?: () => void;
}

/** 登出：清空所有服务端状态缓存（避免残留上一个用户的数据），再跳转登录页 */
export function useLogout(options: UseLogoutOptions = {}): UseMutationResult<void, ApiError, void> {
  const queryClient = useQueryClient();

  return useMutation<void, ApiError, void>({
    mutationFn: logout,
    onSettled: () => {
      queryClient.setQueryData(currentUserQueryKey, null);
      queryClient.clear();
      options.onLoggedOut?.();
    },
  });
}
