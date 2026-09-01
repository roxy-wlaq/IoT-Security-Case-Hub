import { httpClient } from '@/shared/api/httpClient';
import type { DictionaryListParams, Tag } from '@/shared/types/dictionary';

/**
 * 标签 API。
 *
 *   GET    /api/v1/tags?search=&enabled=
 *   GET    /api/v1/tags/{id}
 *   POST   /api/v1/tags
 *   PUT    /api/v1/tags/{id}
 *   PUT    /api/v1/tags/{id}/toggle-enabled
 */
const TAG_BASE = '/tags';

export async function listTags(params?: DictionaryListParams): Promise<Tag[]> {
  const response = await httpClient.get<Tag[]>(TAG_BASE, { params });
  return response.data;
}

export async function getTagById(id: string): Promise<Tag> {
  const response = await httpClient.get<Tag>(`${TAG_BASE}/${id}`);
  return response.data;
}

export interface TagCreatePayload {
  name: string;
  description?: string;
  enabled?: boolean;
}

export async function createTag(payload: TagCreatePayload): Promise<Tag> {
  const response = await httpClient.post<Tag>(TAG_BASE, payload);
  return response.data;
}

export interface TagUpdatePayload {
  name?: string;
  description?: string;
  enabled?: boolean;
}

export async function updateTag(id: string, payload: TagUpdatePayload): Promise<Tag> {
  const response = await httpClient.put<Tag>(`${TAG_BASE}/${id}`, payload);
  return response.data;
}

export async function toggleTagEnabled(id: string): Promise<Tag> {
  const response = await httpClient.put<Tag>(`${TAG_BASE}/${id}/toggle-enabled`);
  return response.data;
}
