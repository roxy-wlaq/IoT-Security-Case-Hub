import { httpClient } from '@/shared/api/httpClient';
import type { DictionaryListParams, Tag } from '@/shared/types/dictionary';

/**
 * 标签 API（冻结契约）。
 *
 *   GET    /api/v1/tags?search=&enabled=
 *   POST   /api/v1/tags
 *   PUT    /api/v1/tags/{id}
 */
const TAG_BASE = '/tags';

export async function listTags(params?: DictionaryListParams): Promise<Tag[]> {
  const response = await httpClient.get<Tag[]>(TAG_BASE, { params });
  return response.data;
}

export interface TagCreatePayload {
  code: string;
  name: string;
  description?: string;
  enabled?: boolean;
}

export async function createTag(payload: TagCreatePayload): Promise<Tag> {
  const response = await httpClient.post<Tag>(TAG_BASE, payload);
  return response.data;
}

export interface TagUpdatePayload {
  code?: string;
  name?: string;
  description?: string;
  enabled?: boolean;
}

export async function updateTag(id: string, payload: TagUpdatePayload): Promise<Tag> {
  const response = await httpClient.put<Tag>(`${TAG_BASE}/${id}`, payload);
  return response.data;
}
