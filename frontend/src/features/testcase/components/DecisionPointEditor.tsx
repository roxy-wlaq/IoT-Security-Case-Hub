import { Alert, Button, Card, Empty, Input, List, Modal, Select, Space, Tag, Typography, message } from 'antd';
import { Background, Controls, MiniMap, ReactFlow } from '@xyflow/react';
import type { Edge, Node } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { useMemo, useState } from 'react';
import { toApiError } from '@/shared/api/apiError';
import {
  useCreateDecisionPoint,
  useDeleteDecisionPoint,
  useDecisionPoints,
  useMasterLogicGraph,
  useUpdateDecisionPoint,
} from '@/features/testcase/hooks/useTestCases';
import type { DecisionPoint, TransitionType } from '@/shared/types/testCase';

const TRANSITIONS: Array<{ value: TransitionType; label: string }> = [
  { value: 'NEXT_CASE', label: 'NEXT_CASE（单目标）' },
  { value: 'NEXT_CASES', label: 'NEXT_CASES（多目标）' },
  { value: 'PASS', label: 'PASS' },
  { value: 'FAIL', label: 'FAIL' },
  { value: 'N_A', label: 'N_A' },
];

type Props = { masterId: string; versionId: string; readOnly: boolean };
type Draft = { name: string; description: string; displayOrder: number; transitionType: TransitionType; targetMasterTestCaseIds: string[] };
const emptyDraft: Draft = { name: '', description: '', displayOrder: 1, transitionType: 'PASS', targetMasterTestCaseIds: [] };

export function DecisionPointEditor({ masterId, versionId, readOnly }: Props) {
  const pointsQuery = useDecisionPoints(masterId, versionId);
  const graphQuery = useMasterLogicGraph(masterId, versionId);
  const createMutation = useCreateDecisionPoint();
  const updateMutation = useUpdateDecisionPoint();
  const deleteMutation = useDeleteDecisionPoint();
  const [editing, setEditing] = useState<DecisionPoint | null | undefined>(undefined);
  const [draft, setDraft] = useState<Draft>(emptyDraft);
  const points = pointsQuery.data ?? [];
  const graph = graphQuery.data;
  const nodes = useMemo<Node[]>(() => graph?.nodes.map((node, index) => ({
    id: node.masterTestCaseId,
    data: { label: node.label },
    position: { x: (index % 3) * 220, y: Math.floor(index / 3) * 100 },
    type: 'default',
  })) ?? [], [graph]);
  const edges = useMemo<Edge[]>(() => graph?.edges.map((edge) => ({
    id: edge.id,
    source: edge.sourceMasterTestCaseId,
    target: edge.targetMasterTestCaseId,
    label: edge.transitionType,
    animated: false,
  })) ?? [], [graph]);

  const openCreate = () => { setDraft({ ...emptyDraft, displayOrder: points.length + 1 }); setEditing(null); };
  const openEdit = (point: DecisionPoint) => {
    setDraft({ name: point.name, description: point.description ?? '', displayOrder: point.displayOrder, transitionType: point.transition?.type ?? 'PASS', targetMasterTestCaseIds: point.transition?.targets.map((target) => target.masterTestCaseId) ?? [] });
    setEditing(point);
  };
  const save = () => {
    if (!draft.name.trim()) { message.error('请输入 Decision Point 名称'); return; }
    const payload = { ...draft, name: draft.name.trim(), description: draft.description.trim() };
    const options = { onSuccess: () => { setEditing(undefined); message.success('逻辑结构已保存'); }, onError: (error: unknown) => message.error(toApiError(error).userMessage) };
    if (editing) updateMutation.mutate({ masterId, versionId, pointId: editing.id, payload }, options);
    else createMutation.mutate({ masterId, versionId, payload }, options);
  };
  const remove = (pointId: string) => Modal.confirm({ title: '删除 Decision Point？', okType: 'danger', onOk: () => deleteMutation.mutate({ masterId, versionId, pointId }, { onError: (error) => message.error(toApiError(error).userMessage) }) });
  const targetCountAllowed = draft.transitionType === 'PASS' || draft.transitionType === 'FAIL' || draft.transitionType === 'N_A' ? 0 : draft.transitionType === 'NEXT_CASE' ? 1 : 999;
  const targetIds = targetCountAllowed === 0 ? [] : draft.targetMasterTestCaseIds;

  if (pointsQuery.isError || graphQuery.isError) return <Alert type="error" message="逻辑图加载失败" description={toApiError(pointsQuery.error ?? graphQuery.error).userMessage} />;
  return <Card title="Decision Points / Master Logic Graph" extra={!readOnly ? <Button type="primary" onClick={openCreate}>添加 Decision Point</Button> : <Tag>只读</Tag>}>
    <Space direction="vertical" style={{ width: '100%' }} size="large">
      {points.length ? <List bordered dataSource={points} renderItem={(point) => <List.Item actions={readOnly ? [] : [<Button key="edit" type="link" onClick={() => openEdit(point)}>编辑</Button>, <Button key="delete" type="link" danger onClick={() => remove(point.id)}>删除</Button>]}>
        <List.Item.Meta title={`${point.displayOrder}. ${point.name}`} description={<Space><Tag>{point.transition?.type ?? '—'}</Tag><Typography.Text type="secondary">{point.transition?.targets.map((target) => target.caseCode).join(', ') || '无目标'}</Typography.Text></Space>} />
      </List.Item>} /> : <Empty description="暂无 Decision Point" />}
      <div style={{ height: 360, border: '1px solid #d9d9d9', borderRadius: 8 }}>
        {graph?.nodes.length ? <ReactFlow nodes={nodes} edges={edges} fitView nodesDraggable={false} nodesConnectable={false}><Background /><Controls /><MiniMap /></ReactFlow> : <Empty description="暂无逻辑关系" style={{ paddingTop: 120 }} />}
      </div>
    </Space>
    <Modal title={editing ? '编辑 Decision Point' : '添加 Decision Point'} open={editing !== undefined} onCancel={() => setEditing(undefined)} onOk={save} confirmLoading={createMutation.isPending || updateMutation.isPending}>
      <Space direction="vertical" style={{ width: '100%' }}>
        <Input placeholder="名称" value={draft.name} onChange={(event) => setDraft({ ...draft, name: event.target.value })} />
        <Input.TextArea placeholder="描述（可选）" value={draft.description} onChange={(event) => setDraft({ ...draft, description: event.target.value })} />
        <Input type="number" min={1} placeholder="显示顺序" value={draft.displayOrder} onChange={(event) => setDraft({ ...draft, displayOrder: Number(event.target.value) })} />
        <Select style={{ width: '100%' }} value={draft.transitionType} options={TRANSITIONS} onChange={(transitionType: TransitionType) => setDraft({ ...draft, transitionType, targetMasterTestCaseIds: transitionType === 'PASS' || transitionType === 'FAIL' || transitionType === 'N_A' ? [] : draft.targetMasterTestCaseIds })} />
        {targetCountAllowed !== 0 ? <Input placeholder={targetCountAllowed === 1 ? '目标 Master UUID（一个）' : '目标 Master UUID（逗号分隔）'} value={targetIds.join(',')} onChange={(event) => setDraft({ ...draft, targetMasterTestCaseIds: event.target.value.split(',').map((value) => value.trim()).filter(Boolean) })} /> : <Typography.Text type="secondary">{draft.transitionType} 不允许配置目标。</Typography.Text>}
      </Space>
    </Modal>
  </Card>;
}

export default DecisionPointEditor;
