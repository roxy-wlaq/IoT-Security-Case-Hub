import { describe, expect, it, vi } from 'vitest';
import { httpClient } from '@/shared/api/httpClient';
import { createTestCase, listTestCases } from '@/features/testcase/api/testCaseApi';

vi.mock('@/shared/api/httpClient', () => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

describe('testCaseApi', () => {
  it('serializes the frozen library query parameters and endpoint', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({ data: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true } });

    await listTestCases({ q: 'bluetooth', tagIds: ['tag-1', 'tag-2'], page: 0, size: 20, sort: 'updatedAt,desc' });

    expect(httpClient.get).toHaveBeenCalledWith('/test-cases', {
      params: { q: 'bluetooth', tagIds: ['tag-1', 'tag-2'], page: 0, size: 20, sort: 'updatedAt,desc' },
    });
  });

  it('creates a Draft through the Phase 6 endpoint', async () => {
    const detail = { id: 'master-1' };
    vi.mocked(httpClient.post).mockResolvedValue({ data: detail });

    await createTestCase({ caseCode: 'BLE-001' } as never);

    expect(httpClient.post).toHaveBeenCalledWith('/test-cases', { caseCode: 'BLE-001' });
  });
});
