import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { UseMutationResult, UseQueryResult } from '@tanstack/react-query';
import { createCategory, listCategoryTree, updateCategory } from '@/features/dictionary/api/categoryApi';
import type { CategoryCreatePayload, CategoryUpdatePayload } from '@/features/dictionary/api/categoryApi';
import type { ApiError } from '@/shared/api/apiError';
import type { Category, DictionaryListParams } from '@/shared/types/dictionary';

export const categoriesQueryKey = ['dictionary', 'categories'] as const;

export function categoryListQueryKey(params?: DictionaryListParams): readonly unknown[] {
  return [...categoriesQueryKey, 'tree', params ?? {}] as const;
}

/** GET /api/v1/categories/tree */
export function useCategories(params?: DictionaryListParams): UseQueryResult<Category[], ApiError> {
  return useQuery<Category[], ApiError>({
    queryKey: categoryListQueryKey(params),
    queryFn: () => listCategoryTree(params),
    staleTime: 30_000,
  });
}

/** POST /api/v1/categories */
export function useCreateCategory(): UseMutationResult<Category, ApiError, CategoryCreatePayload> {
  const queryClient = useQueryClient();
  return useMutation<Category, ApiError, CategoryCreatePayload>({
    mutationFn: createCategory,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: categoriesQueryKey }),
  });
}

/** PUT /api/v1/categories/{id} */
export function useUpdateCategory(): UseMutationResult<Category, ApiError, { id: string; payload: CategoryUpdatePayload }> {
  const queryClient = useQueryClient();
  return useMutation<Category, ApiError, { id: string; payload: CategoryUpdatePayload }>({
    mutationFn: ({ id, payload }) => updateCategory(id, payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: categoriesQueryKey }),
  });
}
