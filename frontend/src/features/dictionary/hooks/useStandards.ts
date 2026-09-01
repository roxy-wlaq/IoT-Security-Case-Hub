import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { QueryClient, UseMutationResult, UseQueryResult } from '@tanstack/react-query';
import {
  createStandard,
  getStandardById,
  listStandards,
  toggleStandardEnabled,
  updateStandard,
} from '@/features/dictionary/api/standardApi';
import type { StandardCreatePayload, StandardUpdatePayload } from '@/features/dictionary/api/standardApi';
import type { ApiError } from '@/shared/api/apiError';
import type { StandardListParams, StandardTaskType } from '@/shared/types/dictionary';

export const standardsQueryKey = ['dictionary', 'standards'] as const;

export function standardListQueryKey(params?: StandardListParams): readonly unknown[] {
  return [...standardsQueryKey, 'list', params ?? {}] as const;
}

export function standardDetailQueryKey(id: string): readonly unknown[] {
  return [...standardsQueryKey, 'detail', id] as const;
}

function invalidateStandards(queryClient: QueryClient, id?: string): Promise<unknown> {
  const promises: Promise<unknown>[] = [queryClient.invalidateQueries({ queryKey: standardsQueryKey })];
  if (id) {
    promises.push(queryClient.invalidateQueries({ queryKey: standardDetailQueryKey(id) }));
  }
  return Promise.all(promises);
}

/** GET /api/v1/standards */
export function useStandards(params?: StandardListParams): UseQueryResult<StandardTaskType[], ApiError> {
  return useQuery<StandardTaskType[], ApiError>({
    queryKey: standardListQueryKey(params),
    queryFn: () => listStandards(params),
    staleTime: 30_000,
  });
}

/** GET /api/v1/standards/{id} */
export function useStandard(id: string | undefined): UseQueryResult<StandardTaskType, ApiError> {
  return useQuery<StandardTaskType, ApiError>({
    queryKey: standardDetailQueryKey(id ?? ''),
    queryFn: () => getStandardById(id as string),
    enabled: Boolean(id),
  });
}

/** POST /api/v1/standards */
export function useCreateStandard(): UseMutationResult<StandardTaskType, ApiError, StandardCreatePayload> {
  const queryClient = useQueryClient();
  return useMutation<StandardTaskType, ApiError, StandardCreatePayload>({
    mutationFn: createStandard,
    onSuccess: () => invalidateStandards(queryClient),
  });
}

/** PUT /api/v1/standards/{id} */
export function useUpdateStandard(): UseMutationResult<
  StandardTaskType,
  ApiError,
  { id: string; payload: StandardUpdatePayload }
> {
  const queryClient = useQueryClient();
  return useMutation<StandardTaskType, ApiError, { id: string; payload: StandardUpdatePayload }>({
    mutationFn: ({ id, payload }) => updateStandard(id, payload),
    onSuccess: (data) => invalidateStandards(queryClient, data.id),
  });
}

/** PUT /api/v1/standards/{id}/toggle-enabled */
export function useToggleStandardEnabled(): UseMutationResult<StandardTaskType, ApiError, string> {
  const queryClient = useQueryClient();
  return useMutation<StandardTaskType, ApiError, string>({
    mutationFn: toggleStandardEnabled,
    onSuccess: (data) => invalidateStandards(queryClient, data.id),
  });
}
