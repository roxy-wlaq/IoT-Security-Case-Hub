import { describe, expect, it, vi } from 'vitest';
import { httpClient } from '@/shared/api/httpClient';
import { listAuditLogs } from '@/features/audit/api/auditApi';

vi.mock('@/shared/api/httpClient', () => ({ httpClient: { get: vi.fn() } }));

describe('auditApi', () => {
  it('passes action, resource, and actor filters to the read-only endpoint', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({ data: { content: [] } } as never);

    await listAuditLogs({ action: 'PROJECT_CREATE', resourceType: 'PROJECT', resourceId: 'p-1', actorUsername: 'admin' });

    expect(httpClient.get).toHaveBeenCalledWith('/audit-logs', {
      params: { action: 'PROJECT_CREATE', resourceType: 'PROJECT', resourceId: 'p-1', actorUsername: 'admin' },
    });
  });
});
