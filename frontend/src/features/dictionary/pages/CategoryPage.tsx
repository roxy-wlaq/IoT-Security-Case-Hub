import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, Tag, TreeSelect, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { PermissionGuard } from '@/shared/components/PermissionGuard';
import { useDebouncedValue } from '@/shared/hooks/useDebouncedValue';
import { toApiError } from '@/shared/api/apiError';
import { useCategories, useCreateCategory, useUpdateCategory } from '@/features/dictionary/hooks/useCategories';
import { CATEGORY_FORM_DEFAULTS, categorySchema } from '@/features/dictionary/schemas/categorySchema';
import type { CategoryFormValues } from '@/features/dictionary/schemas/categorySchema';
import type { Category, CategoryLevel } from '@/shared/types/dictionary';

const CATEGORY_MANAGE_PERMISSION = 'category:manage';

/** AntD Select 的 value 只接受 string | number | null，用字符串值承载布尔筛选。 */
type EnabledFilter = 'enabled' | 'disabled';

const ENABLED_FILTER_OPTIONS = [
  { value: 'enabled', label: '启用' },
  { value: 'disabled', label: '禁用' },
];

interface CategoryFormModalProps {
  open: boolean;
  editing: Category | null;
  /** 新建二级分类时预设的父分类；新建一级/编辑时为 null */
  presetParent: Category | null;
  /** 一级分类列表，供二级分类选择父分类 */
  parentOptions: Category[];
  onClose: () => void;
}

function CategoryFormModal({ open, editing, presetParent, parentOptions, onClose }: CategoryFormModalProps) {
  const createMutation = useCreateCategory();
  const updateMutation = useUpdateCategory();

  const isEditing = Boolean(editing);

  /**
   * level 永远由上下文推导（编辑对象本身 / 预设父分类），表单里没有任何 level 输入；
   * 服务端会根据 parentId 最终裁决。
   */
  const level: CategoryLevel = editing ? editing.level : presetParent ? 2 : 1;

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
    if (presetParent) {
      return { ...CATEGORY_FORM_DEFAULTS, level: 2, parentId: presetParent.id };
    }
    return CATEGORY_FORM_DEFAULTS;
  }, [editing, presetParent]);

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

  const parentIdValue = watch('parentId');
  const enabledValue = watch('enabled');
  const sortOrderValue = watch('sortOrder');

  const pending = createMutation.isPending || updateMutation.isPending;
  const submitError = toApiError(createMutation.error ?? updateMutation.error);

  const onSubmit = handleSubmit(async (values) => {
    // 不发送 level：服务端从 parentId 推导
    const payload = {
      parentId: level === 2 ? values.parentId : null,
      code: values.code,
      name: values.name,
      description: values.description?.trim() ? values.description.trim() : undefined,
      sortOrder: values.sortOrder,
      enabled: values.enabled,
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
      title={
        isEditing
          ? `编辑${level === 1 ? '一级' : '二级'}分类`
          : level === 2
            ? `在「${presetParent?.name ?? ''}」下新建二级分类`
            : '新建一级分类'
      }
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
        {level === 2 ? (
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

export function CategoryAdminPage() {
  const [search, setSearch] = useState('');
  const [enabledFilter, setEnabledFilter] = useState<EnabledFilter | undefined>(undefined);
  const debouncedSearch = useDebouncedValue(search);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Category | null>(null);
  const [presetParent, setPresetParent] = useState<Category | null>(null);
  const [togglingId, setTogglingId] = useState<string | null>(null);

  const params = useMemo(
    () => ({
      search: debouncedSearch.trim() || undefined,
      enabled: enabledFilter === 'enabled' ? true : enabledFilter === 'disabled' ? false : undefined,
    }),
    [debouncedSearch, enabledFilter],
  );

  const { data, isLoading, isError, error, refetch } = useCategories(params);
  const updateMutation = useUpdateCategory();

  useEffect(() => {
    if (!updateMutation.isPending) {
      setTogglingId(null);
    }
  }, [updateMutation.isPending]);

  const handleToggle = async (record: Category) => {
    setTogglingId(record.id);
    try {
      await updateMutation.mutateAsync({ id: record.id, payload: { enabled: !record.enabled } });
    } catch {
      setTogglingId(null);
    }
  };

  const handleEdit = (record: Category) => {
    setEditing(record);
    setPresetParent(null);
    setModalOpen(true);
  };

  const handleCreateRoot = () => {
    setEditing(null);
    setPresetParent(null);
    setModalOpen(true);
  };

  const handleCreateChild = (parent: Category) => {
    setEditing(null);
    setPresetParent(parent);
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
      width: 240,
      render: (_: unknown, record: Category) => (
        <Space>
          <PermissionGuard permission={CATEGORY_MANAGE_PERMISSION}>
            <Button type="link" size="small" onClick={() => handleEdit(record)} disabled={togglingId === record.id}>
              编辑
            </Button>
            {record.level === 1 ? (
              <Button type="link" size="small" onClick={() => handleCreateChild(record)}>
                新增二级
              </Button>
            ) : null}
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
        <Select<EnabledFilter>
          allowClear
          placeholder="按状态筛选"
          value={enabledFilter}
          onChange={(value) => setEnabledFilter(value)}
          style={{ width: 140 }}
          options={ENABLED_FILTER_OPTIONS}
        />
        <PermissionGuard permission={CATEGORY_MANAGE_PERMISSION}>
          <Button type="primary" onClick={handleCreateRoot}>
            新建一级分类
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
        expandable={{ childrenColumnName: 'children', defaultExpandAllRows: true }}
      />

      <CategoryFormModal
        open={modalOpen}
        editing={editing}
        presetParent={presetParent}
        parentOptions={parentOptions}
        onClose={() => {
          setModalOpen(false);
          setEditing(null);
          setPresetParent(null);
        }}
      />
    </div>
  );
}

export default CategoryAdminPage;
