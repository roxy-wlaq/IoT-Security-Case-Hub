import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { QueryClient, UseMutationResult, UseQueryResult } from '@tanstack/react-query';
import { createTool, getToolById, listTools, toggleToolEnabled, updateTool } from '@/features/dictionary/api/toolApi';
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

function invalidateTools(queryClient: QueryClient, id?: string): Promise<unknown> {
  const promises: Promise<unknown>[] = [queryClient.invalidateQueries({ queryKey: toolsQueryKey })];
  if (id) {
    promises.push(queryClient.invalidateQueries({ queryKey: toolDetailQueryKey(id) }));
  }
  return Promise.all(promises);
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
    onSuccess: () => invalidateTools(queryClient),
  });
}

/** PUT /api/v1/tools/{id} */
export function useUpdateTool(): UseMutationResult<Tool, ApiError, { id: string; payload: ToolUpdatePayload }> {
  const queryClient = useQueryClient();
  return useMutation<Tool, ApiError, { id: string; payload: ToolUpdatePayload }>({
    mutationFn: ({ id, payload }) => updateTool(id, payload),
    onSuccess: (data) => invalidateTools(queryClient, data.id),
  });
}

/** PUT /api/v1/tools/{id}/toggle-enabled */
export function useToggleToolEnabled(): UseMutationResult<Tool, ApiError, string> {
  const queryClient = useQueryClient();
  return useMutation<Tool, ApiError, string>({
    mutationFn: toggleToolEnabled,
    onSuccess: (data) => invalidateTools(queryClient, data.id),
  });
}
