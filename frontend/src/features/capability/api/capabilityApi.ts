import { httpClient } from '@/shared/api/httpClient';
import type {
  Capability,
  CapabilityCreatePayload,
  CapabilityTreeNode,
  CapabilityUpdatePayload,
} from '@/shared/types/capability';

/**
 * 能力库 API。
 *
 *   GET  /api/v1/capabilities/tree
 *   POST /api/v1/capabilities                      （capability:manage_library）
 *   PUT  /api/v1/capabilities/{id}                 （capability:manage_library）
 *   POST /api/v1/capabilities/{id}/enable          （capability:manage_library）
 *   POST /api/v1/capabilities/{id}/disable         （capability:manage_library）
 */
const CAPABILITY_BASE = '/capabilities';

/** GET /api/v1/capabilities/tree */
export async function fetchCapabilityTree(): Promise<CapabilityTreeNode[]> {
  const response = await httpClient.get<CapabilityTreeNode[]>(`${CAPABILITY_BASE}/tree`);
  return response.data;
}

/** POST /api/v1/capabilities */
export async function createCapability(payload: CapabilityCreatePayload): Promise<Capability> {
  const response = await httpClient.post<Capability>(CAPABILITY_BASE, payload);
  return response.data;
}

/** PUT /api/v1/capabilities/{id} */
export async function updateCapability(id: string, payload: CapabilityUpdatePayload): Promise<Capability> {
  const response = await httpClient.put<Capability>(`${CAPABILITY_BASE}/${id}`, payload);
  return response.data;
}

/** POST /api/v1/capabilities/{id}/enable */
export async function enableCapability(id: string): Promise<Capability> {
  const response = await httpClient.post<Capability>(`${CAPABILITY_BASE}/${id}/enable`);
  return response.data;
}

/** POST /api/v1/capabilities/{id}/disable —— 退役，从不物理删除 */
export async function disableCapability(id: string): Promise<Capability> {
  const response = await httpClient.post<Capability>(`${CAPABILITY_BASE}/${id}/disable`);
  return response.data;
}
