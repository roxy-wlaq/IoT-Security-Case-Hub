import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { UseMutationResult } from '@tanstack/react-query';
import { changePassword } from '@/features/auth/api/authApi';
import { currentUserQueryKey } from '@/features/auth/hooks/useCurrentUser';
import type { ApiError } from '@/shared/api/apiError';
import type { ChangePasswordRequest } from '@/shared/types/auth';

/** 改密成功后刷新 /me，使 mustChangePassword 变为 false */
export function useChangePassword(): UseMutationResult<void, ApiError, ChangePasswordRequest> {
  const queryClient = useQueryClient();

  return useMutation<void, ApiError, ChangePasswordRequest>({
    mutationFn: changePassword,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: currentUserQueryKey });
    },
  });
}
