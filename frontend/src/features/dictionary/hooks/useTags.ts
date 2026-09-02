import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { UseMutationResult, UseQueryResult } from '@tanstack/react-query';
import { createTag, listTags, updateTag } from '@/features/dictionary/api/tagApi';
import type { TagCreatePayload, TagUpdatePayload } from '@/features/dictionary/api/tagApi';
import type { ApiError } from '@/shared/api/apiError';
import type { DictionaryListParams, Tag } from '@/shared/types/dictionary';

export const tagsQueryKey = ['dictionary', 'tags'] as const;

export function tagListQueryKey(params?: DictionaryListParams): readonly unknown[] {
  return [...tagsQueryKey, 'list', params ?? {}] as const;
}

/** GET /api/v1/tags */
export function useTags(params?: DictionaryListParams): UseQueryResult<Tag[], ApiError> {
  return useQuery<Tag[], ApiError>({
    queryKey: tagListQueryKey(params),
    queryFn: () => listTags(params),
    staleTime: 30_000,
  });
}

/** POST /api/v1/tags */
export function useCreateTag(): UseMutationResult<Tag, ApiError, TagCreatePayload> {
  const queryClient = useQueryClient();
  return useMutation<Tag, ApiError, TagCreatePayload>({
    mutationFn: createTag,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: tagsQueryKey }),
  });
}

/** PUT /api/v1/tags/{id} */
export function useUpdateTag(): UseMutationResult<Tag, ApiError, { id: string; payload: TagUpdatePayload }> {
  const queryClient = useQueryClient();
  return useMutation<Tag, ApiError, { id: string; payload: TagUpdatePayload }>({
    mutationFn: ({ id, payload }) => updateTag(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: tagsQueryKey }),
  });
}
