import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { TestCaseDetailPage } from '@/features/testcase/pages/TestCaseDetailPage';
import { TestCaseLibraryPage } from '@/features/testcase/pages/TestCaseLibraryPage';
import { TestCaseDraftForm } from '@/features/testcase/components/TestCaseDraftForm';
import { useTestCase, useTestCaseVersion, useTestCaseVersions, useTestCases, useReviewRecords, useContributors } from '@/features/testcase/hooks/useTestCases';

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: () => ({ matches: false, addListener: vi.fn(), removeListener: vi.fn(), addEventListener: vi.fn(), removeEventListener: vi.fn() }),
});

vi.mock('@/features/auth/hooks/useCurrentUser', () => ({
  useCurrentUser: vi.fn(() => ({ data: { permissions: ['test_case:draft_create'], roles: [] } })),
}));
vi.mock('@/features/testcase/hooks/useTestCases', () => {
  const noopMutation = () => ({ isPending: false, mutate: vi.fn(), mutateAsync: vi.fn() });
  return {
    useTestCases: vi.fn(),
    useTestCase: vi.fn(),
    useTestCaseVersions: vi.fn(),
    useTestCaseVersion: vi.fn(),
    useReviewRecords: vi.fn(),
    useContributors: vi.fn(),
    useSubmitReview: vi.fn(noopMutation),
    usePublish: vi.fn(noopMutation),
    useReturnReview: vi.fn(noopMutation),
    useReject: vi.fn(noopMutation),
    useDeprecate: vi.fn(noopMutation),
    useCreateRevision: vi.fn(noopMutation),
    useAddContributor: vi.fn(noopMutation),
    useRemoveContributor: vi.fn(noopMutation),
  };
});
vi.mock('@/features/dictionary/hooks/useCategories', () => ({ useCategories: vi.fn(() => ({ data: [] })) }));
vi.mock('@/features/dictionary/hooks/useTags', () => ({ useTags: vi.fn(() => ({ data: [] })) }));
vi.mock('@/features/dictionary/hooks/useTools', () => ({ useTools: vi.fn(() => ({ data: [] })) }));
vi.mock('@/features/dictionary/hooks/useStandards', () => ({ useStandards: vi.fn(() => ({ data: [] })) }));

const detail = {
  id: 'master-1', caseCode: 'BLE-001', categoryId: 'cat-1', categoryName: 'Bluetooth', createdBy: 'user-1', enabled: true,
  createdAt: '2026-09-01T00:00:00Z', updatedAt: '2026-09-02T00:00:00Z', tags: [{ id: 'tag-1', code: 'ble', name: 'BLE' }],
  currentVersion: null, draftVersion: null, allowedActions: { editDraft: false, createDraft: false },
  visibleVersion: {
    id: 'version-1', masterTestCaseId: 'master-1', versionLabel: '1.0', versionMajor: 1, versionMinor: 0, status: 'PUBLISHED',
    isCurrentVersion: true, caseName: 'Pairing', testPurpose: 'Verify pairing', preconditions: 'Powered device', selectionMode: 'SINGLE',
    evidenceRequired: true, evidenceRequirement: 'Logs', remarkRequirement: 'None', progressiveRole: 'ENTRY', createdBy: 'user-1',
    revisionClosed: true, steps: [{ id: 'step-1', sequenceNo: 1, title: 'Connect', content: 'Connect device' }],
    tools: [{ id: 'tool-1', code: 'adb', name: 'ADB' }], standardMappings: [{ standardTaskTypeId: 'std-1', standardCode: 'STD', standardName: 'Standard', mappingNote: 'baseline' }],
    attachments: [], createdAt: '2026-09-01T00:00:00Z', updatedAt: '2026-09-02T00:00:00Z',
  },
  versions: [{ id: 'version-1', versionLabel: '1.0', versionMajor: 1, versionMinor: 0, status: 'PUBLISHED', isCurrentVersion: true, createdBy: 'user-1', publishedAt: '2026-09-01T00:00:00Z', createdAt: '2026-09-01T00:00:00Z' }],
};

