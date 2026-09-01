import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { UseMutationResult } from '@tanstack/react-query';
import { login } from '@/features/auth/api/authApi';
import { currentUserQueryKey } from '@/features/auth/hooks/useCurrentUser';
import type { ApiError } from '@/shared/api/apiError';
import type { CurrentUser, LoginRequest } from '@/shared/types/auth';

/** 登录成功后写入缓存并使 /me 重新拉取（Session Cookie 已建立） */
export function useLogin(): UseMutationResult<CurrentUser, ApiError, LoginRequest> {
  const queryClient = useQueryClient();

  return useMutation<CurrentUser, ApiError, LoginRequest>({
    mutationFn: login,
    onSuccess: (user) => {
      queryClient.setQueryData(currentUserQueryKey, user);
      void queryClient.invalidateQueries({ queryKey: currentUserQueryKey });
    },
  });
}
