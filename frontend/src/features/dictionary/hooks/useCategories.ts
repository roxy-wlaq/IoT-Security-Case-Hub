import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { QueryClient, UseMutationResult, UseQueryResult } from '@tanstack/react-query';
import {
  createCategory,
  getCategoryById,
  listCategories,
  toggleCategoryEnabled,
  updateCategory,
} from '@/features/dictionary/api/categoryApi';
import type { CategoryCreatePayload, CategoryUpdatePayload } from '@/features/dictionary/api/categoryApi';
import type { ApiError } from '@/shared/api/apiError';
import type { Category, DictionaryListParams } from '@/shared/types/dictionary';

export const categoriesQueryKey = ['dictionary', 'categories'] as const;

export function categoryListQueryKey(params?: DictionaryListParams): readonly unknown[] {
  return [...categoriesQueryKey, 'list', params ?? {}] as const;
}

export function categoryDetailQueryKey(id: string): readonly unknown[] {
  return [...categoriesQueryKey, 'detail', id] as const;
}

function invalidateCategories(queryClient: QueryClient, id?: string): Promise<unknown> {
  const promises: Promise<unknown>[] = [queryClient.invalidateQueries({ queryKey: categoriesQueryKey })];
  if (id) {
    promises.push(queryClient.invalidateQueries({ queryKey: categoryDetailQueryKey(id) }));
  }
  return Promise.all(promises);
}

/** GET /api/v1/categories */
export function useCategories(params?: DictionaryListParams): UseQueryResult<Category[], ApiError> {
  return useQuery<Category[], ApiError>({
    queryKey: categoryListQueryKey(params),
    queryFn: () => listCategories(params),
    staleTime: 30_000,
  });
}

/** GET /api/v1/categories/{id} */
export function useCategory(id: string | undefined): UseQueryResult<Category, ApiError> {
  return useQuery<Category, ApiError>({
    queryKey: categoryDetailQueryKey(id ?? ''),
    queryFn: () => getCategoryById(id as string),
    enabled: Boolean(id),
  });
}

/** POST /api/v1/categories */
export function useCreateCategory(): UseMutationResult<Category, ApiError, CategoryCreatePayload> {
  const queryClient = useQueryClient();
  return useMutation<Category, ApiError, CategoryCreatePayload>({
    mutationFn: createCategory,
    onSuccess: () => invalidateCategories(queryClient),
  });
}

/** PUT /api/v1/categories/{id} */
export function useUpdateCategory(): UseMutationResult<Category, ApiError, { id: string; payload: CategoryUpdatePayload }> {
  const queryClient = useQueryClient();
  return useMutation<Category, ApiError, { id: string; payload: CategoryUpdatePayload }>({
    mutationFn: ({ id, payload }) => updateCategory(id, payload),
    onSuccess: (data) => invalidateCategories(queryClient, data.id),
  });
}

/** PUT /api/v1/categories/{id}/toggle-enabled */
export function useToggleCategoryEnabled(): UseMutationResult<Category, ApiError, string> {
  const queryClient = useQueryClient();
  return useMutation<Category, ApiError, string>({
    mutationFn: toggleCategoryEnabled,
    onSuccess: (data) => invalidateCategories(queryClient, data.id),
  });
}
