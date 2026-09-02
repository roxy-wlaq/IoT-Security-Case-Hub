import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Form, Input, Modal, Select, Space, Switch, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { PermissionGuard } from '@/shared/components/PermissionGuard';
import { useDebouncedValue } from '@/shared/hooks/useDebouncedValue';
import { toApiError } from '@/shared/api/apiError';
import { useCreateStandard, useStandards, useUpdateStandard } from '@/features/dictionary/hooks/useStandards';
import { STANDARD_FORM_DEFAULTS, standardSchema } from '@/features/dictionary/schemas/standardSchema';
import type { StandardFormValues } from '@/features/dictionary/schemas/standardSchema';
import type { StandardTaskType, StandardType } from '@/shared/types/dictionary';

const STANDARD_MANAGE_PERMISSION = 'standard:manage';

/** AntD Select 的 value 只接受 string | number | null，用字符串值承载布尔筛选。 */
type EnabledFilter = 'enabled' | 'disabled';

const TYPE_OPTIONS = [
  { value: 'STANDARD', label: '标准 (STANDARD)' },
  { value: 'TASK_TYPE', label: '任务类型 (TASK_TYPE)' },
];

const ENABLED_FILTER_OPTIONS = [
  { value: 'enabled', label: '启用' },
  { value: 'disabled', label: '禁用' },
];

interface StandardFormModalProps {
  open: boolean;
  editing: StandardTaskType | null;
  onClose: () => void;
}

