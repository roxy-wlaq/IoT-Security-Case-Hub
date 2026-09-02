import { httpClient } from '@/shared/api/httpClient';
import type { DictionaryListParams, Tool } from '@/shared/types/dictionary';

/**
 * 工具 API（冻结契约）。
 *
 *   GET    /api/v1/tools?search=&enabled=
 *   GET    /api/v1/tools/{id}
 *   POST   /api/v1/tools
 *   PUT    /api/v1/tools/{id}
 */
const TOOL_BASE = '/tools';

export async function listTools(params?: DictionaryListParams): Promise<Tool[]> {
  const response = await httpClient.get<Tool[]>(TOOL_BASE, { params });
  return response.data;
}

export async function getToolById(id: string): Promise<Tool> {
  const response = await httpClient.get<Tool>(`${TOOL_BASE}/${id}`);
  return response.data;
}

export interface ToolCreatePayload {
  code: string;
  name: string;
  description?: string;
  platform?: string;
  website?: string;
  enabled?: boolean;
}

export async function createTool(payload: ToolCreatePayload): Promise<Tool> {
  const response = await httpClient.post<Tool>(TOOL_BASE, payload);
  return response.data;
}

export interface ToolUpdatePayload {
  code?: string;
  name?: string;
  description?: string;
  platform?: string;
  website?: string;
  enabled?: boolean;
}

export async function updateTool(id: string, payload: ToolUpdatePayload): Promise<Tool> {
  const response = await httpClient.put<Tool>(`${TOOL_BASE}/${id}`, payload);
  return response.data;
}
