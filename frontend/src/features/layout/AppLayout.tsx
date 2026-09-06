import { Layout } from 'antd';
import { Outlet } from 'react-router-dom';
import { AppSider } from '@/features/layout/AppSider';
import { AppHeader } from '@/features/layout/AppHeader';

const { Content } = Layout;

export function AppLayout() {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <AppSider />
      <Layout>
        <AppHeader />
        <Content
          style={{
            flex: 1,
            padding: 24,
            overflow: 'auto',
            background: 'var(--color-bg-page)',
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}

export default AppLayout;
