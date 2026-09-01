import { Button, Layout, Space, Tag, Typography } from 'antd';
import { LogoutOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { useLogout } from '@/features/auth/hooks/useLogout';

const { Header } = Layout;

export function AppHeader() {
  const { data: user } = useCurrentUser();
  const navigate = useNavigate();
  const logoutMutation = useLogout({
    onLoggedOut: () => navigate('/login', { replace: true }),
  });

  return (
    <Header
      style={{
        background: '#fff',
        padding: '0 24px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        borderBottom: '1px solid #f0f0f0',
      }}
    >
      <Typography.Title level={4} style={{ margin: 0 }}>
        工作台
      </Typography.Title>
      <Space size="middle">
        <span>{user?.displayName ?? user?.username}</span>
        {user?.roles?.map((role) => (
          <Tag key={role} color="blue">
            {role}
          </Tag>
        ))}
        <Button
          icon={<LogoutOutlined />}
          loading={logoutMutation.isPending}
          onClick={() => logoutMutation.mutate()}
        >
          退出登录
        </Button>
      </Space>
    </Header>
  );
}

export default AppHeader;
