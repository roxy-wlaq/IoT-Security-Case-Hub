import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { UseMutationResult, UseQueryResult } from '@tanstack/react-query';
import { createStandard, listStandards, updateStandard } from '@/features/dictionary/api/standardApi';
import type { StandardCreatePayload, StandardUpdatePayload } from '@/features/dictionary/api/standardApi';
import type { ApiError } from '@/shared/api/apiError';
import type { StandardListParams, StandardTaskType } from '@/shared/types/dictionary';

export const standardsQueryKey = ['dictionary', 'standards'] as const;

export function standardListQueryKey(params?: StandardListParams): readonly unknown[] {
  return [...standardsQueryKey, 'list', params ?? {}] as const;
}

/** GET /api/v1/standard-task-types */
export function useStandards(params?: StandardListParams): UseQueryResult<StandardTaskType[], ApiError> {
  return useQuery<StandardTaskType[], ApiError>({
    queryKey: standardListQueryKey(params),
    queryFn: () => listStandards(params),
    staleTime: 30_000,
  });
}

/** POST /api/v1/standard-task-types */
export function useCreateStandard(): UseMutationResult<StandardTaskType, ApiError, StandardCreatePayload> {
  const queryClient = useQueryClient();
  return useMutation<StandardTaskType, ApiError, StandardCreatePayload>({
    mutationFn: createStandard,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: standardsQueryKey }),
  });
}

/** PUT /api/v1/standard-task-types/{id} */
export function useUpdateStandard(): UseMutationResult<
  StandardTaskType,
  ApiError,
  { id: string; payload: StandardUpdatePayload }
> {
  const queryClient = useQueryClient();
  return useMutation<StandardTaskType, ApiError, { id: string; payload: StandardUpdatePayload }>({
    mutationFn: ({ id, payload }) => updateStandard(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: standardsQueryKey }),
  });
}
