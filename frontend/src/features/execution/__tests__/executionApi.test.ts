import { describe, expect, it, vi } from 'vitest';
import { httpClient } from '@/shared/api/httpClient';
import { executionApi } from '@/features/execution/api/executionApi';

vi.mock('@/shared/api/httpClient', () => ({
  httpClient: { get: vi.fn(), post: vi.fn(), delete: vi.fn(), patch: vi.fn() },
}));

const mocked = httpClient as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  delete: ReturnType<typeof vi.fn>;
};

describe('executionApi', () => {
  it('loads the execution detail contract', async () => {
    mocked.get.mockResolvedValue({ data: { projectTestCaseId: 'ptc-1' } });
    await executionApi.detail('ptc-1');
    expect(mocked.get).toHaveBeenCalledWith('/project-test-cases/ptc-1/execution');
  });

  it('posts the selected Decision Points for completion', async () => {
    mocked.post.mockResolvedValue({ data: { executionStatus: 'COMPLETED' } });
    await executionApi.complete('ptc-1', ['dp-1']);
    expect(mocked.post).toHaveBeenCalledWith('/project-test-cases/ptc-1/execution/complete', { selectedDecisionPointIds: ['dp-1'] });
  });

  it('requests evidence as a downloadable blob', async () => {
    mocked.get.mockResolvedValue({ data: new Blob(['proof']) });
    const createObjectURL = vi.fn().mockReturnValue('blob:test');
    const revokeObjectURL = vi.fn();
    vi.stubGlobal('URL', { ...URL, createObjectURL, revokeObjectURL });
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    await executionApi.downloadEvidence('ptc-1', 'e-1', 'proof.txt');

    expect(mocked.get).toHaveBeenCalledWith('/project-test-cases/ptc-1/evidence/e-1/download', { responseType: 'blob' });
    expect(createObjectURL).toHaveBeenCalledOnce();
    expect(click).toHaveBeenCalledOnce();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:test');
    vi.unstubAllGlobals(); click.mockRestore();
  });
});
