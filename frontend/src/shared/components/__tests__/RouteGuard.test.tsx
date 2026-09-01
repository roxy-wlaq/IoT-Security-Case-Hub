import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { RouteGuard } from '@/shared/components/RouteGuard';
import { currentUserQueryKey } from '@/features/auth/hooks/useCurrentUser';
import type { RouteGuardProps } from '@/shared/components/RouteGuard';
import type { CurrentUser } from '@/shared/types/auth';

const admin: CurrentUser = {
  id: '1',
  username: 'admin',
  displayName: 'Admin',
  enabled: true,
  mustChangePassword: false,
  roles: ['ADMIN'],
  permissions: ['user:read', 'audit:read'],
};

const tester: CurrentUser = {
  id: '2',
  username: 'tester',
  displayName: 'Tester',
  enabled: true,
  mustChangePassword: false,
  roles: ['TESTER'],
  permissions: ['test_case:read'],
};

function renderRouteGuard(user: CurrentUser | null, guardProps: Omit<RouteGuardProps, 'children'>) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  if (user !== undefined) {
    queryClient.setQueryData(currentUserQueryKey, user);
  }
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/protected']}>
        <Routes>
          <Route
            path="/protected"
            element={
              <RouteGuard {...guardProps}>
                <div>secret</div>
              </RouteGuard>
            }
          />
          <Route path="/login" element={<div>login-page</div>} />
          <Route path="/403" element={<div>forbidden-page</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('RouteGuard', () => {
  it('redirects unauthenticated users to /login', () => {
    renderRouteGuard(null, {});
    expect(screen.queryByText('secret')).not.toBeInTheDocument();
    expect(screen.getByText('login-page')).toBeInTheDocument();
  });

  it('renders children for an authenticated user', () => {
    renderRouteGuard(tester, {});
    expect(screen.getByText('secret')).toBeInTheDocument();
  });

  it('redirects to /403 when the required permission is missing', () => {
    renderRouteGuard(tester, { permission: 'audit:read' });
    expect(screen.queryByText('secret')).not.toBeInTheDocument();
    expect(screen.getByText('forbidden-page')).toBeInTheDocument();
  });

  it('denies ADMIN when a specific non-ADMIN role is required (MEDIUM-03)', () => {
    renderRouteGuard(admin, { roles: ['TESTER'] });
    expect(screen.queryByText('secret')).not.toBeInTheDocument();
    expect(screen.getByText('forbidden-page')).toBeInTheDocument();
  });

  it('allows ADMIN through an ADMIN-only role gate', () => {
    renderRouteGuard(admin, { roles: ['ADMIN'] });
    expect(screen.getByText('secret')).toBeInTheDocument();
  });
});
