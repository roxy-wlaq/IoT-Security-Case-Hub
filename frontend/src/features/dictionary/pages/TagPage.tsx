import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Form, Input, Modal, Select, Space, Switch, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { PermissionGuard } from '@/shared/components/PermissionGuard';
import { useDebouncedValue } from '@/shared/hooks/useDebouncedValue';
import { toApiError } from '@/shared/api/apiError';
import { useCreateTag, useTags, useToggleTagEnabled, useUpdateTag } from '@/features/dictionary/hooks/useTags';
import { TAG_FORM_DEFAULTS, tagSchema } from '@/features/dictionary/schemas/tagSchema';
import type { TagFormValues } from '@/features/dictionary/schemas/tagSchema';
import type { Tag } from '@/shared/types/dictionary';

const TAG_MANAGE_PERMISSION = 'tag:manage';

interface TagFormModalProps {
  open: boolean;
  editing: Tag | null;
  onClose: () => void;
}

function TagFormModal({ open, editing, onClose }: TagFormModalProps) {
  const createMutation = useCreateTag();
  const updateMutation = useUpdateTag();

  const isEditing = Boolean(editing);

  const defaultValues: TagFormValues = useMemo(() => {
    if (editing) {
      return {
        name: editing.name,
        description: editing.description ?? '',
        enabled: editing.enabled,
      };
    }
    return TAG_FORM_DEFAULTS;
  }, [editing]);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<TagFormValues>({
    resolver: zodResolver(tagSchema),
    defaultValues,
    mode: 'onSubmit',
  });

  useEffect(() => {
    reset(defaultValues);
  }, [defaultValues, reset]);

  const enabledValue = watch('enabled');

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
      title={isEditing ? '编辑标签' : '新建标签'}
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
          label="名称"
          required
          validateStatus={errors.name ? 'error' : undefined}
          help={errors.name?.message}
        >
          <Input {...register('name')} placeholder="请输入标签名称" disabled={pending} />
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

export function TagPage() {
  const [search, setSearch] = useState('');
  const [enabledFilter, setEnabledFilter] = useState<boolean | undefined>(undefined);
  const debouncedSearch = useDebouncedValue(search);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Tag | null>(null);
  const [togglingId, setTogglingId] = useState<string | null>(null);

  const params = useMemo(
    () => ({
      search: debouncedSearch.trim() || undefined,
      enabled: enabledFilter,
    }),
    [debouncedSearch, enabledFilter],
  );

  const { data, isLoading, isError, error, refetch } = useTags(params);
  const toggleMutation = useToggleTagEnabled();

  useEffect(() => {
    if (!toggleMutation.isPending) {
      setTogglingId(null);
    }
  }, [toggleMutation.isPending]);

  const handleToggle = async (record: Tag) => {
    setTogglingId(record.id);
    try {
      await toggleMutation.mutateAsync(record.id);
    } catch {
      setTogglingId(null);
    }
  };

  const handleEdit = (record: Tag) => {
    setEditing(record);
    setModalOpen(true);
  };

  const handleCreate = () => {
    setEditing(null);
    setModalOpen(true);
  };

  const columns: ColumnsType<Tag> = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
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
      render: (_: unknown, record: Tag) => (
        <Space>
          <PermissionGuard permission={TAG_MANAGE_PERMISSION}>
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
      <Typography.Title level={3}>标签</Typography.Title>

      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search
          allowClear
          placeholder="按名称搜索"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          style={{ width: 260 }}
        />
        <Select
          allowClear
          placeholder="按状态筛选"
          value={enabledFilter}
          onChange={(value) => setEnabledFilter(value)}
          style={{ width: 140 }}
          options={[
            { value: true, label: '启用' },
            { value: false, label: '禁用' },
          ]}
        />
        <PermissionGuard permission={TAG_MANAGE_PERMISSION}>
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

      <Table<Tag>
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={isLoading}
        pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (total) => `共 ${total} 条` }}
      />

      <TagFormModal
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

export default TagPage;
