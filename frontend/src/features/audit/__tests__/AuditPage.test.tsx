import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import AuditPage from '@/features/audit/pages/AuditPage';
import { currentUserQueryKey } from '@/features/auth/hooks/useCurrentUser';
import { listAuditLogs } from '@/features/audit/api/auditApi';
import type { CurrentUser } from '@/shared/types/auth';

vi.mock('@/features/audit/api/auditApi', () => {
  return {
    AUDIT_ACTIONS: ['LOGIN', 'LOGIN_FAILURE', 'ROLE_CHANGE', 'PROJECT_CREATE', 'PROJECT_ARCHIVE',
      'TEST_CASE_PUBLISH', 'TEST_CASE_DEPRECATE', 'GENERATION_RULE_UPDATE',
      'CAPABILITY_LIBRARY_UPDATE', 'EVIDENCE_DELETE'],
    listAuditLogs: vi.fn(),
  };
});

const admin: CurrentUser = {
  id: 'admin-id', username: 'admin', displayName: 'Admin', enabled: true, mustChangePassword: false,
  roles: ['ADMIN'], permissions: ['audit:read'],
};
const tester: CurrentUser = {
  id: 'tester-id', username: 'tester', displayName: 'Tester', enabled: true, mustChangePassword: false,
  roles: ['TESTER'], permissions: ['audit:read'],
};

const response = (content = [{
  id: 'audit-1', occurredAt: '2026-01-01T00:00:00Z', action: 'PROJECT_CREATE' as const,
  actorUsername: 'admin', resourceType: 'PROJECT', resourceId: 'project-1', resourceLabel: 'Demo', detail: { safe: true },
}]) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1, first: true, last: true });

function renderPage(user: CurrentUser) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  queryClient.setQueryData(currentUserQueryKey, user);
  return render(<QueryClientProvider client={queryClient}><AuditPage /></QueryClientProvider>);
}

describe('AuditPage', () => {
  beforeAll(() => {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation((query: string) => ({
        matches: false, media: query, onchange: null,
        addListener: vi.fn(), removeListener: vi.fn(),
        addEventListener: vi.fn(), removeEventListener: vi.fn(), dispatchEvent: vi.fn(),
      })),
    });
  });

  beforeEach(() => {
    vi.mocked(listAuditLogs).mockResolvedValue(response());
  });

  it('requires ADMIN plus audit:read in the UI', () => {
    renderPage(tester);
    expect(screen.queryByText('审计日志')).not.toBeInTheDocument();
  });

  it('renders rows, filters, pagination, and no mutation controls', async () => {
    vi.mocked(listAuditLogs).mockResolvedValue({ ...response(), totalElements: 40, totalPages: 2 });
    renderPage(admin);

    expect(await screen.findByText('PROJECT_CREATE')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Resource type')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Resource ID')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Actor username')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /查\s*询/ })).toBeInTheDocument();
    expect(screen.getByTitle('2')).toBeInTheDocument();
    expect(screen.queryByText('删除')).not.toBeInTheDocument();
    expect(screen.queryByText('编辑')).not.toBeInTheDocument();
  });

  it('passes resource and actor filters and reports empty results', async () => {
    vi.mocked(listAuditLogs).mockResolvedValue(response([]));
    renderPage(admin);

    await screen.findByText('审计日志');
    fireEvent.change(screen.getByPlaceholderText('Resource ID'), { target: { value: 'project-1' } });
    fireEvent.change(screen.getByPlaceholderText('Actor username'), { target: { value: 'admin' } });
    fireEvent.click(screen.getByRole('button', { name: /查\s*询/ }));

    await waitFor(() => expect(listAuditLogs).toHaveBeenLastCalledWith(expect.objectContaining({
      resourceId: 'project-1', actorUsername: 'admin', page: 0, size: 20,
    })));
    expect(screen.getAllByText('No data').length).toBeGreaterThan(0);
  });

  it('shows the loading and error states', async () => {
    let reject!: (error: Error) => void;
    vi.mocked(listAuditLogs).mockReturnValue(new Promise((_, fail) => { reject = fail; }));
    renderPage(admin);
    expect(screen.getByText('审计日志')).toBeInTheDocument();
    reject(new Error('network'));
    expect(await screen.findByText('审计日志加载失败')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /查\s*询/ })).toBeInTheDocument();
  });
});
