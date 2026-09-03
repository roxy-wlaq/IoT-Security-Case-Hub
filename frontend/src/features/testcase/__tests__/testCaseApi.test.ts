import { describe, expect, it, vi } from 'vitest';
import { httpClient } from '@/shared/api/httpClient';
import {
  addContributor,
  createDecisionPoint,
  createRevision,
  createTestCase,
  deprecateVersion,
  getReviewRecords,
  getMasterLogicGraph,
  listDecisionPoints,
  listContributors,
  listTestCases,
  publishVersion,
  rejectVersion,
  removeContributor,
  returnReview,
  submitReview,
  updateDecisionPoint,
  deleteDecisionPoint,
} from '@/features/testcase/api/testCaseApi';

vi.mock('@/shared/api/httpClient', () => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

// axios exposes a complex AxiosInstance type, so `vi.mocked` cannot surface the
// mock methods; cast the mock shape explicitly for type-safe assertions.
const mocked = httpClient as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  put: ReturnType<typeof vi.fn>;
  delete: ReturnType<typeof vi.fn>;
};

describe('testCaseApi', () => {
  it('serializes the frozen library query parameters and endpoint', async () => {
    mocked.get.mockResolvedValue({ data: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true } });

    await listTestCases({ q: 'bluetooth', tagIds: ['tag-1', 'tag-2'], page: 0, size: 20, sort: 'updatedAt,desc' });

    expect(mocked.get).toHaveBeenCalledWith('/test-cases', {
      params: { q: 'bluetooth', tagIds: ['tag-1', 'tag-2'], page: 0, size: 20, sort: 'updatedAt,desc' },
    });
  });

  it('creates a Draft through the Phase 6 endpoint', async () => {
    const detail = { id: 'master-1' };
    mocked.post.mockResolvedValue({ data: detail });

    await createTestCase({ caseCode: 'BLE-001' } as never);

    expect(mocked.post).toHaveBeenCalledWith('/test-cases', { caseCode: 'BLE-001' });
  });

  // -------------------------------------------------------------------------
  // Phase 7 — lifecycle endpoints
  // -------------------------------------------------------------------------

  it('submits a Draft for review', async () => {
    mocked.post.mockResolvedValue({ data: { id: 'master-1' } });
    await submitReview('master-1', { comment: 'ready' });
    expect(mocked.post).toHaveBeenCalledWith('/test-cases/master-1/draft/submit-review', { comment: 'ready' });
  });

  it('publishes a REVIEW version', async () => {
    mocked.post.mockResolvedValue({ data: { id: 'master-1' } });
    await publishVersion('master-1', 'v-2', { comment: 'approved' });
    expect(mocked.post).toHaveBeenCalledWith('/test-cases/master-1/versions/v-2/publish', { comment: 'approved' });
  });

  it('returns a REVIEW version to Draft', async () => {
    mocked.post.mockResolvedValue({ data: { id: 'master-1' } });
    await returnReview('master-1', 'v-2', { comment: 'fix steps' });
    expect(mocked.post).toHaveBeenCalledWith('/test-cases/master-1/versions/v-2/return', { comment: 'fix steps' });
  });

  it('rejects a REVIEW version (keeps REVIEW status)', async () => {
    mocked.post.mockResolvedValue({ data: { id: 'master-1' } });
    await rejectVersion('master-1', 'v-2', { comment: 'non-compliant' });
    expect(mocked.post).toHaveBeenCalledWith('/test-cases/master-1/versions/v-2/reject', { comment: 'non-compliant' });
  });

  it('deprecates a PUBLISHED version', async () => {
    mocked.post.mockResolvedValue({ data: { id: 'master-1' } });
    await deprecateVersion('master-1', 'v-2', {});
    expect(mocked.post).toHaveBeenCalledWith('/test-cases/master-1/versions/v-2/deprecate', {});
  });

  it('creates a revision from the current PUBLISHED version', async () => {
    mocked.post.mockResolvedValue({ data: { id: 'master-1' } });
    await createRevision('master-1', { changeReason: 'minor update' });
    expect(mocked.post).toHaveBeenCalledWith('/test-cases/master-1/revisions', { changeReason: 'minor update' });
  });

  it('fetches the review records of a version', async () => {
    mocked.get.mockResolvedValue({ data: [] });
    await getReviewRecords('master-1', 'v-2');
    expect(mocked.get).toHaveBeenCalledWith('/test-cases/master-1/versions/v-2/review-records');
  });

  it('lists contributors of the current Draft', async () => {
    mocked.get.mockResolvedValue({ data: [] });
    await listContributors('master-1');
    expect(mocked.get).toHaveBeenCalledWith('/test-cases/master-1/draft/contributors');
  });

  it('adds a contributor', async () => {
    mocked.post.mockResolvedValue({ data: [] });
    await addContributor('master-1', 'user-9');
    expect(mocked.post).toHaveBeenCalledWith('/test-cases/master-1/draft/contributors', { userId: 'user-9' });
  });

  it('removes a contributor', async () => {
    mocked.delete.mockResolvedValue({ data: [] });
    await removeContributor('master-1', 'user-9');
    expect(mocked.delete).toHaveBeenCalledWith('/test-cases/master-1/draft/contributors/user-9');
  });

  it('reads and edits the version-owned Decision Point contract', async () => {
    mocked.get.mockResolvedValue({ data: [] });
    await listDecisionPoints('master-1', 'v-2');
    expect(mocked.get).toHaveBeenCalledWith('/test-cases/master-1/versions/v-2/decision-points');

    mocked.post.mockResolvedValue({ data: { id: 'dp-1' } });
    const payload = { name: 'Reachable', description: 'branch', displayOrder: 1, transitionType: 'NEXT_CASE' as const, targetMasterTestCaseIds: ['master-2'] };
    await createDecisionPoint('master-1', 'v-2', payload);
    expect(mocked.post).toHaveBeenCalledWith('/test-cases/master-1/versions/v-2/decision-points', payload);

    mocked.put.mockResolvedValue({ data: { id: 'dp-1' } });
    await updateDecisionPoint('master-1', 'v-2', 'dp-1', payload);
    expect(mocked.put).toHaveBeenCalledWith('/test-cases/master-1/versions/v-2/decision-points/dp-1', payload);

    mocked.delete.mockResolvedValue({ data: undefined });
    await deleteDecisionPoint('master-1', 'v-2', 'dp-1');
    expect(mocked.delete).toHaveBeenCalledWith('/test-cases/master-1/versions/v-2/decision-points/dp-1');
  });

  it('reads the Master Logic Graph', async () => {
    mocked.get.mockResolvedValue({ data: { nodes: [], edges: [] } });
    await getMasterLogicGraph('master-1', 'v-2');
    expect(mocked.get).toHaveBeenCalledWith('/test-cases/master-1/versions/v-2/logic-graph');
  });
});
