import { Alert, App, Button, Input, Popover, Select, Space, Table, Tag, Typography, theme } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DeleteOutlined, DownloadOutlined, SaveOutlined, StarOutlined } from '@ant-design/icons';
import { useMemo, useState } from 'react';
import type { Key } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCategories } from '@/features/dictionary/hooks/useCategories';
import { useStandards } from '@/features/dictionary/hooks/useStandards';
import { useTags } from '@/features/dictionary/hooks/useTags';
import { useTools } from '@/features/dictionary/hooks/useTools';
import { useTestCases } from '@/features/testcase/hooks/useTestCases';
import { toApiError } from '@/shared/api/apiError';
import { BrandEmpty } from '@/shared/components/BrandEmpty';
import { BulkActionBar } from '@/shared/components/BulkActionBar';
import { ColumnSettingsPopover } from '@/shared/components/ColumnSettingsPopover';
import { DensitySwitch } from '@/shared/components/DensitySwitch';
import { PageContainer } from '@/shared/components/PageContainer';
import { PermissionGuard } from '@/shared/components/PermissionGuard';
import { StatusTag, testCaseStatusMeta } from '@/shared/components/StatusTag';
import { TableSkeleton } from '@/shared/components/TableSkeleton';
import { useTablePreferences } from '@/shared/hooks/useTablePreferences';
import { downloadCsv } from '@/shared/utils/csvExport';
import type { Category } from '@/shared/types/dictionary';
import type { TestCaseStatus, TestCaseSummary } from '@/shared/types/testCase';

interface SavedView {
  name: string;
  q: string;
  categoryId?: string;
  tagIds: string[];
  toolIds: string[];
  standardTaskTypeIds: string[];
  status?: TestCaseStatus;
}

const VIEWS_STORAGE_KEY = 'casehub.testCaseLibrary.views';

function readViews(): SavedView[] {
  if (typeof window === 'undefined') {
    return [];
  }
  try {
    const raw = window.localStorage.getItem(VIEWS_STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? (parsed as SavedView[]) : [];
  } catch {
    return [];
  }
}

function persistViews(views: SavedView[]): void {
  try {
    window.localStorage.setItem(VIEWS_STORAGE_KEY, JSON.stringify(views));
  } catch {
    /* ignore storage errors */
  }
}

function SavedViewsControl({
  views,
  onApply,
  onDelete,
  onSave,
}: {
  views: SavedView[];
  onApply: (view: SavedView) => void;
  onDelete: (name: string) => void;
  onSave: (name: string) => void;
}) {
  const { token } = theme.useToken();
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const save = () => {
    if (!name.trim()) return;
    onSave(name.trim());
    setName('');
  };

  return (
    <Popover
      trigger="click"
      placement="bottomRight"
      open={open}
      onOpenChange={setOpen}
      content={
        <div style={{ minWidth: 230, display: 'flex', flexDirection: 'column', gap: token.paddingXS }}>
          {views.length === 0 ? (
            <Typography.Text type="secondary" style={{ fontSize: token.fontSizeSM }}>
              暂无保存的视图
            </Typography.Text>
          ) : (
            views.map((view) => (
              <div
                key={view.name}
                style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: token.paddingXS }}
              >
                <Button
                  type="link"
                  size="small"
                  style={{ padding: 0 }}
                  onClick={() => {
                    onApply(view);
                    setOpen(false);
                  }}
                >
                  {view.name}
                </Button>
                <Button
                  type="text"
                  size="small"
                  danger
                  icon={<DeleteOutlined />}
                  aria-label={`删除视图 ${view.name}`}
                  onClick={() => onDelete(view.name)}
                />
              </div>
            ))
          )}
          <div
            style={{
              borderTop: '1px solid var(--color-border-secondary)',
              paddingTop: token.paddingSM,
              marginTop: token.paddingXS,
              display: 'flex',
              gap: token.paddingXS,
            }}
          >
            <Input
              size="small"
              placeholder="保存当前筛选为视图"
              value={name}
              onChange={(event) => setName(event.target.value)}
              onPressEnter={save}
            />
            <Button size="small" type="primary" icon={<SaveOutlined />} disabled={!name.trim()} onClick={save}>
              保存
            </Button>
          </div>
        </div>
      }
    >
      <Button icon={<StarOutlined />}>视图</Button>
    </Popover>
  );
}

