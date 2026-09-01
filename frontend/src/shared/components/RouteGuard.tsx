import type { ReactNode } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { hasAllPermissions, hasAnyPermission, hasAnyRole, hasPermission } from '@/shared/api/apiError';
import { ErrorState } from '@/shared/components/ErrorState';
import { LoadingState } from '@/shared/components/LoadingState';

export interface RouteGuardProps {
  children?: ReactNode;
  /** 需要单个 permission */
  permission?: string;
  /** 命中任意一个 permission 即可 */
  anyOf?: string[];
  /** 必须同时具备全部 permission */
  allOf?: string[];
  /** 命中任意一个 role 即可 */
  roles?: string[];
  /** 仅未登录可访问（登录页），已登录则重定向到首页 */
  guestOnly?: boolean;
}

/**
 * 路由守卫。
 *
 * - 未登录 → 重定向 /login，并通过 location.state.from 保留来源以便登录后回跳
 * - 已登录访问 /login → 重定向 /
 * - 已登录但权限不足 → /403
 * - /me 查询失败（非 401）→ ErrorState + 重试
 */
export function RouteGuard({ children, permission, anyOf, allOf, roles, guestOnly = false }: RouteGuardProps) {
  const location = useLocation();
  const { data: user, isLoading, isError, error, refetch } = useCurrentUser();

  if (isLoading) {
    return <LoadingState tip="正在校验登录状态…" block />;
  }

  if (isError) {
    return <ErrorState error={error} onRetry={() => void refetch()} title="无法获取登录状态" />;
  }

  if (guestOnly) {
    if (user) {
      return <Navigate to="/" replace />;
    }
    return <>{children ?? <Outlet />}</>;
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  const allowed =
    hasPermission(user, permission) &&
    hasAnyPermission(user, anyOf) &&
    hasAllPermissions(user, allOf) &&
    hasAnyRole(user, roles);

  if (!allowed) {
    return <Navigate to="/403" replace />;
  }

  return <>{children ?? <Outlet />}</>;
}

export default RouteGuard;
