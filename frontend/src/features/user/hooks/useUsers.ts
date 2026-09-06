import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { UseMutationResult, UseQueryResult } from '@tanstack/react-query';
import type { PagedResponse } from '@/shared/types/testCase';
import {
  createUser,
  disableUser,
  enableUser,
  listRoles,
  listUsers,
  resetUserPassword,
  updateUser,
  updateUserRoles,
} from '@/features/user/api/userApi';
import type {
  PasswordResetResult,
  Role,
  UserCreatePayload,
  UserListParams,
  UserSummary,
} from '@/features/user/api/userApi';
import type { ApiError } from '@/shared/api/apiError';

export const usersQueryKey = ['admin', 'users'] as const;

export function userListQueryKey(params?: UserListParams): readonly unknown[] {
  return [...usersQueryKey, 'list', params ?? {}] as const;
}

export const rolesQueryKey = ['admin', 'roles'] as const;

/** GET /api/v1/users */
export function useUsers(params?: UserListParams): UseQueryResult<PagedResponse<UserSummary>, ApiError> {
  return useQuery<PagedResponse<UserSummary>, ApiError>({
    queryKey: userListQueryKey(params),
    queryFn: () => listUsers(params),
    placeholderData: (previous) => previous,
  });
}

/** GET /api/v1/roles */
export function useRoles(enabled = true): UseQueryResult<Role[], ApiError> {
  return useQuery<Role[], ApiError>({
    queryKey: rolesQueryKey,
    queryFn: listRoles,
    enabled,
    staleTime: 5 * 60_000,
  });
}

function useInvalidateUsers() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: usersQueryKey });
}

/** POST /api/v1/users */
export function useCreateUser(): UseMutationResult<
  UserSummary & { generatedPassword: string | null },
  ApiError,
  UserCreatePayload
> {
  const invalidate = useInvalidateUsers();
  return useMutation<UserSummary & { generatedPassword: string | null }, ApiError, UserCreatePayload>({
    mutationFn: (payload) => createUser(payload),
    onSuccess: () => invalidate(),
  });
}

/** PUT /api/v1/users/{id} */
export function useUpdateUser(): UseMutationResult<
  UserSummary,
  ApiError,
  { id: string; displayName: string }
> {
  const invalidate = useInvalidateUsers();
  return useMutation<UserSummary, ApiError, { id: string; displayName: string }>({
    mutationFn: ({ id, displayName }) => updateUser(id, { displayName }),
    onSuccess: () => invalidate(),
  });
}

/** PUT /api/v1/users/{id}/roles */
export function useUpdateUserRoles(): UseMutationResult<
  UserSummary,
  ApiError,
  { id: string; roles: string[] }
> {
  const invalidate = useInvalidateUsers();
  return useMutation<UserSummary, ApiError, { id: string; roles: string[] }>({
    mutationFn: ({ id, roles }) => updateUserRoles(id, roles),
    onSuccess: () => invalidate(),
  });
}

/** POST /api/v1/users/{id}/enable */
export function useEnableUser(): UseMutationResult<UserSummary, ApiError, string> {
  const invalidate = useInvalidateUsers();
  return useMutation<UserSummary, ApiError, string>({
    mutationFn: (id) => enableUser(id),
    onSuccess: () => invalidate(),
  });
}

/** POST /api/v1/users/{id}/disable */
export function useDisableUser(): UseMutationResult<UserSummary, ApiError, string> {
  const invalidate = useInvalidateUsers();
  return useMutation<UserSummary, ApiError, string>({
    mutationFn: (id) => disableUser(id),
    onSuccess: () => invalidate(),
  });
}

/** POST /api/v1/users/{id}/password-reset */
export function useResetUserPassword(): UseMutationResult<
  PasswordResetResult,
  ApiError,
  { id: string; password?: string }
> {
  const invalidate = useInvalidateUsers();
  return useMutation<PasswordResetResult, ApiError, { id: string; password?: string }>({
    mutationFn: ({ id, password }) => resetUserPassword(id, password),
    onSuccess: () => invalidate(),
  });
}
