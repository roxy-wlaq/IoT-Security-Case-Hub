import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  getTestCase,
  getTestCaseVersion,
  getReviewRecords,
  listContributors,
  listTestCaseVersions,
  submitReview,
  publishVersion,
  returnReview,
  rejectVersion,
  deprecateVersion,
  createRevision,
} from '@/features/testcase/api/testCaseApi';
import type { TestCaseDetail, TestCaseVersion } from '@/shared/types/testCase';
import { TestCaseDetailPage } from '@/features/testcase/pages/TestCaseDetailPage';

vi.mock('@/features/testcase/api/testCaseApi', () => ({
  getTestCase: vi.fn(),
  listTestCaseVersions: vi.fn(),
  getTestCaseVersion: vi.fn(),
  getReviewRecords: vi.fn(),
  listContributors: vi.fn(),
  submitReview: vi.fn(),
  publishVersion: vi.fn(),
  returnReview: vi.fn(),
  rejectVersion: vi.fn(),
  deprecateVersion: vi.fn(),
  createRevision: vi.fn(),
  addContributor: vi.fn(),
  removeContributor: vi.fn(),
}));

// antd's responsive observer (useBreakpoint) reads window.matchMedia; jsdom lacks it.
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: () => ({ matches: false, addListener: vi.fn(), removeListener: vi.fn(), addEventListener: vi.fn(), removeEventListener: vi.fn() }),
});

const versionBase: TestCaseVersion = {
  id: 'v-1',
  masterTestCaseId: 'master-1',
  versionLabel: 'v1.0',
  versionMajor: 1,
  versionMinor: 0,
  status: 'DRAFT',
  isCurrentVersion: false,
  caseName: 'BLE Pairing',
  testPurpose: 'purpose',
  preconditions: 'pre',
  selectionMode: 'SINGLE',
  evidenceRequired: false,
  evidenceRequirement: null,
  remarkRequirement: null,
  progressiveRole: null,
  basedOnVersionId: null,
  changeReason: null,
  createdBy: 'owner-1',
  reviewedBy: null,
  publishedAt: null,
  deprecatedAt: null,
  revisionClosed: false,
  latestReviewAction: null,
  steps: [{ id: 's-1', sequenceNo: 1, title: 'Step 1', content: 'do something' }],
  tools: [],
  standardMappings: [],
  attachments: [],
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};

function draftDetail(): TestCaseDetail {
  return {
    id: 'master-1',
    caseCode: 'BLE-001',
    categoryId: 'cat-1',
    categoryName: 'Category',
    createdBy: 'owner-1',
    enabled: true,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    tags: [],
    currentVersion: null,
    draftVersion: { ...versionBase },
    visibleVersion: { ...versionBase },
    versions: [{ id: 'v-1', versionLabel: 'v1.0', versionMajor: 1, versionMinor: 0, status: 'DRAFT', isCurrentVersion: false, changeReason: null, createdBy: 'owner-1', createdAt: new Date().toISOString() }],
    allowedActions: { editDraft: true, createDraft: false, submitReview: true, publish: false, returnReview: false, reject: false, deprecate: false, createRevision: false, manageContributors: true },
  };
}

function rejectedDetail(): TestCaseDetail {
  const reviewVersion: TestCaseVersion = { ...versionBase, id: 'v-review', status: 'REVIEW', revisionClosed: true, latestReviewAction: 'REJECT' };
  return {
    id: 'master-1',
    caseCode: 'BLE-001',
    categoryId: 'cat-1',
    categoryName: 'Category',
    createdBy: 'owner-1',
    enabled: true,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    tags: [],
    currentVersion: null,
    draftVersion: null,
    visibleVersion: reviewVersion,
    versions: [{ id: 'v-review', versionLabel: 'v1.0', versionMajor: 1, versionMinor: 0, status: 'REVIEW', isCurrentVersion: false, changeReason: null, createdBy: 'owner-1', createdAt: new Date().toISOString() }],
    allowedActions: { editDraft: false, createDraft: false, submitReview: false, publish: true, returnReview: true, reject: true, deprecate: false, createRevision: false, manageContributors: false },
  };
}

function renderPage(detail: TestCaseDetail) {
  vi.mocked(getTestCase).mockResolvedValue(detail);
  vi.mocked(listTestCaseVersions).mockResolvedValue(detail.versions);
  vi.mocked(getTestCaseVersion).mockResolvedValue(detail.visibleVersion);
  vi.mocked(getReviewRecords).mockResolvedValue([]);
  vi.mocked(listContributors).mockResolvedValue([]);
  vi.mocked(submitReview).mockResolvedValue(detail);
  vi.mocked(publishVersion).mockResolvedValue(detail);
  vi.mocked(returnReview).mockResolvedValue(detail);
  vi.mocked(rejectVersion).mockResolvedValue(detail);
  vi.mocked(deprecateVersion).mockResolvedValue(detail);
  vi.mocked(createRevision).mockResolvedValue(detail);

  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const wrapper = ({ children }: { children?: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/test-cases/master-1']}>
        <Routes>
          <Route path="/test-cases/:masterId" element={children} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
  return render(<TestCaseDetailPage />, { wrapper });
}

describe('TestCaseDetailPage — lifecycle', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the action bar from server-computed AllowedActions and submits for review', async () => {
    const user = userEvent.setup();
    renderPage(draftDetail());

    expect(await screen.findByRole('button', { name: '提交评审' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '编辑 Draft' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '发布' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '提交评审' }));
    // Modal opens with an OK button. antd inserts a space into 2-char CJK labels
    // ("提交" → "提 交"), so match on the whitespace-stripped name.
    await user.click(await screen.findByRole('button', { name: (name) => name.replace(/\s/g, '') === '提交' }));

    await waitFor(() => expect(submitReview).toHaveBeenCalledWith('master-1', expect.objectContaining({ comment: '' })));
  });

  it('shows the Rejected label when latestReviewAction is REJECT', async () => {
    renderPage(rejectedDetail());

    expect(await screen.findByText('已驳回')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: (name) => name.replace(/\s/g, '') === '驳回' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '提交评审' })).not.toBeInTheDocument();
  });
});
