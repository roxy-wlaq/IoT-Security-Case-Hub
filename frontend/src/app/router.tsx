import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import type { ComponentType } from 'react';
import { RouteGuard } from '@/shared/components/RouteGuard';
import { AppLayout } from '@/features/layout/AppLayout';
import { LoginPage } from '@/features/auth/LoginPage';
import { Dashboard } from '@/pages/Dashboard';
import { PlaceholderPage } from '@/pages/PlaceholderPage';
import { Forbidden } from '@/pages/Forbidden';
import { NotFound } from '@/pages/NotFound';
import { NAVIGATION_ITEMS } from '@/shared/config/navigation';
import { StandardAdminPage } from '@/features/dictionary/pages/StandardPage';
import { CategoryAdminPage } from '@/features/dictionary/pages/CategoryPage';
import { TagAdminPage } from '@/features/dictionary/pages/TagPage';
import { ToolPage } from '@/features/dictionary/pages/ToolPage';
import { CapabilityAdminPage } from '@/features/capability/pages/CapabilityAdminPage';
import { TestCaseDraftPage } from '@/features/testcase/pages/TestCaseDraftPage';
import { TestCaseLibraryPage } from '@/features/testcase/pages/TestCaseLibraryPage';
import { TestCaseDetailPage } from '@/features/testcase/pages/TestCaseDetailPage';
import { ProjectPage } from '@/features/project/pages/ProjectPage';
import { GenerationRulePage } from '@/features/generation/pages/GenerationRulePage';
import { MyTestsPage } from '@/features/project/pages/MyTestsPage';

/**
 * 已实现模块的页面映射（Phase 4 基础字典 / Phase 5 能力库）。
 * 不在映射里的导航项继续渲染占位页，直到对应 Phase 完成。
 */
const pageComponents: Record<string, ComponentType> = {
  '/test-cases': TestCaseLibraryPage,
  '/admin/capabilities': CapabilityAdminPage,
  '/admin/standards': StandardAdminPage,
  '/admin/categories': CategoryAdminPage,
  '/admin/tags': TagAdminPage,
  '/tools': ToolPage,
  '/projects': ProjectPage,
  '/generation-rules': GenerationRulePage,
  '/my-tests': MyTestsPage,
};

const protectedRoutes = [
  { index: true, element: <Dashboard /> },
  ...NAVIGATION_ITEMS.filter((item) => item.path !== '/').map((item) => {
    const PageComponent = pageComponents[item.path];
    return {
      path: item.path.slice(1),
      element: (
        <RouteGuard permission={item.permission} roles={item.roles}>
          {PageComponent ? <PageComponent /> : <PlaceholderPage item={item} />}
        </RouteGuard>
      ),
    };
  }),
  { path: 'test-cases/new', element: <RouteGuard permission="test_case:draft_create"><TestCaseDraftPage /></RouteGuard> },
  { path: 'test-cases/:masterId', element: <RouteGuard permission="test_case:read"><TestCaseDetailPage /></RouteGuard> },
  { path: 'test-cases/:masterId/edit', element: <RouteGuard permission="test_case:read"><TestCaseDraftPage /></RouteGuard> },
  { path: '403', element: <Forbidden /> },
  { path: '*', element: <NotFound /> },
];

const browserRouter = createBrowserRouter([
  {
    path: '/login',
    element: (
      <RouteGuard guestOnly>
        <LoginPage />
      </RouteGuard>
    ),
  },
  {
    element: (
      <RouteGuard>
        <AppLayout />
      </RouteGuard>
    ),
    children: protectedRoutes,
  },
]);

export function AppRouter() {
  return <RouterProvider router={browserRouter} />;
}

export default AppRouter;
