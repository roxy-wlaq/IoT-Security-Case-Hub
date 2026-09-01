import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PermissionGuard } from '@/shared/components/PermissionGuard';
import { currentUserQueryKey } from '@/features/auth/hooks/useCurrentUser';
import type { PermissionGuardProps } from '@/shared/components/PermissionGuard';
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

function renderGuard(user: CurrentUser | null, props: Omit<PermissionGuardProps, 'children'>) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  if (user !== undefined) {
    queryClient.setQueryData(currentUserQueryKey, user);
  }
  return render(
    <QueryClientProvider client={queryClient}>
      <PermissionGuard {...props}>
        <div>guarded-content</div>
      </PermissionGuard>
    </QueryClientProvider>
  );
}

describe('PermissionGuard', () => {
  it('renders children when the user has the permission', () => {
    renderGuard(admin, { permission: 'audit:read' });
    expect(screen.getByText('guarded-content')).toBeInTheDocument();
  });

  it('renders fallback when the permission is missing', () => {
    renderGuard(tester, { permission: 'audit:read' });
    expect(screen.queryByText('guarded-content')).not.toBeInTheDocument();
  });

  it('hides content for ADMIN when a specific non-ADMIN role is required (MEDIUM-03)', () => {
    renderGuard(admin, { roles: ['TESTER'] });
    expect(screen.queryByText('guarded-content')).not.toBeInTheDocument();
  });

  it('shows content for ADMIN when the ADMIN role is required', () => {
    renderGuard(admin, { roles: ['ADMIN'] });
    expect(screen.getByText('guarded-content')).toBeInTheDocument();
  });
});
