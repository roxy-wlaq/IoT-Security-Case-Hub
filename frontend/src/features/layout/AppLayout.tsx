import { Outlet } from 'react-router-dom';
import { Card } from 'antd';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { ChangePasswordForm } from '@/features/auth/ChangePasswordForm';
import { AppSider } from '@/features/layout/AppSider';
import { AppHeader } from '@/features/layout/AppHeader';

export function AppLayout() {
  const { data: user } = useCurrentUser();

  // 强制改密门：未改密前不允许进入任何业务页面。
  if (user?.mustChangePassword) {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: '#f0f2f5',
          padding: 16,
        }}
      >
        <Card style={{ width: 420 }} title="修改密码">
          {user ? (
            <ChangePasswordForm user={user} onSuccess={() => window.location.assign('/')} />
          ) : null}
        </Card>
      </div>
    );
  }

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
