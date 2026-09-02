import { Alert, Button, Card, Descriptions, List, Space, Spin, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTestCase, useTestCaseVersion, useTestCaseVersions } from '@/features/testcase/hooks/useTestCases';
import { toApiError } from '@/shared/api/apiError';
import type { VersionSummary } from '@/shared/types/testCase';

const formatDate = (value?: string | null) => value ? new Date(value).toLocaleString() : '—';

export function TestCaseDetailPage() {
  const { masterId = '' } = useParams<{ masterId: string }>();
  const navigate = useNavigate();
  const detailQuery = useTestCase(masterId);
  const versionsQuery = useTestCaseVersions(masterId);
  const [selectedVersionId, setSelectedVersionId] = useState('');
  const detail = detailQuery.data;

  useEffect(() => {
    if (!selectedVersionId && detail?.visibleVersion.id) setSelectedVersionId(detail.visibleVersion.id);
  }, [detail, selectedVersionId]);

  const selectedVersionQuery = useTestCaseVersion(masterId, selectedVersionId, Boolean(selectedVersionId));
  const version = selectedVersionQuery.data ?? detail?.visibleVersion;
  const versions = versionsQuery.data ?? detail?.versions ?? [];
  const versionColumns: ColumnsType<VersionSummary> = useMemo(() => [
    { title: '版本', dataIndex: 'versionLabel', key: 'versionLabel' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (status: string) => <Tag>{status}</Tag> },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', render: (value: string) => formatDate(value) },
    { title: '发布时间', dataIndex: 'publishedAt', key: 'publishedAt', render: (value: string | null) => formatDate(value) },
    { title: '当前', dataIndex: 'isCurrentVersion', key: 'isCurrentVersion', render: (current: boolean) => current ? '是' : '否' },
  ], []);

  if (detailQuery.isLoading) return <Spin tip="加载测试用例…" />;
  if (detailQuery.isError || !detail || !version) {
    return <Alert type="error" showIcon message="加载测试用例失败" description={toApiError(detailQuery.error).userMessage} />;
  }

  return <Space direction="vertical" size="large" style={{ width: '100%' }}>
    <Space>
      <Button type="link" onClick={() => navigate('/test-cases')}>返回测试库</Button>
      {detail.allowedActions.editDraft ? <Button type="primary" onClick={() => navigate(`/test-cases/${masterId}/edit`)}>编辑 Draft</Button> : null}
    </Space>
    <Typography.Title level={3}>{detail.caseCode} · {version.caseName}</Typography.Title>
    <Card title="测试用例详情">
      <Descriptions bordered column={2}>
        <Descriptions.Item label="Case Code">{detail.caseCode}</Descriptions.Item>
        <Descriptions.Item label="Case Name">{version.caseName}</Descriptions.Item>
        <Descriptions.Item label="Category">{detail.categoryName}</Descriptions.Item>
        <Descriptions.Item label="Tags">{detail.tags.length ? detail.tags.map((tag) => <Tag key={tag.id}>{tag.name}</Tag>) : '—'}</Descriptions.Item>
        <Descriptions.Item label="Purpose" span={2}>{version.testPurpose || '—'}</Descriptions.Item>
        <Descriptions.Item label="Preconditions" span={2}>{version.preconditions || '—'}</Descriptions.Item>
        <Descriptions.Item label="Tools">{version.tools.length ? version.tools.map((tool) => <Tag key={tool.id}>{tool.name}</Tag>) : '—'}</Descriptions.Item>
        <Descriptions.Item label="Standard Mapping">{version.standardMappings.length ? version.standardMappings.map((mapping) => <div key={mapping.standardTaskTypeId}>{mapping.standardName}: {mapping.mappingNote || '—'}</div>) : '—'}</Descriptions.Item>
        <Descriptions.Item label="Evidence Requirement">{version.evidenceRequirement || (version.evidenceRequired ? 'Required' : 'Not required')}</Descriptions.Item>
        <Descriptions.Item label="Remark Requirement">{version.remarkRequirement || '—'}</Descriptions.Item>
        <Descriptions.Item label="Progressive Role">{version.progressiveRole || 'None'}</Descriptions.Item>
        <Descriptions.Item label="Version / Status">{version.versionLabel} / {version.status}</Descriptions.Item>
        <Descriptions.Item label="Created">{formatDate(version.createdAt)}</Descriptions.Item>
        <Descriptions.Item label="Updated">{formatDate(version.updatedAt)}</Descriptions.Item>
      </Descriptions>
    </Card>
    <Card title="Steps">
      <List dataSource={version.steps} renderItem={(step) => <List.Item><Typography.Text strong>{step.sequenceNo}. {step.title || 'Step'}</Typography.Text><Typography.Paragraph style={{ margin: 0 }}>{step.content}</Typography.Paragraph></List.Item>} />
    </Card>
    <Card title="版本历史">
      <Table<VersionSummary> rowKey="id" columns={versionColumns} dataSource={versions} loading={versionsQuery.isLoading} pagination={false} onRow={(record) => ({ onClick: () => setSelectedVersionId(record.id), style: { cursor: 'pointer' } })} />
    </Card>
  </Space>;
}

export default TestCaseDetailPage;
