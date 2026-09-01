import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { QueryClient, UseMutationResult, UseQueryResult } from '@tanstack/react-query';
import { createTag, getTagById, listTags, toggleTagEnabled, updateTag } from '@/features/dictionary/api/tagApi';
import type { TagCreatePayload, TagUpdatePayload } from '@/features/dictionary/api/tagApi';
import type { ApiError } from '@/shared/api/apiError';
import type { DictionaryListParams, Tag } from '@/shared/types/dictionary';

export const tagsQueryKey = ['dictionary', 'tags'] as const;

export function tagListQueryKey(params?: DictionaryListParams): readonly unknown[] {
  return [...tagsQueryKey, 'list', params ?? {}] as const;
}

export function tagDetailQueryKey(id: string): readonly unknown[] {
  return [...tagsQueryKey, 'detail', id] as const;
}

function invalidateTags(queryClient: QueryClient, id?: string): Promise<unknown> {
  const promises: Promise<unknown>[] = [queryClient.invalidateQueries({ queryKey: tagsQueryKey })];
  if (id) {
    promises.push(queryClient.invalidateQueries({ queryKey: tagDetailQueryKey(id) }));
  }
  return Promise.all(promises);
}

/** GET /api/v1/tags */
export function useTags(params?: DictionaryListParams): UseQueryResult<Tag[], ApiError> {
  return useQuery<Tag[], ApiError>({
    queryKey: tagListQueryKey(params),
    queryFn: () => listTags(params),
    staleTime: 30_000,
  });
}

/** GET /api/v1/tags/{id} */
export function useTag(id: string | undefined): UseQueryResult<Tag, ApiError> {
  return useQuery<Tag, ApiError>({
    queryKey: tagDetailQueryKey(id ?? ''),
    queryFn: () => getTagById(id as string),
    enabled: Boolean(id),
  });
}

/** POST /api/v1/tags */
export function useCreateTag(): UseMutationResult<Tag, ApiError, TagCreatePayload> {
  const queryClient = useQueryClient();
  return useMutation<Tag, ApiError, TagCreatePayload>({
    mutationFn: createTag,
    onSuccess: () => invalidateTags(queryClient),
  });
}

/** PUT /api/v1/tags/{id} */
export function useUpdateTag(): UseMutationResult<Tag, ApiError, { id: string; payload: TagUpdatePayload }> {
  const queryClient = useQueryClient();
  return useMutation<Tag, ApiError, { id: string; payload: TagUpdatePayload }>({
    mutationFn: ({ id, payload }) => updateTag(id, payload),
    onSuccess: (data) => invalidateTags(queryClient, data.id),
  });
}

/** PUT /api/v1/tags/{id}/toggle-enabled */
export function useToggleTagEnabled(): UseMutationResult<Tag, ApiError, string> {
  const queryClient = useQueryClient();
  return useMutation<Tag, ApiError, string>({
    mutationFn: toggleTagEnabled,
    onSuccess: (data) => invalidateTags(queryClient, data.id),
  });
}
