import type { ReactNode } from 'react';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { hasAllPermissions, hasAnyPermission, hasAnyRole, hasPermission } from '@/shared/api/apiError';

export interface PermissionGuardProps {
  children?: ReactNode;
  /** 需要单个 permission，例如 'user:create' */
  permission?: string;
  /** 命中任意一个 permission 即可 */
  anyOf?: string[];
  /** 必须同时具备全部 permission */
  allOf?: string[];
  /** 命中任意一个 role 即可；ADMIN 视为拥有全部 role */
  roles?: string[];
  /** 无权限时渲染的内容，缺省为 null */
  fallback?: ReactNode;
}

/**
 * 细粒度 UI 权限控制。仅用于 UI 展示，不能替代后端鉴权
 * （Security & RBAC Detail V1.0 §1）。
 */
export function PermissionGuard({ children, permission, anyOf, allOf, roles, fallback = null }: PermissionGuardProps) {
  const { data: user } = useCurrentUser();

  const allowed =
    hasPermission(user, permission) &&
    hasAnyPermission(user, anyOf) &&
    hasAllPermissions(user, allOf) &&
    hasAnyRole(user, roles);

  if (!allowed) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
}

export default PermissionGuard;
