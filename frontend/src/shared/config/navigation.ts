import { hasAnyRole, hasPermission } from '@/shared/api/apiError';
import type { CurrentUser } from '@/shared/types/auth';

/**
 * 菜单项与所需 permission / role 的映射。
 *
 * Phase 3 只要求"不同角色登录看到不同菜单"，模块页面本身为占位页。
 * 权限码取自 Security & RBAC Detail V1.0 §33。
 */
export interface NavigationItem {
  /** 路由路径，同时用作 antd Menu 的 key */
  path: string;
  label: string;
  /** 需要的单一 permission */
  permission?: string;
  /** 额外需要命中其中之一的 role */
  roles?: string[];
  /** 占位页展示的说明 */
  description: string;
  /** 计划实现阶段 */
  plannedPhase: string;
}

export const NAVIGATION_ITEMS: readonly NavigationItem[] = [
  {
    path: '/',
    label: '工作台',
    description: '显示当前登录用户、角色与权限数量。',
    plannedPhase: 'Phase 3',
  },
  {
    path: '/users',
    label: '用户管理',
    permission: 'user:read',
    description: '用户、角色与权限管理。',
    plannedPhase: 'Phase 4+',
  },
  {
    path: '/test-cases',
    label: '测试库',
    permission: 'test_case:read',
    description: 'Master Test Case 库、版本与生命周期。',
    plannedPhase: 'Phase 6',
  },
  {
    path: '/capabilities',
    label: '能力库',
    permission: 'capability:read',
    description: '能力树与项目能力矩阵。',
    plannedPhase: 'Phase 5',
  },
  {
    path: '/projects',
    label: '项目管理',
    permission: 'project:read',
    description: '项目、能力矩阵、生成与测试计划。',
    plannedPhase: 'Phase 9',
  },
  {
    path: '/generation-rules',
    label: '生成规则',
    permission: 'generation_rule:read',
    description: '生成规则条件组与输出配置。',
    plannedPhase: 'Phase 11',
  },
  {
    path: '/audit-logs',
    label: '审计日志',
    permission: 'audit:read',
    roles: ['ADMIN'],
    description: '全系统审计事件查询（仅管理员）。',
    plannedPhase: 'Phase 26',
  },
];

export function findNavigationItem(path: string): NavigationItem | undefined {
  return NAVIGATION_ITEMS.find((item) => item.path === path);
}

export function hasNavigationAccess(item: NavigationItem, user: CurrentUser | null | undefined): boolean {
  if (!user) {
    return false;
  }
  return hasPermission(user, item.permission) && hasAnyRole(user, item.roles);
}

export function filterNavigation(items: readonly NavigationItem[], user: CurrentUser | null | undefined): NavigationItem[] {
  return items.filter((item) => hasNavigationAccess(item, user));
}
