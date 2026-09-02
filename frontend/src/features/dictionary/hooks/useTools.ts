import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { UseMutationResult, UseQueryResult } from '@tanstack/react-query';
import { createTool, getToolById, listTools, updateTool } from '@/features/dictionary/api/toolApi';
import type { ToolCreatePayload, ToolUpdatePayload } from '@/features/dictionary/api/toolApi';
import type { ApiError } from '@/shared/api/apiError';
import type { DictionaryListParams, Tool } from '@/shared/types/dictionary';

export const toolsQueryKey = ['dictionary', 'tools'] as const;

export function toolListQueryKey(params?: DictionaryListParams): readonly unknown[] {
  return [...toolsQueryKey, 'list', params ?? {}] as const;
}

export function toolDetailQueryKey(id: string): readonly unknown[] {
  return [...toolsQueryKey, 'detail', id] as const;
}

/** GET /api/v1/tools */
export function useTools(params?: DictionaryListParams): UseQueryResult<Tool[], ApiError> {
  return useQuery<Tool[], ApiError>({
    queryKey: toolListQueryKey(params),
    queryFn: () => listTools(params),
    staleTime: 30_000,
  });
}

/** GET /api/v1/tools/{id} */
export function useTool(id: string | undefined): UseQueryResult<Tool, ApiError> {
  return useQuery<Tool, ApiError>({
    queryKey: toolDetailQueryKey(id ?? ''),
    queryFn: () => getToolById(id as string),
    enabled: Boolean(id),
  });
}

/** POST /api/v1/tools */
export function useCreateTool(): UseMutationResult<Tool, ApiError, ToolCreatePayload> {
  const queryClient = useQueryClient();
  return useMutation<Tool, ApiError, ToolCreatePayload>({
    mutationFn: createTool,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: toolsQueryKey }),
  });
}

/** PUT /api/v1/tools/{id} */
export function useUpdateTool(): UseMutationResult<Tool, ApiError, { id: string; payload: ToolUpdatePayload }> {
  const queryClient = useQueryClient();
  return useMutation<Tool, ApiError, { id: string; payload: ToolUpdatePayload }>({
    mutationFn: ({ id, payload }) => updateTool(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: toolsQueryKey }),
  });
}
