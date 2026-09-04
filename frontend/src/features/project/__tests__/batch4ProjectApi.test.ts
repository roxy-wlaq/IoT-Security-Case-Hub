import { describe, expect, it, vi } from 'vitest';
import { httpClient } from '@/shared/api/httpClient';
import { getVersionAvailability, listCustomTestCases, reviewCapabilityUpdateRequest, submitTestCaseChangeRequest, upgradeProjectTestCaseVersion } from '@/features/project/api/projectApi';

vi.mock('@/shared/api/httpClient', () => ({ httpClient: { get: vi.fn(), post: vi.fn(), put: vi.fn() } }));
const mocked = httpClient as unknown as { get: ReturnType<typeof vi.fn>; post: ReturnType<typeof vi.fn> };

describe('Batch 4 project APIs', () => {
  it('loads project-scoped custom cases', async () => { mocked.get.mockResolvedValue({ data: [] }); await listCustomTestCases('project-1'); expect(mocked.get).toHaveBeenCalledWith('/projects/project-1/custom-test-cases'); });
  it('submits a source-version-bound change request', async () => { mocked.post.mockResolvedValue({ data: { status: 'PENDING' } }); await submitTestCaseChangeRequest('master-1', { sourceVersionId: 'version-1', reason: 'clarify step' }); expect(mocked.post).toHaveBeenCalledWith('/test-cases/master-1/change-requests', { sourceVersionId: 'version-1', reason: 'clarify step' }); });
  it('reviews capability requests and preserves approval path', async () => { mocked.post.mockResolvedValue({ data: { status: 'APPROVED' } }); await reviewCapabilityUpdateRequest('project-1', 'request-1', true, 'verified'); expect(mocked.post).toHaveBeenCalledWith('/projects/project-1/capability-update-requests/request-1/approve', { comment: 'verified' }); });
  it('reads availability before upgrading the same PTC', async () => { mocked.get.mockResolvedValue({ data: { newVersionAvailable: true } }); await getVersionAvailability('ptc-1'); expect(mocked.get).toHaveBeenCalledWith('/project-test-cases/ptc-1/version'); mocked.post.mockResolvedValue({ data: { upgraded: true } }); await upgradeProjectTestCaseVersion('ptc-1'); expect(mocked.post).toHaveBeenCalledWith('/project-test-cases/ptc-1/version/upgrade'); });
});