function StandardFormModal({ open, editing, onClose }: StandardFormModalProps) {
  const createMutation = useCreateStandard();
  const updateMutation = useUpdateStandard();

  const isEditing = Boolean(editing);

  const defaultValues: StandardFormValues = useMemo(() => {
    if (editing) {
      return {
        code: editing.code,
        name: editing.name,
        type: editing.type,
        description: editing.description ?? '',
        enabled: editing.enabled,
      };
    }
    return STANDARD_FORM_DEFAULTS;
  }, [editing]);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<StandardFormValues>({
    resolver: zodResolver(standardSchema),
    defaultValues,
    mode: 'onSubmit',
  });

  useEffect(() => {
    reset(defaultValues);
  }, [defaultValues, reset]);

  const enabledValue = watch('enabled');
  const typeValue = watch('type');

  const pending = createMutation.isPending || updateMutation.isPending;
  const submitError = toApiError(createMutation.error ?? updateMutation.error);

  const onSubmit = handleSubmit(async (values) => {
    const payload = {
      ...values,
      description: values.description?.trim() ? values.description.trim() : undefined,
    };
    try {
      if (isEditing && editing) {
        await updateMutation.mutateAsync({ id: editing.id, payload });
      } else {
        await createMutation.mutateAsync(payload);
      }
      onClose();
    } catch {
      // 错误已由 mutation.error 承载
    }
  });

  return (
    <Modal
      title={isEditing ? '编辑标准任务类型' : '新建标准任务类型'}
      open={open}
      onCancel={onClose}
      confirmLoading={pending}
      onOk={onSubmit}
      okText={isEditing ? '保存' : '创建'}
      cancelText="取消"
      destroyOnClose
      maskClosable={false}
    >
      {submitError ? (
        <Alert style={{ marginBottom: 16 }} type="error" showIcon message={submitError.userMessage} />
      ) : null}

      <Form layout="vertical" requiredMark>
        <Form.Item
          label="编码"
          required
          validateStatus={errors.code ? 'error' : undefined}
          help={errors.code?.message}
        >
          <Input {...register('code')} placeholder="例如 STD-001" disabled={pending} />
        </Form.Item>

        <Form.Item
          label="名称"
          required
          validateStatus={errors.name ? 'error' : undefined}
          help={errors.name?.message}
        >
          <Input {...register('name')} placeholder="请输入名称" disabled={pending} />
        </Form.Item>

        <Form.Item
          label="类型"
          required
          validateStatus={errors.type ? 'error' : undefined}
          help={errors.type?.message}
        >
          <Select<StandardType>
            value={typeValue}
            onChange={(value) => setValue('type', value, { shouldValidate: true })}
            disabled={pending}
            options={TYPE_OPTIONS}
          />
        </Form.Item>

        <Form.Item
          label="描述"
          validateStatus={errors.description ? 'error' : undefined}
          help={errors.description?.message}
        >
          <Input.TextArea {...register('description')} rows={3} placeholder="可选" disabled={pending} />
        </Form.Item>

        <Form.Item label="启用状态">
          <Switch
            checked={enabledValue}
            onChange={(checked) => setValue('enabled', checked)}
            disabled={pending}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}

export function StandardAdminPage() {
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<StandardType | undefined>(undefined);
  const [enabledFilter, setEnabledFilter] = useState<EnabledFilter | undefined>(undefined);
  const debouncedSearch = useDebouncedValue(search);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<StandardTaskType | null>(null);
  const [togglingId, setTogglingId] = useState<string | null>(null);

  const params = useMemo(
    () => ({
      search: debouncedSearch.trim() || undefined,
      type: typeFilter,
      enabled: enabledFilter === 'enabled' ? true : enabledFilter === 'disabled' ? false : undefined,
    }),
    [debouncedSearch, typeFilter, enabledFilter],
  );

  const { data, isLoading, isError, error, refetch } = useStandards(params);
  const updateMutation = useUpdateStandard();

  useEffect(() => {
    if (!updateMutation.isPending) {
      setTogglingId(null);
    }
  }, [updateMutation.isPending]);

  const handleToggle = async (record: StandardTaskType) => {
    setTogglingId(record.id);
    try {
      await updateMutation.mutateAsync({ id: record.id, payload: { enabled: !record.enabled } });
    } catch {
      setTogglingId(null);
    }
  };

  const handleEdit = (record: StandardTaskType) => {
    setEditing(record);
    setModalOpen(true);
  };

  const handleCreate = () => {
    setEditing(null);
    setModalOpen(true);
  };

  const columns: ColumnsType<StandardTaskType> = [
    { title: '编码', dataIndex: 'code', key: 'code', width: 160 },
    { title: '名称', dataIndex: 'name', key: 'name' },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 140,
      render: (type: StandardType) => <Tag color={type === 'STANDARD' ? 'blue' : 'purple'}>{type}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 90,
      render: (enabled: boolean) => (enabled ? <Tag color="green">启用</Tag> : <Tag>禁用</Tag>),
    },
    {
      title: '操作',
      key: 'actions',
      width: 180,
      render: (_: unknown, record: StandardTaskType) => (
        <Space>
          <PermissionGuard permission={STANDARD_MANAGE_PERMISSION}>
            <Button type="link" size="small" onClick={() => handleEdit(record)} disabled={togglingId === record.id}>
              编辑
            </Button>
            <Button
              type="link"
              size="small"
              onClick={() => void handleToggle(record)}
              loading={togglingId === record.id}
            >
              {record.enabled ? '禁用' : '启用'}
            </Button>
          </PermissionGuard>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Typography.Title level={3}>标准任务类型</Typography.Title>

      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search
          allowClear
          placeholder="按编码或名称搜索"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          style={{ width: 260 }}
        />
        <Select<StandardType>
          allowClear
          placeholder="按类型筛选"
          value={typeFilter}
          onChange={(value) => setTypeFilter(value)}
          style={{ width: 180 }}
          options={TYPE_OPTIONS}
        />
        <Select<EnabledFilter>
          allowClear
          placeholder="按状态筛选"
          value={enabledFilter}
          onChange={(value) => setEnabledFilter(value)}
          style={{ width: 140 }}
          options={ENABLED_FILTER_OPTIONS}
        />
        <PermissionGuard permission={STANDARD_MANAGE_PERMISSION}>
          <Button type="primary" onClick={handleCreate}>
            新建
          </Button>
        </PermissionGuard>
      </Space>

      {isError ? (
        <Alert
          type="error"
          showIcon
          message="加载失败"
          description={toApiError(error).userMessage}
          action={
            <Button size="small" onClick={() => void refetch()}>
              重试
            </Button>
          }
        />
      ) : null}

      <Table<StandardTaskType>
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={isLoading}
        pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (total) => `共 ${total} 条` }}
      />

      <StandardFormModal
        open={modalOpen}
        editing={editing}
        onClose={() => {
          setModalOpen(false);
          setEditing(null);
        }}
      />
    </div>
  );
}

export default StandardAdminPage;