function flattenCategories(categories: Category[]): Category[] {
  return categories.flatMap((category) => [category, ...flattenCategories(category.children ?? [])]);
}

export function TestCaseLibraryPage() {
  const navigate = useNavigate();
  const { message } = App.useApp();
  const { token } = theme.useToken();

  const [q, setQ] = useState('');
  const [categoryId, setCategoryId] = useState<string>();
  const [tagIds, setTagIds] = useState<string[]>([]);
  const [toolIds, setToolIds] = useState<string[]>([]);
  const [standardTaskTypeIds, setStandardTaskTypeIds] = useState<string[]>([]);
  const [status, setStatus] = useState<TestCaseStatus>();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);
  const [views, setViews] = useState<SavedView[]>(readViews);

  const { prefs, setDensity, setHiddenColumnKeys } = useTablePreferences('casehub.table.testCaseLibrary');

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
    { title: '编码', dataIndex: 'caseCode', key: 'caseCode', render: (value: string) => <span style={{ fontFamily: token.fontFamilyCode }}>{value}</span> },
    { title: '名称', dataIndex: 'caseName', key: 'caseName' },
    { title: '分类', dataIndex: 'categoryName', key: 'categoryName' },
    { title: 'Tags', dataIndex: 'tags', key: 'tags', render: (values: TestCaseSummary['tags']) => values.map((tag) => <Tag key={tag.id}>{tag.name}</Tag>) },
    { title: '版本', dataIndex: 'versionLabel', key: 'versionLabel' },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (value: TestCaseStatus) => {
        const meta = testCaseStatusMeta(value);
        return <StatusTag tone={meta.tone} label={meta.label} dot />;
      },
    },
    { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', render: (value: string) => new Date(value).toLocaleString() },
    { title: '操作', key: 'action', render: (_value: unknown, record: TestCaseSummary) => <Button type="link" onClick={() => navigate(`/test-cases/${record.id}`)}>查看</Button> },
  ];
  const columnDefs = [
    { key: 'caseCode', title: '编码' },
    { key: 'caseName', title: '名称' },
    { key: 'categoryName', title: '分类' },
    { key: 'tags', title: 'Tags' },
    { key: 'versionLabel', title: '版本' },
    { key: 'status', title: '状态' },
    { key: 'updatedAt', title: '更新时间' },
    { key: 'action', title: '操作', lockable: false },
  ];
  const visibleColumns = columns.filter((column) => !prefs.hiddenColumnKeys.includes(String(column.key ?? '')));

  const categoryOptions = flattenCategories(categories.data ?? []).map((item) => ({ value: item.id, label: item.name }));

  const applyView = (view: SavedView) => {
    setQ(view.q);
    setCategoryId(view.categoryId);
    setTagIds(view.tagIds);
    setToolIds(view.toolIds);
    setStandardTaskTypeIds(view.standardTaskTypeIds);
    setStatus(view.status);
    setPage(0);
  };
  const saveView = (name: string) => {
    const next: SavedView[] = [
      ...views.filter((view) => view.name !== name),
      { name, q, categoryId, tagIds, toolIds, standardTaskTypeIds, status },
    ];
    setViews(next);
    persistViews(next);
    message.success(`视图「${name}」已保存`);
  };
  const deleteView = (name: string) => {
    const next = views.filter((view) => view.name !== name);
    setViews(next);
    persistViews(next);
  };

  const exportSelected = () => {
    const selected = (query.data?.content ?? []).filter((row) => selectedRowKeys.includes(row.id));
    if (selected.length === 0) {
      return;
    }
    downloadCsv(`test-cases-${new Date().toISOString().slice(0, 10)}`, [
      { header: '编码', value: (row: TestCaseSummary) => row.caseCode },
      { header: '名称', value: (row: TestCaseSummary) => row.caseName },
      { header: '分类', value: (row: TestCaseSummary) => row.categoryName },
      { header: '标签', value: (row: TestCaseSummary) => row.tags.map((tag) => tag.name).join(' / ') },
      { header: '版本', value: (row: TestCaseSummary) => row.versionLabel },
      { header: '状态', value: (row: TestCaseSummary) => testCaseStatusMeta(row.status).label },
      { header: '更新时间', value: (row: TestCaseSummary) => new Date(row.updatedAt).toLocaleString() },
    ], selected);
    message.success(`已导出 ${selected.length} 条用例`);
  };

  const emptyContent = (
    <PermissionGuard permission="test_case:draft_create" fallback={<BrandEmpty description="暂无用例" />}>
      <BrandEmpty
        description="暂无用例，试试调整筛选或新建 Draft"
        cta="新建 Draft"
        onCta={() => navigate('/test-cases/new')}
      />
    </PermissionGuard>
  );

  return (
    <PageContainer
      title="测试用例库"
      subtitle="Master Test Case 库、版本与生命周期"
      extra={
        <PermissionGuard permission="test_case:draft_create">
          <Button type="primary" onClick={() => navigate('/test-cases/new')}>新建 Draft</Button>
        </PermissionGuard>
      }
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          gap: token.padding,
          flexWrap: 'wrap',
          marginBottom: token.padding,
        }}
      >
        <Space wrap>
          <Input.Search allowClear placeholder="搜索编码、名称、目的、步骤或工具" value={q} onChange={(event) => { setQ(event.target.value); setPage(0); }} />
          <Select allowClear placeholder="分类" options={categoryOptions} value={categoryId} onChange={resetPage(setCategoryId)} />
          <Select mode="multiple" placeholder="Tags" options={(tags.data ?? []).map((tag) => ({ value: tag.id, label: tag.name }))} value={tagIds} onChange={resetPage(setTagIds)} />
          <Select mode="multiple" placeholder="Tools" options={(tools.data ?? []).map((tool) => ({ value: tool.id, label: tool.name }))} value={toolIds} onChange={resetPage(setToolIds)} />
          <Select mode="multiple" placeholder="Standard" options={(standards.data ?? []).map((standard) => ({ value: standard.id, label: standard.name }))} value={standardTaskTypeIds} onChange={resetPage(setStandardTaskTypeIds)} />
          <Select allowClear placeholder="状态" options={['DRAFT', 'REVIEW', 'PUBLISHED', 'DEPRECATED'].map((value) => ({ value, label: testCaseStatusMeta(value as TestCaseStatus).label }))} value={status} onChange={resetPage(setStatus)} />
        </Space>
        <Space wrap>
          <SavedViewsControl views={views} onApply={applyView} onDelete={deleteView} onSave={saveView} />
          <DensitySwitch value={prefs.density} onChange={setDensity} />
          <ColumnSettingsPopover columns={columnDefs} hiddenKeys={prefs.hiddenColumnKeys} onChange={setHiddenColumnKeys} />
        </Space>
      </div>

      <BulkActionBar count={selectedRowKeys.length} onClear={() => setSelectedRowKeys([])}>
        <Button size="small" icon={<DownloadOutlined />} onClick={exportSelected}>
          导出 CSV
        </Button>
      </BulkActionBar>

      {query.isError ? (
        <Alert
          type="error"
          showIcon
          message="加载失败"
          description={toApiError(query.error).userMessage}
          action={<Button onClick={() => void query.refetch()}>重试</Button>}
          style={{ marginBottom: token.padding }}
        />
      ) : null}

      {query.isLoading ? (
        <TableSkeleton rows={6} />
      ) : (
        <Table<TestCaseSummary>
          rowKey="id"
          columns={visibleColumns}
          dataSource={query.data?.content ?? []}
          loading={query.isFetching}
          size={prefs.density}
          rowSelection={{ selectedRowKeys, onChange: (keys) => setSelectedRowKeys(keys) }}
          locale={{ emptyText: emptyContent }}
          pagination={{
            current: (query.data?.page ?? page) + 1,
            pageSize: query.data?.size ?? size,
            total: query.data?.totalElements ?? 0,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (current, pageSize) => { setPage(current - 1); setSize(pageSize); },
          }}
        />
      )}
    </PageContainer>
  );
}

export default TestCaseLibraryPage;
