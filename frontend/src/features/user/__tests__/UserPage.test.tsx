import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Modal } from 'antd';
import type { ReactElement } from 'react';
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { UserAdminPage, UserManagementContent } from '@/features/user/pages/UserPage';
import { currentUserQueryKey } from '@/features/auth/hooks/useCurrentUser';
import {
  createUser,
  disableUser,
  listRoles,
  listUsers,
  resetUserPassword,
  updateUser,
  updateUserRoles,
} from '@/features/user/api/userApi';
import type { CurrentUser } from '@/shared/types/auth';
import type { UserSummary } from '@/features/user/api/userApi';

vi.mock('@/features/user/api/userApi', () => ({
  listUsers: vi.fn(),
  listRoles: vi.fn(),
  createUser: vi.fn(),
  updateUser: vi.fn(),
  updateUserRoles: vi.fn(),
  enableUser: vi.fn(),
  disableUser: vi.fn(),
  resetUserPassword: vi.fn(),
}));

const admin: CurrentUser = {
  id: 'admin-id', username: 'admin', displayName: 'Admin', enabled: true, mustChangePassword: false,
  roles: ['ADMIN'],
  permissions: ['user:read', 'user:create', 'user:update', 'user:disable', 'role:manage'],
};
const tester: CurrentUser = {
  id: 'tester-id', username: 'tester', displayName: 'Tester', enabled: true, mustChangePassword: false,
  roles: ['TESTER'], permissions: [],
};

const alice: UserSummary = {
  id: 'user-1', username: 'alice', displayName: 'Alice', enabled: true, mustChangePassword: false,
  roles: ['TESTER'],
};
const bob: UserSummary = {
  id: 'user-2', username: 'bob', displayName: 'Bob', enabled: false, mustChangePassword: true,
  roles: ['TEST_COORDINATOR'],
};

function paged(content: UserSummary[], totalElements = content.length) {
  return {
    content, page: 0, size: 20, totalElements,
    totalPages: Math.max(1, Math.ceil(totalElements / 20)), first: true, last: totalElements <= 20,
  };
}

function renderWithUser(user: CurrentUser, element: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  queryClient.setQueryData(currentUserQueryKey, user);
  return render(<QueryClientProvider client={queryClient}>{element}</QueryClientProvider>);
}

function renderPage(user: CurrentUser) {
  return renderWithUser(user, <UserAdminPage />);
}

/** antd 的 Select 下拉渲染在 document.body 的 portal 中，据此取当前可见的下拉容器 */
function visibleDropdown(): HTMLElement {
  const node = document.querySelector('.ant-select-dropdown:not(.ant-select-dropdown-hidden)');
  if (!(node instanceof HTMLElement)) {
    throw new Error('ant-select-dropdown portal not found');
  }
  return node;
}

