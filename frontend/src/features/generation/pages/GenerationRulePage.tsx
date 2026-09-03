import { Alert, Card, Table, Tag, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { listGenerationRules } from '@/features/project/api/projectApi';
import type { GenerationRule } from '@/shared/types/project';

export function GenerationRulePage() {
  const [rules, setRules] = useState<GenerationRule[]>([]); const [error, setError] = useState(false);
  useEffect(() => { void listGenerationRules().then(setRules).catch(() => setError(true)); }, []);
  return <Card><Typography.Title level={3}>生成规则</Typography.Title>{error ? <Alert type="error" message="生成规则加载失败" /> : null}<Table rowKey="id" dataSource={rules} columns={[{ title: 'Rule Code', dataIndex: 'ruleCode' }, { title: '名称', dataIndex: 'name' }, { title: '模式', dataIndex: 'mode' }, { title: '状态', dataIndex: 'status', render: (v: string) => <Tag color={v === 'ENABLED' ? 'green' : 'default'}>{v}</Tag> }, { title: '输出 Master 数', render: (_: unknown, rule: GenerationRule) => rule.outputMasterTestCaseIds.length }]} /></Card>;
}
export default GenerationRulePage;
