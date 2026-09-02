import { httpClient } from '@/shared/api/httpClient';
import type { Category, DictionaryListParams } from '@/shared/types/dictionary';

/**
 * 分类 API（冻结契约）。level 由服务端从 parentId 推导，客户端永远不传 level。
 *
 *   GET    /api/v1/categories/tree?search=&enabled=
 *   POST   /api/v1/categories
 *   PUT    /api/v1/categories/{id}
 */
const CATEGORY_BASE = '/categories';

/** GET /api/v1/categories/tree（返回两级树） */
export async function listCategoryTree(params?: DictionaryListParams): Promise<Category[]> {
  const response = await httpClient.get<Category[]>(`${CATEGORY_BASE}/tree`, { params });
  return response.data;
}

export interface CategoryCreatePayload {
  /** null 表示创建一级分类；一级分类 id 表示创建其下的二级分类 */
  parentId?: string | null;
  code: string;
  name: string;
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
  description?: string;
  sortOrder?: number;
  enabled?: boolean;
}

export async function updateCategory(id: string, payload: CategoryUpdatePayload): Promise<Category> {
  const response = await httpClient.put<Category>(`${CATEGORY_BASE}/${id}`, payload);
  return response.data;
}
