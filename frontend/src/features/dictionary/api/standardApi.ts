import { httpClient } from '@/shared/api/httpClient';
import type { StandardListParams, StandardTaskType, StandardType } from '@/shared/types/dictionary';

/**
 * 标准任务类型 / 任务类型 API（冻结契约）。
 *
 *   GET    /api/v1/standard-task-types?search=&enabled=&type=
 *   POST   /api/v1/standard-task-types
 *   PUT    /api/v1/standard-task-types/{id}
 */
const STANDARD_BASE = '/standard-task-types';

export async function listStandards(params?: StandardListParams): Promise<StandardTaskType[]> {
  const response = await httpClient.get<StandardTaskType[]>(STANDARD_BASE, { params });
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