describe('UserAdminPage', () => {
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

  afterEach(() => {
    // antd 静态方法 Modal.info/Modal.confirm 渲染到 body 的独立容器，
    // 不随 React 树 cleanup 移除，会污染后续用例的 findByRole('dialog')。
    Modal.destroyAll();
  });

  beforeEach(() => {
    vi.mocked(listUsers).mockResolvedValue(paged([alice, bob]));
    vi.mocked(listRoles).mockResolvedValue([
      { code: 'ADMIN', name: 'Administrator' },
      { code: 'TESTER', name: 'Tester' },
    ]);
    vi.mocked(createUser).mockReset();
    vi.mocked(updateUser).mockReset();
    vi.mocked(updateUserRoles).mockReset();
    vi.mocked(disableUser).mockReset();
    vi.mocked(resetUserPassword).mockReset();
  });

  it('hides the page from non-admin users', async () => {
    renderPage(tester);
    expect(screen.queryByText('用户管理')).not.toBeInTheDocument();
    expect(listUsers).not.toHaveBeenCalled();
  });

  it('renders users with status, forced-change flag and roles', async () => {
    renderPage(admin);

    expect(await screen.findByText('alice')).toBeInTheDocument();
    expect(screen.getByText('bob')).toBeInTheDocument();
    // 「启用」同时出现在 alice 的状态 Tag 与 bob 操作列的启用按钮中，需区分断言
    const aliceRow = screen.getByRole('row', { name: /alice/ });
    expect(within(aliceRow).getByText('启用')).toBeInTheDocument();
    const bobRow = screen.getByRole('row', { name: /bob/ });
    expect(within(bobRow).getByText('已禁用')).toBeInTheDocument();
    expect(within(bobRow).getByText('待修改密码')).toBeInTheDocument();
    expect(within(aliceRow).getByText('TESTER')).toBeInTheDocument();
    expect(within(bobRow).getByText('TEST_COORDINATOR')).toBeInTheDocument();
  });

  it('renders operation buttons for an admin who has the page-level role', async () => {
    // 冻结语义：hasPermission() 对 ADMIN 直接放行全部权限（apiError.ts §34），
    // 因此只要是 ADMIN（page 入口已校验 roles=['ADMIN']）就能看到全部操作按钮。
    const adminRoleOnly: CurrentUser = {
      ...admin,
      id: 'viewer-id',
      username: 'viewer',
      // 后端 UserPrincipal 中 ADMIN 也可能只关联部分 user:* 权限；
      // 但前端 hasPermission 将 ADMIN 视为全权限，故 UI 恒给出操作入口（点按后由后端 403 兜底）。
      permissions: ['user:read'],
    };
    renderPage(adminRoleOnly);

    await screen.findByText('alice');
    expect(screen.getByText('创建用户')).toBeInTheDocument();
    const aliceRow = screen.getByRole('row', { name: /alice/ });
    expect(within(aliceRow).getByText('编辑')).toBeInTheDocument();
    expect(within(aliceRow).getByText('角色')).toBeInTheDocument();
    expect(within(aliceRow).getByText('重置密码')).toBeInTheDocument();
    expect(within(aliceRow).getByText('禁用')).toBeInTheDocument();
  });

  it('hides operation buttons from a non-admin with only user:read', async () => {
    // 绕过页面的 roles=['ADMIN'] 门槛，直接渲染内层以验证按钮级权限细分：
    // 非 ADMIN 用户只有拥有对应 user:* / role:* 权限码时才会看到相应操作入口。
    const readOnlyNonAdmin: CurrentUser = {
      id: 'coord-id', username: 'coord', displayName: 'Coord', enabled: true, mustChangePassword: false,
      roles: ['COORDINATOR'], permissions: ['user:read'],
    };
    renderWithUser(readOnlyNonAdmin, <UserManagementContent />);

    await screen.findByText('alice');
    expect(screen.queryByText('创建用户')).not.toBeInTheDocument();
    const aliceRow = screen.getByRole('row', { name: /alice/ });
    expect(within(aliceRow).queryByText('编辑')).not.toBeInTheDocument();
    expect(within(aliceRow).queryByText('角色')).not.toBeInTheDocument();
    expect(within(aliceRow).queryByText('重置密码')).not.toBeInTheDocument();
    expect(within(aliceRow).queryByText('禁用')).not.toBeInTheDocument();
  });

  it('shows only the enable/disable button for a non-admin with user:disable', async () => {
    const withDisable: CurrentUser = {
      id: 'coord2-id', username: 'coord2', displayName: 'Coord2', enabled: true, mustChangePassword: false,
      roles: ['COORDINATOR'], permissions: ['user:read', 'user:disable'],
    };
    renderWithUser(withDisable, <UserManagementContent />);

    await screen.findByText('alice');
    const aliceRow = screen.getByRole('row', { name: /alice/ });
    expect(within(aliceRow).getByText('禁用')).toBeInTheDocument();
    expect(within(aliceRow).queryByText('编辑')).not.toBeInTheDocument();
    expect(within(aliceRow).queryByText('角色')).not.toBeInTheDocument();
    expect(within(aliceRow).queryByText('重置密码')).not.toBeInTheDocument();
  });

  it('disables self-targeting disable and password reset actions', async () => {
    const selfRow: UserSummary = { ...admin, id: 'admin-id', username: 'admin', roles: ['ADMIN'] };
    vi.mocked(listUsers).mockResolvedValue(paged([selfRow, alice]));
    renderPage(admin);

    await screen.findByText('alice');
    const adminRow = screen.getByRole('row', { name: /^admin/ });
    // 用 toBeDisabled 而非 ant-btn-disabled class（antd 5.29 link 按钮 disabled 不挂该类）
    expect(within(adminRow).getByRole('button', { name: '禁用' })).toBeDisabled();
    expect(within(adminRow).getByRole('button', { name: '重置密码' })).toBeDisabled();
    const aliceRow = screen.getByRole('row', { name: /alice/ });
    expect(within(aliceRow).getByRole('button', { name: '禁用' })).toBeEnabled();
    expect(within(aliceRow).getByRole('button', { name: '重置密码' })).toBeEnabled();
  });

  it('creates a user and shows the one-time generated password', async () => {
    vi.mocked(createUser).mockResolvedValue({
      ...alice, id: 'new-1', username: 'carol', generatedPassword: 'TempPassw0rdX1',
    });
    renderPage(admin);
    const user = userEvent.setup();

    await user.click(await screen.findByText('创建用户'));
    const dialog = await screen.findByRole('dialog');
    const formArea = within(dialog);
    await user.type(await formArea.findByLabelText(/用户名/), 'carol');
    await user.type(formArea.getByLabelText(/显示名称/), 'Carol');
    await user.click(formArea.getByRole('combobox'));
    const dropdown = visibleDropdown();
    await user.click(within(dropdown).getByText('TESTER（Tester）'));
    await user.click(formArea.getByRole('button', { name: /创\s*建/ }));

    await waitFor(() => expect(createUser).toHaveBeenCalledWith({
      username: 'carol', displayName: 'Carol', roles: ['TESTER'], password: undefined,
    }));
    expect(await screen.findByText('TempPassw0rdX1')).toBeInTheDocument();
  }, 30000);

  it('updates a display name through the edit modal', async () => {
    vi.mocked(updateUser).mockResolvedValue({ ...alice, displayName: 'Alice Two' });
    renderPage(admin);
    const user = userEvent.setup();

    await screen.findByText('alice');
    const aliceRow = screen.getByRole('row', { name: /alice/ });
    await user.click(within(aliceRow).getByText('编辑'));
    const dialog = await screen.findByRole('dialog');
    const formArea = within(dialog);
    await user.clear(await formArea.findByLabelText(/显示名称/));
    await user.type(formArea.getByLabelText(/显示名称/), 'Alice Two');
    await user.click(formArea.getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => expect(updateUser).toHaveBeenCalledWith('user-1', { displayName: 'Alice Two' }));
  }, 30000);

  it('replaces roles through the roles modal', async () => {
    vi.mocked(updateUserRoles).mockResolvedValue({ ...alice, roles: ['ADMIN'] });
    renderPage(admin);
    const user = userEvent.setup();

    await screen.findByText('alice');
    const aliceRow = screen.getByRole('row', { name: /alice/ });
    await user.click(within(aliceRow).getByText('角色'));
    const dialog = await screen.findByRole('dialog');
    const formArea = within(dialog);
    await formArea.findByText(/分配角色/);

    // 目标用户当前角色为 TESTER，已作为选中项渲染在 select 中；
    // 先移除它，再从下拉中选择 ADMIN，最终保存即为“全量替换”为 [ADMIN]。
    const combobox = formArea.getByRole('combobox');
    const selectedTag = within(combobox.closest('.ant-select') as HTMLElement).getByText('TESTER（Tester）');
    const removeBtn = selectedTag.closest('.ant-select-selection-item')?.querySelector('.ant-select-selection-item-remove');
    if (removeBtn) {
      await user.click(removeBtn);
    }
    await user.click(combobox);
    const dropdown = visibleDropdown();
    await user.click(within(dropdown).getByText('ADMIN（Administrator）'));
    await user.click(formArea.getByRole('button', { name: /保\s*存/ }));

    await waitFor(() => expect(updateUserRoles).toHaveBeenCalledWith('user-1', ['ADMIN']));
  }, 30000);

  it('resets a password and reveals the generated temporary password once', async () => {
    vi.mocked(resetUserPassword).mockResolvedValue({
      generated: true, password: 'TempPassw0rdX2', mustChangePassword: true,
    });
    renderPage(admin);
    const user = userEvent.setup();

    await screen.findByText('alice');
    const aliceRow = screen.getByRole('row', { name: /alice/ });
    await user.click(within(aliceRow).getByText('重置密码'));
    const dialog = await screen.findByRole('dialog');
    const formArea = within(dialog);
    await user.click(await formArea.findByRole('button', { name: /重\s*置/ }));

    await waitFor(() => expect(resetUserPassword).toHaveBeenCalledWith('user-1', undefined));
    expect(await screen.findByText('TempPassw0rdX2')).toBeInTheDocument();
  }, 30000);

  it('search keyword is passed to the list query', async () => {
    renderPage(admin);
    const user = userEvent.setup();
    const input = await screen.findByPlaceholderText('搜索用户名或显示名称');
    await screen.findByText('alice');

    await user.type(input, 'ali{Enter}');

    await waitFor(() => expect(listUsers).toHaveBeenLastCalledWith(expect.objectContaining({ q: 'ali' })));
  }, 30000);
});
