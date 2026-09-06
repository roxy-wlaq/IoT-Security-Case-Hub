import { Alert, Button, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useCallback, useMemo, useState } from 'react';
import PermissionGuard from '@/shared/components/PermissionGuard';
import { toApiError } from '@/shared/api/apiError';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import {
  useCreateUser,
  useDisableUser,
  useEnableUser,
  useResetUserPassword,
  useRoles,
  useUpdateUser,
  useUpdateUserRoles,
  useUsers,
} from '@/features/user/hooks/useUsers';
import type { UserSummary } from '@/features/user/api/userApi';
import type { PasswordResetResult } from '@/features/user/api/userApi';

/**
 * 管理员用户管理页（/users）。
 * 权限矩阵与后端冻结契约一致：
 *   user:read 列表 / user:create 创建 / user:update 编辑与重置密码 /
 *   user:disable 启停 / role:manage 角色分配。
 */
export function UserAdminPage() {
  return (
    <PermissionGuard permission="user:read" roles={['ADMIN']}>
      <UserManagementContent />
    </PermissionGuard>
  );
}

export function UserManagementContent() {
  const { data: currentUser } = useCurrentUser();
  const [filters, setFilters] = useState<{ q?: string; enabled?: boolean; role?: string }>({});
  const [page, setPage] = useState(0);
  const size = 20;

  const usersQuery = useUsers({ ...filters, page, size });
  const rolesQuery = useRoles();

  const [createOpen, setCreateOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<UserSummary | null>(null);
  const [rolesTarget, setRolesTarget] = useState<UserSummary | null>(null);
  const [resetTarget, setResetTarget] = useState<UserSummary | null>(null);
  const [secret, setSecret] = useState<PasswordResetResult | null>(null);

  const createUser = useCreateUser();
  const updateUser = useUpdateUser();
  const updateUserRoles = useUpdateUserRoles();
  const enableUser = useEnableUser();
  const disableUser = useDisableUser();
  const resetPassword = useResetUserPassword();

  const roleOptions = useMemo(
    () => (rolesQuery.data ?? []).map((role) => ({ value: role.code, label: role.name ? `${role.code}（${role.name}）` : role.code })),
    [rolesQuery.data],
  );

  const showError = useCallback((error: unknown, fallback: string) => {
    const apiError = toApiError(error);
    message.error(apiError.userMessage || fallback);
  }, []);

  const handleDisable = useCallback(
    (target: UserSummary) => {
      disableUser.mutate(target.id, { onError: (error) => showError(error, '禁用失败') });
    },
    [disableUser, showError],
  );

  const handleEnable = useCallback(
    (target: UserSummary) => {
      enableUser.mutate(target.id, { onError: (error) => showError(error, '启用失败') });
    },
    [enableUser, showError],
  );

  const columns: ColumnsType<UserSummary> = [
    { title: '用户名', dataIndex: 'username' },
    { title: '显示名称', dataIndex: 'displayName' },
    {
      title: '状态',
      dataIndex: 'enabled',
      render: (enabled: boolean) =>
        enabled ? <Tag color="green">启用</Tag> : <Tag color="red">已禁用</Tag>,
    },
    {
      title: '待改密',
      dataIndex: 'mustChangePassword',
      render: (value: boolean) => (value ? <Tag color="orange">待修改密码</Tag> : <Tag>—</Tag>),
    },
    {
      title: '角色',
      dataIndex: 'roles',
      render: (roles: string[]) => roles.map((role) => <Tag key={role}>{role}</Tag>),
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => {
        const isSelf = currentUser?.id === record.id;
        return (
          <Space size="small" wrap>
            <PermissionGuard permission="user:update">
              <Button type="link" size="small" onClick={() => setEditTarget(record)}>
                编辑
              </Button>
            </PermissionGuard>
            <PermissionGuard permission="role:manage">
              <Button type="link" size="small" onClick={() => setRolesTarget(record)}>
                角色
              </Button>
            </PermissionGuard>
            <PermissionGuard permission="user:update">
              <Button type="link" size="small" disabled={isSelf} onClick={() => setResetTarget(record)}>
                重置密码
              </Button>
            </PermissionGuard>
            <PermissionGuard permission="user:disable">
              {record.enabled ? (
                <Popconfirm
                  title={`确定禁用用户 ${record.username} 吗？`}
                  description="该用户的登录会话将被立即注销。"
                  okText="禁用"
                  cancelText="取消"
                  okButtonProps={{ danger: true }}
                  disabled={isSelf}
                  onConfirm={() => handleDisable(record)}
                >
                  <Button type="link" size="small" danger disabled={isSelf}>
                    禁用
                  </Button>
                </Popconfirm>
              ) : (
                <Button type="link" size="small" onClick={() => handleEnable(record)}>
                  启用
                </Button>
              )}
            </PermissionGuard>
          </Space>
        );
      },
    },
  ];

  return (
    <div style={{ padding: '24px 0' }}>
      <Typography.Title level={3}>用户管理</Typography.Title>

      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search
          allowClear
          placeholder="搜索用户名或显示名称"
          style={{ width: 260 }}
          onSearch={(q) => {
            setPage(0);
            setFilters((old) => ({ ...old, q: q || undefined }));
          }}
        />
        <Select
          allowClear
          placeholder="状态"
          style={{ width: 120 }}
          options={[
            { value: 'true', label: '启用' },
            { value: 'false', label: '已禁用' },
          ]}
          onChange={(value) => {
            setPage(0);
            setFilters((old) => ({ ...old, enabled: value === undefined ? undefined : value === 'true' }));
          }}
        />
        <Select
          allowClear
          placeholder="角色"
          style={{ width: 200 }}
          loading={rolesQuery.isLoading}
          options={roleOptions}
          onChange={(value) => {
            setPage(0);
            setFilters((old) => ({ ...old, role: value }));
          }}
        />
        <PermissionGuard permission="user:create">
          <Button type="primary" onClick={() => setCreateOpen(true)}>
            创建用户
          </Button>
        </PermissionGuard>
      </Space>

      {usersQuery.error ? (
        <Alert type="error" showIcon style={{ marginBottom: 16 }} message={toApiError(usersQuery.error).userMessage} />
      ) : null}

      <Table
        rowKey="id"
        loading={usersQuery.isLoading}
        dataSource={usersQuery.data?.content ?? []}
        columns={columns}
        pagination={{
          current: (usersQuery.data?.page ?? page) + 1,
          pageSize: usersQuery.data?.size ?? size,
          total: usersQuery.data?.totalElements ?? 0,
          showSizeChanger: false,
          onChange: (next) => setPage(next - 1),
        }}
      />

      <CreateUserModal
        open={createOpen}
        roleOptions={roleOptions}
        pending={createUser.isPending}
        onClose={() => setCreateOpen(false)}
        onSubmit={(values) =>
          createUser.mutate(values, {
            onSuccess: (created) => {
              setCreateOpen(false);
              if (created.generatedPassword) {
                Modal.info({
                  title: '用户创建成功',
                  content: (
                    <div>
                      <p>初始密码只显示这一次，请立即告知用户：</p>
                      <Typography.Text copyable code>{created.generatedPassword}</Typography.Text>
                      <p style={{ marginTop: 8 }}>用户首次登录时将被要求修改密码。</p>
                    </div>
                  ),
                });
              } else {
                message.success('用户创建成功');
              }
            },
            onError: (error) => showError(error, '创建失败'),
          })
        }
      />

      <EditUserModal
        open={editTarget !== null}
        target={editTarget}
        pending={updateUser.isPending}
        onClose={() => setEditTarget(null)}
        onSubmit={(values) => {
          if (!editTarget) return;
          updateUser.mutate({ id: editTarget.id, displayName: values.displayName }, {
            onSuccess: () => {
              setEditTarget(null);
              message.success('已更新显示名称');
            },
            onError: (error) => showError(error, '更新失败'),
          });
        }}
      />

      <RolesModal
        open={rolesTarget !== null}
        target={rolesTarget}
        roleOptions={roleOptions}
        pending={updateUserRoles.isPending}
        onClose={() => setRolesTarget(null)}
        onSubmit={(values) => {
          if (!rolesTarget) return;
          updateUserRoles.mutate({ id: rolesTarget.id, roles: values.roles }, {
            onSuccess: () => {
              setRolesTarget(null);
              message.success('角色已更新');
            },
            onError: (error) => showError(error, '角色更新失败'),
          });
        }}
      />

      <ResetPasswordModal
        open={resetTarget !== null}
        target={resetTarget}
        pending={resetPassword.isPending}
        onClose={() => setResetTarget(null)}
        onSubmit={(values) => {
          if (!resetTarget) return;
          resetPassword.mutate({ id: resetTarget.id, password: values.password }, {
            onSuccess: (result) => {
              setResetTarget(null);
              setSecret(result);
            },
            onError: (error) => showError(error, '重置密码失败'),
          });
        }}
      />

      <Modal
        open={secret !== null}
        title="密码重置成功"
        footer={<Button type="primary" onClick={() => setSecret(null)}>我已保存</Button>}
        onCancel={() => setSecret(null)}
      >
        {secret?.generated && secret.password ? (
          <div>
            <p>临时密码只显示这一次，请立即告知用户：</p>
            <Typography.Text copyable code>{secret.password}</Typography.Text>
          </div>
        ) : (
          <p>密码已重置。该用户下次登录时将被要求修改密码，且原有会话已全部注销。</p>
        )}
      </Modal>
    </div>
  );
}

interface CreateUserValues {
  username: string;
  displayName: string;
  password?: string;
  roles: string[];
}

function CreateUserModal({ open, roleOptions, pending, onClose, onSubmit }: {
  open: boolean;
  roleOptions: { value: string; label: string }[];
  pending: boolean;
  onClose: () => void;
  onSubmit: (values: CreateUserValues) => void;
}) {
  const [form] = Form.useForm<CreateUserValues>();
  return (
    <Modal
      open={open}
      title="创建用户"
      okText="创建"
      cancelText="取消"
      confirmLoading={pending}
      destroyOnClose
      onCancel={() => {
        form.resetFields();
        onClose();
      }}
      onOk={() => form.submit()}
    >
      <Form
        form={form}
        layout="vertical"
        requiredMark={false}
        onFinish={(values) => onSubmit({ ...values, password: values.password || undefined })}
      >
        <Form.Item
          name="username"
          label="用户名"
          rules={[
            { required: true, message: '请输入用户名' },
            { pattern: /^[a-zA-Z0-9][a-zA-Z0-9._-]*$/, message: '仅限字母、数字、点、下划线和中划线，且以字母或数字开头' },
            { min: 3, max: 100, message: '长度 3-100 个字符' },
          ]}
        >
          <Input placeholder="例如 alice" autoComplete="off" />
        </Form.Item>
        <Form.Item
          name="displayName"
          label="显示名称"
          rules={[{ required: true, message: '请输入显示名称' }, { max: 150, message: '最长 150 个字符' }]}
        >
          <Input placeholder="例如 Alice" />
        </Form.Item>
        <Form.Item
          name="password"
          label="初始密码"
          extra="留空则由系统生成随机初始密码（创建后仅显示一次）"
          rules={[{ min: 12, max: 128, message: '密码长度需为 12-128 位' }]}
        >
          <Input.Password placeholder="留空自动生成" autoComplete="new-password" />
        </Form.Item>
        <Form.Item name="roles" label="角色" rules={[{ required: true, message: '请至少选择一个角色' }]}>
          <Select mode="multiple" placeholder="选择角色" options={roleOptions} />
        </Form.Item>
      </Form>
    </Modal>
  );
}

function EditUserModal({ open, target, pending, onClose, onSubmit }: {
  open: boolean;
  target: UserSummary | null;
  pending: boolean;
  onClose: () => void;
  onSubmit: (values: { displayName: string }) => void;
}) {
  const [form] = Form.useForm<{ displayName: string }>();
  return (
    <Modal
      open={open && target !== null}
      title={`编辑用户${target ? `：${target.username}` : ''}`}
      okText="保存"
      cancelText="取消"
      confirmLoading={pending}
      destroyOnClose
      onCancel={() => {
        form.resetFields();
        onClose();
      }}
      onOk={() => form.submit()}
    >
      <Form form={form} layout="vertical" requiredMark={false}
        initialValues={target ? { displayName: target.displayName } : undefined}
        onFinish={onSubmit}
      >
        <Form.Item
          name="displayName"
          label="显示名称"
          rules={[{ required: true, message: '请输入显示名称' }, { max: 150, message: '最长 150 个字符' }]}
        >
          <Input />
        </Form.Item>
      </Form>
    </Modal>
  );
}

function RolesModal({ open, target, roleOptions, pending, onClose, onSubmit }: {
  open: boolean;
  target: UserSummary | null;
  roleOptions: { value: string; label: string }[];
  pending: boolean;
  onClose: () => void;
  onSubmit: (values: { roles: string[] }) => void;
}) {
  const [form] = Form.useForm<{ roles: string[] }>();
  return (
    <Modal
      open={open && target !== null}
      title={`分配角色${target ? `：${target.username}` : ''}`}
      okText="保存"
      cancelText="取消"
      confirmLoading={pending}
      destroyOnClose
      onCancel={() => {
        form.resetFields();
        onClose();
      }}
      onOk={() => form.submit()}
    >
      <Form form={form} layout="vertical" requiredMark={false}
        initialValues={target ? { roles: target.roles } : undefined}
        onFinish={onSubmit}
      >
        <Form.Item name="roles" label="角色" rules={[{ required: true, message: '请至少选择一个角色' }]}>
          <Select mode="multiple" placeholder="选择角色" options={roleOptions} />
        </Form.Item>
      </Form>
    </Modal>
  );
}

function ResetPasswordModal({ open, target, pending, onClose, onSubmit }: {
  open: boolean;
  target: UserSummary | null;
  pending: boolean;
  onClose: () => void;
  onSubmit: (values: { password?: string }) => void;
}) {
  const [form] = Form.useForm<{ password?: string }>();
  return (
    <Modal
      open={open && target !== null}
      title={`重置密码${target ? `：${target.username}` : ''}`}
      okText="重置"
      cancelText="取消"
      confirmLoading={pending}
      destroyOnClose
      onCancel={() => {
        form.resetFields();
        onClose();
      }}
      onOk={() => form.submit()}
    >
      <Form
        form={form}
        layout="vertical"
        requiredMark={false}
        onFinish={(values) => onSubmit({ password: values.password || undefined })}
      >
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="重置后该用户的所有登录会话将被注销，且下次登录必须修改密码。"
        />
        <Form.Item
          name="password"
          label="新密码"
          extra="留空则由系统生成随机临时密码（仅显示一次）"
          rules={[{ min: 12, max: 128, message: '密码长度需为 12-128 位' }]}
        >
          <Input.Password placeholder="留空自动生成" autoComplete="new-password" />
        </Form.Item>
      </Form>
    </Modal>
  );
}

export default UserAdminPage;
