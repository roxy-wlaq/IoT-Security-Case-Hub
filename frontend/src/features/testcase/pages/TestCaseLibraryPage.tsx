import { Alert, Button, Input, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTestCases } from '@/features/testcase/hooks/useTestCases';
import { toApiError } from '@/shared/api/apiError';
import { LoadingState } from '@/shared/components/LoadingState';
import { PermissionGuard } from '@/shared/components/PermissionGuard';
import type { TestCaseSummary } from '@/shared/types/testCase';

export function TestCaseLibraryPage() {
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const params = useMemo(() => ({ q: q.trim() || undefined, page: 0, size: 20, sort: 'updatedAt,desc' }), [q]);
  const { data, isLoading, isError, error, refetch } = useTestCases(params);
  const columns: ColumnsType<TestCaseSummary> = [
    { title: '编码', dataIndex: 'caseCode', key: 'caseCode' },
    { title: '名称', dataIndex: 'caseName', key: 'caseName' },
    { title: '分类', dataIndex: 'categoryName', key: 'categoryName' },
    { title: '版本', dataIndex: 'versionLabel', key: 'versionLabel' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (status: string) => <Tag>{status}</Tag> },
    { title: '操作', key: 'action', render: (_: unknown, record) => <Button type="link" onClick={() => navigate(`/test-cases/${record.id}`)}>查看</Button> },
  ];
  return <div>
    <Typography.Title level={3}>测试用例库</Typography.Title>
    <Space style={{ marginBottom: 16 }}>
      <Input.Search allowClear placeholder="搜索编码、名称、目的、步骤或工具" value={q} onChange={(event) => setQ(event.target.value)} />
      <PermissionGuard permission="test_case:draft_create"><Button type="primary" onClick={() => navigate('/test-cases/new')}>新建 Draft</Button></PermissionGuard>
    </Space>
    {isError ? <Alert type="error" showIcon message="加载失败" description={toApiError(error).userMessage} action={<Button onClick={() => void refetch()}>重试</Button>} /> : null}
    {isLoading ? <LoadingState /> : <Table<TestCaseSummary> rowKey="id" columns={columns} dataSource={data?.content ?? []} pagination={false} />}
  </div>;
}

export default TestCaseLibraryPage;