describe('Phase 6 review round frontend behavior', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useTestCases).mockReturnValue({ data: { content: [], page: 0, size: 20, totalElements: 45, totalPages: 3, first: true, last: false }, isLoading: false, isError: false, refetch: vi.fn() } as never);
    vi.mocked(useTestCase).mockReturnValue({ data: detail, isLoading: false, isError: false } as never);
    vi.mocked(useTestCaseVersions).mockReturnValue({ data: detail.versions, isLoading: false } as never);
    vi.mocked(useTestCaseVersion).mockReturnValue({ data: detail.visibleVersion, isLoading: false } as never);
    vi.mocked(useReviewRecords).mockReturnValue({ data: [], isLoading: false } as never);
    vi.mocked(useContributors).mockReturnValue({ data: [], isLoading: false } as never);
  });

  it('libraryFilterState', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter><TestCaseLibraryPage /></MemoryRouter>);
    await user.type(screen.getByPlaceholderText('搜索编码、名称、目的、步骤或工具'), 'ble');
    expect(vi.mocked(useTestCases).mock.calls.at(-1)?.[0]).toMatchObject({ q: 'ble', page: 0, size: 20 });
  });

  it('pagination', () => {
    render(<MemoryRouter><TestCaseLibraryPage /></MemoryRouter>);
    expect(screen.getByText('共 45 条')).toBeInTheDocument();
  });

  it('createDraftPermission', async () => {
    const { useCurrentUser } = await import('@/features/auth/hooks/useCurrentUser');
    vi.mocked(useCurrentUser).mockReturnValue({ data: { permissions: [], roles: [] } } as never);
    render(<MemoryRouter><TestCaseLibraryPage /></MemoryRouter>);
    expect(screen.queryByRole('button', { name: '新建 Draft' })).not.toBeInTheDocument();
  });

  it('progressiveRoleSelection', () => {
    render(<TestCaseDraftForm onSubmit={vi.fn()} />);
    expect(screen.getByText('渐进角色')).toBeInTheDocument();
  });

  it('stepsAddRemoveReorder', async () => {
    const user = userEvent.setup();
    render(<TestCaseDraftForm onSubmit={vi.fn()} />);
    await user.click(screen.getByRole('button', { name: '新增步骤' }));
    expect(screen.getByLabelText('步骤 1 内容')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /删/ })).toBeInTheDocument();
  });

  it('standardMappingNotePreserved', async () => {
    render(<TestCaseDraftForm onSubmit={vi.fn()} standardOptions={[{ value: 'std-1', label: 'Standard' }]} initialValues={{ standardMappings: [{ standardTaskTypeId: 'std-1', mappingNote: 'keep this note' }] }} />);
    expect(await screen.findByDisplayValue('keep this note')).toBeInTheDocument();
  });

  it('detailRendering', () => {
    render(<MemoryRouter><TestCaseDetailPage /></MemoryRouter>);
    expect(screen.getByText('BLE-001')).toBeInTheDocument();
    expect(screen.getByText('Pairing')).toBeInTheDocument();
    expect(screen.getByText('Connect device')).toBeInTheDocument();
    expect(screen.getByText(/baseline/)).toBeInTheDocument();
  });

  it('versionHistoryRendering', () => {
    render(<MemoryRouter><TestCaseDetailPage /></MemoryRouter>);
    expect(screen.getByText('版本历史')).toBeInTheDocument();
    expect(screen.getAllByText('1.0').length).toBeGreaterThan(0);
  });

  it('readOnlyDetailForTester', () => {
    render(<MemoryRouter><TestCaseDetailPage /></MemoryRouter>);
    expect(screen.queryByRole('button', { name: '编辑 Draft' })).not.toBeInTheDocument();
  });
});
