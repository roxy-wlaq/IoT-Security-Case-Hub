import { Card, Descriptions, Tag, Typography } from 'antd';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';

export function Dashboard() {
  const { data: user } = useCurrentUser();

  return (
    <div>
      <Typography.Title level={3}>工作台</Typography.Title>
      <Card>
        <Descriptions column={1} bordered>
          <Descriptions.Item label="用户名">{user?.username}</Descriptions.Item>
          <Descriptions.Item label="显示名称">{user?.displayName}</Descriptions.Item>
          <Descriptions.Item label="状态">
            {user?.enabled ? '启用' : '禁用'}
          </Descriptions.Item>
          <Descriptions.Item label="角色">
            {user?.roles?.length ? (
              user.roles.map((role) => (
                <Tag key={role} color="blue">
                  {role}
                </Tag>
              ))
            ) : (
              <span>—</span>
            )}
          </Descriptions.Item>
          <Descriptions.Item label="权限数量">
            {user?.permissions?.length ?? 0}
          </Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  );
}

export default Dashboard;
