import { Layout, Menu, Typography } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { filterNavigation, NAVIGATION_ITEMS } from '@/shared/config/navigation';

const { Sider } = Layout;

export function AppSider() {
  const { data: user } = useCurrentUser();
  const navigate = useNavigate();
  const location = useLocation();

  const items = filterNavigation(NAVIGATION_ITEMS, user).map((item) => ({
    key: item.path,
    label: item.label,
  }));

  const selectedKey =
    NAVIGATION_ITEMS.find(
      (item) => item.path !== '/' && location.pathname.startsWith(item.path),
    )?.path ?? (location.pathname === '/' ? '/' : '');

  return (
    <Sider width={220} theme="light" style={{ borderRight: '1px solid #f0f0f0' }}>
      <div
        style={{
          height: 64,
          display: 'flex',
          alignItems: 'center',
          padding: '0 20px',
          fontWeight: 600,
          fontSize: 16,
          borderBottom: '1px solid #f0f0f0',
        }}
      >
        <Typography.Text strong style={{ fontSize: 16 }}>
          IoT Case Hub
        </Typography.Text>
      </div>
      <Menu
        mode="inline"
        selectedKeys={selectedKey ? [selectedKey] : []}
        items={items}
        style={{ borderRight: 0 }}
        onClick={({ key }) => navigate(key)}
      />
    </Sider>
  );
}

export default AppSider;
