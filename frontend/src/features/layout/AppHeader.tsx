import { Avatar, Button, Dropdown, Layout, Space, theme, Typography } from 'antd';
import { DownOutlined, LogoutOutlined, MoonOutlined, SunOutlined, UserOutlined } from '@ant-design/icons';
import { useLocation, useNavigate } from 'react-router-dom';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { useLogout } from '@/features/auth/hooks/useLogout';
import { useThemeMode } from '@/shared/contexts/ThemeModeContext';
import { findNavigationItem, NAVIGATION_ITEMS } from '@/shared/config/navigation';

const { Header } = Layout;

export function AppHeader() {
  const { data: user } = useCurrentUser();
  const navigate = useNavigate();
  const location = useLocation();
  const { mode, toggle } = useThemeMode();
  const { token } = theme.useToken();
  const logoutMutation = useLogout({
    onLoggedOut: () => navigate('/login', { replace: true }),
  });

  const current =
    NAVIGATION_ITEMS.find(
      (item) => item.path !== '/' && location.pathname.startsWith(item.path),
    ) ?? findNavigationItem('/');

  return (
    <Header
      style={{
        background: 'var(--color-bg-layout)',
        padding: '0 24px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderBottom: '1px solid var(--color-border-secondary)',
        height: 64,
      }}
    >
      <Typography.Title level={4} style={{ margin: 0 }}>
        {current?.label ?? '工作台'}
      </Typography.Title>
      <Space size="middle">
        <Button
          type="text"
          icon={mode === 'dark' ? <SunOutlined /> : <MoonOutlined />}
          onClick={toggle}
          aria-label="切换明暗主题"
        />
        <Dropdown
          menu={{
            items: [
              {
                key: 'logout',
                icon: <LogoutOutlined />,
                label: '退出登录',
              },
            ],
            onClick: ({ key }) => key === 'logout' && logoutMutation.mutate(),
          }}
        >
          <Space style={{ cursor: 'pointer' }} size="small">
            <Avatar size="small" icon={<UserOutlined />} />
            <span>{user?.displayName ?? user?.username}</span>
            <DownOutlined style={{ fontSize: 10, color: token.colorTextTertiary }} />
          </Space>
        </Dropdown>
      </Space>
    </Header>
  );
}

export default AppHeader;
