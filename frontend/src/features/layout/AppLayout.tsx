import { Outlet } from 'react-router-dom';
import { AppSider } from '@/features/layout/AppSider';
import { AppHeader } from '@/features/layout/AppHeader';

export function AppLayout() {
  return (
    <div style={{ minHeight: '100vh', display: 'flex' }}>
      <AppSider />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        <AppHeader />
        <main style={{ flex: 1, padding: 24, overflow: 'auto' }}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default AppLayout;
