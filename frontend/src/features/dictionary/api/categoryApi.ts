import { httpClient } from '@/shared/api/httpClient';
import type { Category, CategoryLevel, DictionaryListParams } from '@/shared/types/dictionary';

/**
 * 分类 API（list 返回二级树）。
 *
 *   GET    /api/v1/categories?search=&enabled=
 *   GET    /api/v1/categories/{id}
 *   POST   /api/v1/categories
 *   PUT    /api/v1/categories/{id}
 *   PUT    /api/v1/categories/{id}/toggle-enabled
 */
const CATEGORY_BASE = '/categories';

export async function listCategories(params?: DictionaryListParams): Promise<Category[]> {
  const response = await httpClient.get<Category[]>(CATEGORY_BASE, { params });
  return response.data;
}

export async function getCategoryById(id: string): Promise<Category> {
  const response = await httpClient.get<Category>(`${CATEGORY_BASE}/${id}`);
  return response.data;
}

export interface CategoryCreatePayload {
  parentId?: string | null;
  code: string;
  name: string;
  level: CategoryLevel;
  description?: string;
  sortOrder?: number;
  enabled?: boolean;
}

export async function createCategory(payload: CategoryCreatePayload): Promise<Category> {
  const response = await httpClient.post<Category>(CATEGORY_BASE, payload);
  return response.data;
}

export interface CategoryUpdatePayload {
  parentId?: string | null;
  code?: string;
  name?: string;
  level?: CategoryLevel;
  description?: string;
  sortOrder?: number;
  enabled?: boolean;
}

export async function updateCategory(id: string, payload: CategoryUpdatePayload): Promise<Category> {
  const response = await httpClient.put<Category>(`${CATEGORY_BASE}/${id}`, payload);
  return response.data;
}

export async function toggleCategoryEnabled(id: string): Promise<Category> {
  const response = await httpClient.put<Category>(`${CATEGORY_BASE}/${id}/toggle-enabled`);
  return response.data;
}
