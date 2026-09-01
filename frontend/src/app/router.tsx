import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { RouteGuard } from '@/shared/components/RouteGuard';
import { AppLayout } from '@/features/layout/AppLayout';
import { LoginPage } from '@/features/auth/LoginPage';
import { Dashboard } from '@/pages/Dashboard';
import { PlaceholderPage } from '@/pages/PlaceholderPage';
import { Forbidden } from '@/pages/Forbidden';
import { NotFound } from '@/pages/NotFound';
import { NAVIGATION_ITEMS } from '@/shared/config/navigation';

const protectedRoutes = [
  { index: true, element: <Dashboard /> },
  ...NAVIGATION_ITEMS.filter((item) => item.path !== '/').map((item) => ({
    path: item.path.slice(1),
    element: (
      <RouteGuard permission={item.permission} roles={item.roles}>
        <PlaceholderPage item={item} />
      </RouteGuard>
    ),
  })),
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
