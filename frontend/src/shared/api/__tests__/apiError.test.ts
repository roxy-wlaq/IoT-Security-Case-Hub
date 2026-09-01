import { describe, it, expect } from 'vitest';
import {
  hasPermission,
  hasAnyPermission,
  hasAllPermissions,
  hasAnyRole,
  userHasRole,
} from '@/shared/api/apiError';
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

describe('hasPermission', () => {
  it('grants ADMIN every permission (RBAC §34)', () => {
    expect(hasPermission(admin, 'anything:read')).toBe(true);
  });

  it('checks the permission list for non-admins', () => {
    expect(hasPermission(tester, 'test_case:read')).toBe(true);
    expect(hasPermission(tester, 'audit:read')).toBe(false);
  });

  it('allows when no permission is required', () => {
    expect(hasPermission(tester, undefined)).toBe(true);
  });

  it('denies unauthenticated users', () => {
    expect(hasPermission(null, 'x:read')).toBe(false);
  });
});

describe('userHasRole (MEDIUM-03)', () => {
  it('ADMIN does NOT automatically satisfy an unrelated role', () => {
    expect(userHasRole(admin, 'TESTER')).toBe(false);
    expect(userHasRole(admin, 'TEST_COORDINATOR')).toBe(false);
  });

  it('ADMIN does satisfy the ADMIN role', () => {
    expect(userHasRole(admin, 'ADMIN')).toBe(true);
  });

  it('matches a real assigned role', () => {
    expect(userHasRole(tester, 'TESTER')).toBe(true);
    expect(userHasRole(tester, 'ADMIN')).toBe(false);
  });

  it('denies when unauthenticated', () => {
    expect(userHasRole(null, 'ADMIN')).toBe(false);
  });
});

describe('hasAnyRole', () => {
  it('passes when the user has one of the required roles', () => {
    expect(hasAnyRole(tester, ['ADMIN', 'TESTER'])).toBe(true);
  });

  it('fails when the user has none of the required roles', () => {
    expect(hasAnyRole(tester, ['ADMIN', 'TEST_COORDINATOR'])).toBe(false);
  });
});

describe('hasAnyPermission / hasAllPermissions', () => {
  it('ADMIN satisfies any permission set', () => {
    expect(hasAnyPermission(admin, ['x:read', 'y:write'])).toBe(true);
    expect(hasAllPermissions(admin, ['x:read', 'y:write'])).toBe(true);
  });

  it('enforces the permission set for non-admins', () => {
    expect(hasAnyPermission(tester, ['test_case:read'])).toBe(true);
    expect(hasAnyPermission(tester, ['audit:read'])).toBe(false);
    expect(hasAllPermissions(tester, ['test_case:read'])).toBe(true);
    expect(hasAllPermissions(tester, ['test_case:read', 'audit:read'])).toBe(false);
  });
});
