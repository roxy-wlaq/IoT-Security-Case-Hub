import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, Tag, TreeSelect, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { PermissionGuard } from '@/shared/components/PermissionGuard';
import { useDebouncedValue } from '@/shared/hooks/useDebouncedValue';
import { toApiError } from '@/shared/api/apiError';
import {
  useCategories,
  useCreateCategory,
  useToggleCategoryEnabled,
  useUpdateCategory,
} from '@/features/dictionary/hooks/useCategories';
import { CATEGORY_FORM_DEFAULTS, categorySchema } from '@/features/dictionary/schemas/categorySchema';
import type { CategoryFormValues } from '@/features/dictionary/schemas/categorySchema';
import type { Category, CategoryLevel } from '@/shared/types/dictionary';

const CATEGORY_MANAGE_PERMISSION = 'category:manage';

interface CategoryFormModalProps {
  open: boolean;
  editing: Category | null;
  /** 一级分类列表，供二级分类选择父分类 */
  parentOptions: Category[];
  onClose: () => void;
}

function CategoryFormModal({ open, editing, parentOptions, onClose }: CategoryFormModalProps) {
  const createMutation = useCreateCategory();
  const updateMutation = useUpdateCategory();

  const isEditing = Boolean(editing);

  const defaultValues: CategoryFormValues = useMemo(() => {
    if (editing) {
      return {
        code: editing.code,
        name: editing.name,
        level: editing.level,
        parentId: editing.parentId ?? null,
        description: editing.description ?? '',
        sortOrder: editing.sortOrder,
        enabled: editing.enabled,
      };
    }
    return CATEGORY_FORM_DEFAULTS;
  }, [editing]);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
    defaultValues,
    mode: 'onSubmit',
  });

  useEffect(() => {
    reset(defaultValues);
  }, [defaultValues, reset]);

  const levelValue = watch('level');
  const parentIdValue = watch('parentId');
  const enabledValue = watch('enabled');
  const sortOrderValue = watch('sortOrder');

  const pending = createMutation.isPending || updateMutation.isPending;
  const submitError = toApiError(createMutation.error ?? updateMutation.error);

  const onSubmit = handleSubmit(async (values) => {
    const payload = {
      ...values,
      parentId: values.level === 2 ? values.parentId : null,
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

  const treeData = useMemo(
    () =>
      parentOptions.map((parent) => ({
        title: `${parent.name} (${parent.code})`,
        value: parent.id,
        key: parent.id,
      })),
    [parentOptions],
  );

  return (
    <Modal
      title={isEditing ? '编辑分类' : '新建分类'}
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
          label="层级"
          required
          validateStatus={errors.level ? 'error' : undefined}
          help={errors.level?.message}
        >
          <Select<CategoryLevel>
            value={levelValue}
            onChange={(value) => {
              setValue('level', value, { shouldValidate: true });
              if (value === 1) {
                setValue('parentId', null, { shouldValidate: true });
              }
            }}
            disabled={pending}
            options={[
              { value: 1, label: '一级分类' },
              { value: 2, label: '二级分类' },
            ]}
          />
        </Form.Item>

        {levelValue === 2 ? (
          <Form.Item
            label="父分类"
            required
            validateStatus={errors.parentId ? 'error' : undefined}
            help={errors.parentId?.message}
          >
            <TreeSelect
              value={parentIdValue ?? undefined}
              onChange={(value: string | null) => setValue('parentId', value ?? null, { shouldValidate: true })}
              treeData={treeData}
              placeholder="请选择一级父分类"
              treeDefaultExpandAll
              allowClear
              disabled={pending}
            />
          </Form.Item>
        ) : null}

        <Form.Item
          label="编码"
          required
          validateStatus={errors.code ? 'error' : undefined}
          help={errors.code?.message}
        >
          <Input {...register('code')} placeholder="例如 CAT-001" disabled={pending} />
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
          label="排序"
          validateStatus={errors.sortOrder ? 'error' : undefined}
          help={errors.sortOrder?.message}
        >
          <InputNumber
            value={sortOrderValue}
            onChange={(value) => setValue('sortOrder', Number(value ?? 0), { shouldValidate: true })}
            min={0}
            precision={0}
            disabled={pending}
            style={{ width: '100%' }}
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

export function CategoryPage() {
  const [search, setSearch] = useState('');
  const [enabledFilter, setEnabledFilter] = useState<boolean | undefined>(undefined);
  const debouncedSearch = useDebouncedValue(search);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Category | null>(null);
  const [togglingId, setTogglingId] = useState<string | null>(null);

  const params = useMemo(
    () => ({
      search: debouncedSearch.trim() || undefined,
      enabled: enabledFilter,
    }),
    [debouncedSearch, enabledFilter],
  );

  const { data, isLoading, isError, error, refetch } = useCategories(params);
  const toggleMutation = useToggleCategoryEnabled();

  useEffect(() => {
    if (!toggleMutation.isPending) {
      setTogglingId(null);
    }
  }, [toggleMutation.isPending]);

  const handleToggle = async (record: Category) => {
    setTogglingId(record.id);
    try {
      await toggleMutation.mutateAsync(record.id);
    } catch {
      setTogglingId(null);
    }
  };

  const handleEdit = (record: Category) => {
    setEditing(record);
    setModalOpen(true);
  };

  const handleCreate = () => {
    setEditing(null);
    setModalOpen(true);
  };

  const parentOptions = useMemo(() => (data ?? []).filter((item) => item.level === 1), [data]);

  const columns: ColumnsType<Category> = [
    { title: '编码', dataIndex: 'code', key: 'code', width: 160 },
    { title: '名称', dataIndex: 'name', key: 'name' },
    {
      title: '层级',
      dataIndex: 'level',
      key: 'level',
      width: 100,
      render: (level: CategoryLevel) => (level === 1 ? <Tag color="geekblue">一级</Tag> : <Tag color="orange">二级</Tag>),
    },
    { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
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
      render: (_: unknown, record: Category) => (
        <Space>
          <PermissionGuard permission={CATEGORY_MANAGE_PERMISSION}>
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
      <Typography.Title level={3}>分类</Typography.Title>

      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search
          allowClear
          placeholder="按编码或名称搜索"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          style={{ width: 260 }}
        />
        <Select<boolean>
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
        <PermissionGuard permission={CATEGORY_MANAGE_PERMISSION}>
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

      <Table<Category>
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={isLoading}
        pagination={false}
        expandable={{ childrenColumnName: 'children' }}
      />

      <CategoryFormModal
        open={modalOpen}
        editing={editing}
        parentOptions={parentOptions}
        onClose={() => {
          setModalOpen(false);
          setEditing(null);
        }}
      />
    </div>
  );
}

export default CategoryPage;
