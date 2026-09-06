import { describe, expect, it, vi } from 'vitest';
import { httpClient } from '@/shared/api/httpClient';
import {
  createUser,
  disableUser,
  enableUser,
  listRoles,
  listUsers,
  resetUserPassword,
  updateUser,
  updateUserRoles,
} from '@/features/user/api/userApi';

vi.mock('@/shared/api/httpClient', () => ({
  httpClient: { get: vi.fn(), post: vi.fn(), put: vi.fn() },
}));

describe('userApi', () => {
  it('lists users with q / enabled / role / pagination params', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({ data: { content: [] } } as never);

    await listUsers({ q: 'ali', enabled: false, role: 'TESTER', page: 2, size: 50 });

    expect(httpClient.get).toHaveBeenCalledWith('/users', {
      params: { q: 'ali', enabled: false, role: 'TESTER', page: 2, size: 50 },
    });
  });

  it('creates a user and omits the password when not supplied', async () => {
    vi.mocked(httpClient.post).mockResolvedValue({ data: { id: 'u1' } } as never);

    await createUser({ username: 'alice', displayName: 'Alice', roles: ['TESTER'] });

    expect(httpClient.post).toHaveBeenCalledWith('/users', {
      username: 'alice',
      displayName: 'Alice',
      roles: ['TESTER'],
    });
  });

  it('updates display name, roles, enable/disable and password reset endpoints', async () => {
    vi.mocked(httpClient.put).mockResolvedValue({ data: {} } as never);
    vi.mocked(httpClient.post).mockResolvedValue({ data: {} } as never);

    await updateUser('u1', { displayName: 'New' });
    await updateUserRoles('u1', ['ADMIN', 'TESTER']);
    await enableUser('u1');
    await disableUser('u1');
    await resetUserPassword('u1');
    await resetUserPassword('u1', 'Temp-Passw0rd!');

    expect(httpClient.put).toHaveBeenCalledWith('/users/u1', { displayName: 'New' });
    expect(httpClient.put).toHaveBeenCalledWith('/users/u1/roles', { roles: ['ADMIN', 'TESTER'] });
    expect(httpClient.post).toHaveBeenCalledWith('/users/u1/enable');
    expect(httpClient.post).toHaveBeenCalledWith('/users/u1/disable');
    expect(httpClient.post).toHaveBeenCalledWith('/users/u1/password-reset', { password: undefined });
    expect(httpClient.post).toHaveBeenCalledWith('/users/u1/password-reset', { password: 'Temp-Passw0rd!' });
  });

  it('loads the role dictionary', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({ data: [] } as never);

    await listRoles();

    expect(httpClient.get).toHaveBeenCalledWith('/roles');
  });
});
