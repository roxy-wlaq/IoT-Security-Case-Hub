import { zodResolver } from '@hookform/resolvers/zod';
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Row,
  Space,
  Spin,
  Tag,
  Tree,
  TreeSelect,
  Typography,
} from 'antd';
import type { DataNode } from 'antd/es/tree';
import { useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { toApiError } from '@/shared/api/apiError';
import { PermissionGuard } from '@/shared/components/PermissionGuard';
import { useCreateCapability, useDisableCapability, useEnableCapability, useCapabilityTree, useUpdateCapability } from '@/features/capability/hooks/useCapabilities';
import { CAPABILITY_FORM_DEFAULTS, capabilitySchema } from '@/features/capability/schemas/capabilitySchema';
import type { CapabilityFormValues } from '@/features/capability/schemas/capabilitySchema';
import type { Capability, CapabilityTreeNode } from '@/shared/types/capability';

const CAPABILITY_MANAGE_PERMISSION = 'capability:manage_library';

interface CapabilityFormModalProps {
  open: boolean;
  /** 编辑目标；null 表示新建 */
  editing: Capability | null;
  /** 新建子能力时的默认父节点；新建 Root 时为 null */
  defaultParent: Capability | null;
  /** 全部能力（扁平），供编辑时选择新的父节点 */
  allCapabilities: Capability[];
  onClose: () => void;
}

function CapabilityFormModal({ open, editing, defaultParent, allCapabilities, onClose }: CapabilityFormModalProps) {
  const createMutation = useCreateCapability();
  const updateMutation = useUpdateCapability();

  const isEditing = Boolean(editing);

  const defaultValues: CapabilityFormValues = useMemo(() => {
    if (editing) {
      return {
        code: editing.code,
        name: editing.name,
        description: editing.description ?? '',
        sortOrder: editing.sortOrder,
      };
    }
    return CAPABILITY_FORM_DEFAULTS;
  }, [editing]);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<CapabilityFormValues>({
    resolver: zodResolver(capabilitySchema),
    defaultValues,
    mode: 'onSubmit',
  });

  useEffect(() => {
    reset(defaultValues);
  }, [defaultValues, reset]);

  // 新建时的父节点：来自 defaultParent（Add Child），或用户在 TreeSelect 里改选；null = Root
  const [parentId, setParentId] = useState<string | null>(defaultParent?.id ?? null);

  useEffect(() => {
    if (open) {
      setParentId(defaultParent?.id ?? null);
    }
  }, [open, defaultParent]);

  const descriptionValue = watch('description');
  const sortOrderValue = watch('sortOrder');

  const pending = createMutation.isPending || updateMutation.isPending;
  const submitError = toApiError(createMutation.error ?? updateMutation.error);

  const onSubmit = handleSubmit(async (values) => {
    const payload = {
      code: values.code,
      name: values.name,
      description: values.description?.trim() ? values.description.trim() : null,
      sortOrder: values.sortOrder,
    };
    try {
      if (isEditing && editing) {
        // PUT 为整体替换：parentId 决定节点归属，不传即移回根节点
        await updateMutation.mutateAsync({ id: editing.id, payload: { ...payload, parentId } });
      } else {
        await createMutation.mutateAsync({ ...payload, parentId });
      }
      onClose();
    } catch {
      // 错误已由 mutation.error 承载，并在 Modal 中展示
    }
  });

  // 父节点候选：新建时为全部能力；编辑时排除自己（把自己设为自己的 parent 由后端拒绝，前端直接不给选）
  const parentOptions = useMemo(() => {
    const candidates = isEditing && editing ? allCapabilities.filter((c) => c.id !== editing.id) : allCapabilities;
    return candidates.map((c) => ({
      title: `${c.name} (${c.code})`,
      value: c.id,
    }));
  }, [allCapabilities, editing, isEditing]);

  return (
    <Modal
      title={isEditing ? '编辑能力' : defaultParent ? `新建子能力（父：${defaultParent.name}）` : '新建根能力'}
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
        <Form.Item label="父能力" extra="留空表示根能力（Root Capability）">
          <TreeSelect
            allowClear
            placeholder="不选择即为根能力"
            value={parentId ?? undefined}
            onChange={(value) => setParentId(value ?? null)}
            treeData={parentOptions}
            treeDefaultExpandAll
            disabled={pending}
          />
        </Form.Item>

        <Form.Item label="编码（Code）" required validateStatus={errors.code ? 'error' : undefined} help={errors.code?.message}>
          <Input {...register('code')} placeholder="例如 BLUETOOTH_BLE_GATT" disabled={pending} />
        </Form.Item>

        <Form.Item label="名称（Name）" required validateStatus={errors.name ? 'error' : undefined} help={errors.name?.message}>
          <Input {...register('name')} placeholder="例如 GATT" disabled={pending} />
        </Form.Item>

        <Form.Item label="描述（Description）" validateStatus={errors.description ? 'error' : undefined} help={errors.description?.message}>
          <Input.TextArea rows={3} {...register('description')} value={descriptionValue} disabled={pending} />
        </Form.Item>

        <Form.Item label="排序（Sort Order）" validateStatus={errors.sortOrder ? 'error' : undefined} help={errors.sortOrder?.message}>
          <InputNumber
            min={0}
            max={9999}
            precision={0}
            value={sortOrderValue}
            onChange={(value) => setValue('sortOrder', value ?? 0, { shouldValidate: true })}
            disabled={pending}
            style={{ width: '100%' }}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}

function toTreeData(nodes: CapabilityTreeNode[]): DataNode[] {
  return nodes.map((node) => ({
    key: node.id,
    title: (
      <Space size={8}>
        <span>{node.name}</span>
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          {node.code}
        </Typography.Text>
        {!node.enabled ? <Tag color="default">已停用</Tag> : null}
      </Space>
    ),
    children: node.children.length > 0 ? toTreeData(node.children) : undefined,
  }));
}

function flattenTree(nodes: CapabilityTreeNode[], acc: Capability[] = []): Capability[] {
  for (const node of nodes) {
    acc.push(node);
    flattenTree(node.children, acc);
  }
  return acc;
}

/** 找到包含给定 id 的节点（含自身）。 */
function findNode(nodes: CapabilityTreeNode[], id: string): CapabilityTreeNode | null {
  for (const node of nodes) {
    if (node.id === id) {
      return node;
    }
    const found = findNode(node.children, id);
    if (found) {
      return found;
    }
  }
  return null;
}

export function CapabilityAdminPage() {
  const treeQuery = useCapabilityTree();
  const enableMutation = useEnableCapability();
  const disableMutation = useDisableCapability();

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Capability | null>(null);
  const [defaultParent, setDefaultParent] = useState<Capability | null>(null);

  const tree = useMemo(() => treeQuery.data ?? [], [treeQuery.data]);
  const allCapabilities = useMemo(() => flattenTree(tree), [tree]);
  const selected = selectedId ? findNode(tree, selectedId) : null;

  const togglePending = enableMutation.isPending || disableMutation.isPending;
  const toggleError = toApiError(enableMutation.error ?? disableMutation.error);

  const openCreateRoot = () => {
    setEditing(null);
    setDefaultParent(null);
    setFormOpen(true);
  };

  const openCreateChild = (parent: Capability) => {
    setEditing(null);
    setDefaultParent(parent);
    setFormOpen(true);
  };

  const openEdit = (capability: Capability) => {
    setEditing(capability);
    setDefaultParent(null);
    setFormOpen(true);
  };

  const handleToggle = async (capability: Capability) => {
    try {
      if (capability.enabled) {
        await disableMutation.mutateAsync(capability.id);
      } else {
        await enableMutation.mutateAsync(capability.id);
      }
    } catch {
      // 错误由 toggleError 承载并在页面顶部展示
    }
  };

  const treeData = useMemo(() => toTreeData(tree), [tree]);

  if (treeQuery.isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: 64 }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div>
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Space align="center" style={{ justifyContent: 'space-between', width: '100%' }}>
          <div>
            <Typography.Title level={4} style={{ margin: 0 }}>
              能力库
            </Typography.Title>
            <Typography.Text type="secondary">
              设备能力的全局定义树。能力与分类是两棵独立的树；这里不涉及任何项目上的安全结论。
            </Typography.Text>
          </div>
          <PermissionGuard permission={CAPABILITY_MANAGE_PERMISSION}>
            <Button type="primary" onClick={openCreateRoot}>
              新建根能力
            </Button>
          </PermissionGuard>
        </Space>

        {toggleError ? <Alert type="error" showIcon message={toggleError.userMessage} closable /> : null}
        {treeQuery.isError ? (
          <Alert type="error" showIcon message={toApiError(treeQuery.error)?.userMessage ?? '能力树加载失败'} />
        ) : null}

        <Row gutter={16}>
          <Col xs={24} lg={10}>
            <Card title="能力树" size="small">
              {tree.length === 0 ? (
                <Empty description="暂无能力，点击右上角新建根能力" />
              ) : (
                <Tree
                  blockNode
                  defaultExpandAll
                  treeData={treeData}
                  selectedKeys={selectedId ? [selectedId] : []}
                  onSelect={(keys) => setSelectedId((keys[0] as string | undefined) ?? null)}
                />
              )}
            </Card>
          </Col>

          <Col xs={24} lg={14}>
            <Card title="详情" size="small">
              {!selected ? (
                <Empty description="在左侧选择一个能力查看详情" />
              ) : (
                <Space direction="vertical" size={16} style={{ width: '100%' }}>
                  <Descriptions
                    column={1}
                    size="small"
                    bordered
                    items={[
                      { key: 'code', label: 'Code', children: selected.code },
                      { key: 'name', label: 'Name', children: selected.name },
                      {
                        key: 'description',
                        label: 'Description',
                        children: selected.description || <Typography.Text type="secondary">（无）</Typography.Text>,
                      },
                      {
                        key: 'enabled',
                        label: 'Enabled',
                        children: selected.enabled ? <Tag color="green">启用</Tag> : <Tag color="default">停用</Tag>,
                      },
                      { key: 'sortOrder', label: 'Sort Order', children: selected.sortOrder },
                      { key: 'createdAt', label: 'Created At', children: new Date(selected.createdAt).toLocaleString() },
                      { key: 'updatedAt', label: 'Updated At', children: new Date(selected.updatedAt).toLocaleString() },
                    ]}
                  />

                  <PermissionGuard permission={CAPABILITY_MANAGE_PERMISSION}>
                    <Space wrap>
                      <Button onClick={() => openCreateChild(selected)}>添加子能力</Button>
                      <Button type="primary" onClick={() => openEdit(selected)}>
                        编辑
                      </Button>
                      <Button danger={selected.enabled} onClick={() => handleToggle(selected)} loading={togglePending}>
                        {selected.enabled ? '停用' : '启用'}
                      </Button>
                    </Space>
                  </PermissionGuard>
                  <Typography.Text type="secondary" style={{ display: 'block' }}>
                    停用不会物理删除能力，历史引用仍然有效。
                  </Typography.Text>
                </Space>
              )}
            </Card>
          </Col>
        </Row>
      </Space>

      <CapabilityFormModal
        open={formOpen}
        editing={editing}
        defaultParent={defaultParent}
        allCapabilities={allCapabilities}
        onClose={() => {
          setFormOpen(false);
          setEditing(null);
          setDefaultParent(null);
        }}
      />
    </div>
  );
}

export default CapabilityAdminPage;
