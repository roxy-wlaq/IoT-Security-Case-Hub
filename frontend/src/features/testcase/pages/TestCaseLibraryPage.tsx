import { Alert, Button, Input, Select, Space, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCategories } from '@/features/dictionary/hooks/useCategories';
import { useStandards } from '@/features/dictionary/hooks/useStandards';
import { useTags } from '@/features/dictionary/hooks/useTags';
import { useTools } from '@/features/dictionary/hooks/useTools';
import { useTestCases } from '@/features/testcase/hooks/useTestCases';
import { toApiError } from '@/shared/api/apiError';
import { LoadingState } from '@/shared/components/LoadingState';
import { PermissionGuard } from '@/shared/components/PermissionGuard';
import type { Category } from '@/shared/types/dictionary';
import type { TestCaseStatus, TestCaseSummary } from '@/shared/types/testCase';

function flattenCategories(categories: Category[]): Category[] {
  return categories.flatMap((category) => [category, ...flattenCategories(category.children ?? [])]);
}

export function TestCaseLibraryPage() {
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const [categoryId, setCategoryId] = useState<string>();
  const [tagIds, setTagIds] = useState<string[]>([]);
  const [toolIds, setToolIds] = useState<string[]>([]);
  const [standardTaskTypeIds, setStandardTaskTypeIds] = useState<string[]>([]);
  const [status, setStatus] = useState<TestCaseStatus>();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const categories = useCategories({ enabled: true });
  const tags = useTags({ enabled: true });
  const tools = useTools({ enabled: true });
  const standards = useStandards({ enabled: true });
  const params = useMemo(() => ({
    q: q.trim() || undefined, categoryId, tagIds: tagIds.length ? tagIds : undefined, toolIds: toolIds.length ? toolIds : undefined,
    standardTaskTypeIds: standardTaskTypeIds.length ? standardTaskTypeIds : undefined, status, page, size, sort: 'updatedAt,desc',
  }), [categoryId, page, q, size, standardTaskTypeIds, status, tagIds, toolIds]);
  const query = useTestCases(params);
  const resetPage = <T,>(setter: (value: T) => void) => (value: T) => { setter(value); setPage(0); };
  const columns: ColumnsType<TestCaseSummary> = [
    { title: '编码', dataIndex: 'caseCode', key: 'caseCode' },
    { title: '名称', dataIndex: 'caseName', key: 'caseName' },
    { title: '分类', dataIndex: 'categoryName', key: 'categoryName' },
    { title: 'Tags', dataIndex: 'tags', key: 'tags', render: (values: TestCaseSummary['tags']) => values.map((tag) => <Tag key={tag.id}>{tag.name}</Tag>) },
    { title: '版本', dataIndex: 'versionLabel', key: 'versionLabel' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (value: string) => <Tag>{value}</Tag> },
    { title: 'Updated At', dataIndex: 'updatedAt', key: 'updatedAt', render: (value: string) => new Date(value).toLocaleString() },
    { title: '操作', key: 'action', render: (_value: unknown, record) => <Button type="link" onClick={() => navigate(`/test-cases/${record.id}`)}>查看</Button> },
  ];
  const categoryOptions = flattenCategories(categories.data ?? []).map((item) => ({ value: item.id, label: item.name }));

  return <div>
    <Typography.Title level={3}>测试用例库</Typography.Title>
    <Space wrap style={{ marginBottom: 16 }}>
      <Input.Search allowClear placeholder="搜索编码、名称、目的、步骤或工具" value={q} onChange={(event) => { setQ(event.target.value); setPage(0); }} />
      <Select allowClear placeholder="分类" options={categoryOptions} value={categoryId} onChange={resetPage(setCategoryId)} />
      <Select mode="multiple" placeholder="Tags" options={(tags.data ?? []).map((tag) => ({ value: tag.id, label: tag.name }))} value={tagIds} onChange={resetPage(setTagIds)} />
      <Select mode="multiple" placeholder="Tools" options={(tools.data ?? []).map((tool) => ({ value: tool.id, label: tool.name }))} value={toolIds} onChange={resetPage(setToolIds)} />
      <Select mode="multiple" placeholder="Standard" options={(standards.data ?? []).map((standard) => ({ value: standard.id, label: standard.name }))} value={standardTaskTypeIds} onChange={resetPage(setStandardTaskTypeIds)} />
      <Select allowClear placeholder="状态" options={['DRAFT', 'REVIEW', 'PUBLISHED', 'DEPRECATED'].map((value) => ({ value, label: value }))} value={status} onChange={resetPage(setStatus)} />
      <PermissionGuard permission="test_case:draft_create"><Button type="primary" onClick={() => navigate('/test-cases/new')}>新建 Draft</Button></PermissionGuard>
    </Space>
    {query.isError ? <Alert type="error" showIcon message="加载失败" description={toApiError(query.error).userMessage} action={<Button onClick={() => void query.refetch()}>重试</Button>} /> : null}
    {query.isLoading ? <LoadingState /> : <Table<TestCaseSummary> rowKey="id" columns={columns} dataSource={query.data?.content ?? []} loading={query.isFetching} pagination={{ current: (query.data?.page ?? page) + 1, pageSize: query.data?.size ?? size, total: query.data?.totalElements ?? 0, showSizeChanger: true, showTotal: (total) => `共 ${total} 条`, onChange: (current, pageSize) => { setPage(current - 1); setSize(pageSize); } }} />}
  </div>;
}

export default TestCaseLibraryPage;
