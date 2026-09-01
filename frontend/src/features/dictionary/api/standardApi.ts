import { httpClient } from '@/shared/api/httpClient';
import type { StandardListParams, StandardTaskType, StandardType } from '@/shared/types/dictionary';

/**
 * 标准任务类型 / 任务类型 API。
 *
 *   GET    /api/v1/standards?search=&enabled=&type=
 *   GET    /api/v1/standards/{id}
 *   POST   /api/v1/standards
 *   PUT    /api/v1/standards/{id}
 *   PUT    /api/v1/standards/{id}/toggle-enabled
 */
const STANDARD_BASE = '/standards';

export async function listStandards(params?: StandardListParams): Promise<StandardTaskType[]> {
  const response = await httpClient.get<StandardTaskType[]>(STANDARD_BASE, { params });
  return response.data;
}

export async function getStandardById(id: string): Promise<StandardTaskType> {
  const response = await httpClient.get<StandardTaskType>(`${STANDARD_BASE}/${id}`);
  return response.data;
}

export interface StandardCreatePayload {
  code: string;
  name: string;
  type: StandardType;
  description?: string;
  enabled?: boolean;
}

export async function createStandard(payload: StandardCreatePayload): Promise<StandardTaskType> {
  const response = await httpClient.post<StandardTaskType>(STANDARD_BASE, payload);
  return response.data;
}

export interface StandardUpdatePayload {
  code?: string;
  name?: string;
  type?: StandardType;
  description?: string;
  enabled?: boolean;
}

export async function updateStandard(id: string, payload: StandardUpdatePayload): Promise<StandardTaskType> {
  const response = await httpClient.put<StandardTaskType>(`${STANDARD_BASE}/${id}`, payload);
  return response.data;
}

export async function toggleStandardEnabled(id: string): Promise<StandardTaskType> {
  const response = await httpClient.put<StandardTaskType>(`${STANDARD_BASE}/${id}/toggle-enabled`);
  return response.data;
}
