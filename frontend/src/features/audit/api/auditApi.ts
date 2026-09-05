import { httpClient } from '@/shared/api/httpClient';
import type { PagedResponse } from '@/shared/types/testCase';

export const AUDIT_ACTIONS = ['LOGIN', 'LOGIN_FAILURE', 'ROLE_CHANGE', 'PROJECT_CREATE', 'PROJECT_ARCHIVE', 'TEST_CASE_PUBLISH', 'TEST_CASE_DEPRECATE', 'GENERATION_RULE_UPDATE', 'CAPABILITY_LIBRARY_UPDATE', 'EVIDENCE_DELETE'] as const;
export type AuditAction = typeof AUDIT_ACTIONS[number];
export interface AuditLog {
  id: string;
  occurredAt: string;
  action: AuditAction;
  actorId?: string | null;
  actorUsername: string;
  resourceType: string;
  resourceId?: string | null;
  resourceLabel?: string | null;
  detail?: Record<string, unknown> | null;
}
export interface AuditLogFilters { page?: number; size?: number; action?: AuditAction; resourceType?: string; resourceId?: string; actorUsername?: string }

export async function listAuditLogs(filters: AuditLogFilters = {}): Promise<PagedResponse<AuditLog>> {
  return (await httpClient.get<PagedResponse<AuditLog>>('/audit-logs', { params: filters })).data;
}
