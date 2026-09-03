import { Alert, Button, Card, Descriptions, Empty, Input, List, message, Modal, Space, Spin, Table, Tag, Timeline, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  useAddContributor,
  useContributors,
  useCreateRevision,
  useDeprecate,
  usePublish,
  useReject,
  useRemoveContributor,
  useReturnReview,
  useReviewRecords,
  useSubmitReview,
  useTestCase,
  useTestCaseVersion,
  useTestCaseVersions,
} from '@/features/testcase/hooks/useTestCases';
import { toApiError } from '@/shared/api/apiError';
import type { ReviewRecordAction, TestCaseStatus, VersionSummary } from '@/shared/types/testCase';
import { DecisionPointEditor } from '@/features/testcase/components/DecisionPointEditor';

const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleString() : '—');

const STATUS_COLOR: Record<TestCaseStatus, string> = {
  DRAFT: 'default',
  REVIEW: 'processing',
  PUBLISHED: 'success',
  DEPRECATED: 'warning',
};

const REVIEW_ACTION_LABEL: Record<ReviewRecordAction, string> = {
  SUBMIT: '提交评审',
  PUBLISH: '发布',
  RETURN: '退回修改',
  REJECT: '驳回',
  DEPRECATE: '弃用',
};

const REVIEW_ACTION_COLOR: Record<ReviewRecordAction, string> = {
  SUBMIT: 'blue',
  PUBLISH: 'green',
  RETURN: 'blue',
  REJECT: 'red',
  DEPRECATE: 'gray',
};

type LifecycleKind = 'submitReview' | 'publish' | 'returnReview' | 'reject' | 'deprecate' | 'createRevision';

const ACTION_META: Record<LifecycleKind, { title: string; okText: string; requireComment: boolean; danger?: boolean }> = {
  submitReview: { title: '提交评审', okText: '提交', requireComment: false },
  publish: { title: '发布版本', okText: '发布', requireComment: false },
  returnReview: { title: '退回修改', okText: '退回', requireComment: true },
  reject: { title: '驳回版本', okText: '驳回', requireComment: true, danger: true },
  deprecate: { title: '弃用版本', okText: '弃用', requireComment: false, danger: true },
  createRevision: { title: '创建修订', okText: '创建', requireComment: false },
};

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

  const visibleVersionId = detail?.visibleVersion.id ?? '';
  const { data: reviewRecords } = useReviewRecords(masterId, selectedVersionId, Boolean(selectedVersionId));
  const canManageContributors = detail?.allowedActions.manageContributors ?? false;
  const { data: contributors } = useContributors(masterId, canManageContributors);

  // Lifecycle mutations
  const submitReviewMut = useSubmitReview();
  const publishMut = usePublish();
  const returnMut = useReturnReview();
  const rejectMut = useReject();
  const deprecateMut = useDeprecate();
  const createRevisionMut = useCreateRevision();
  const addContributorMut = useAddContributor();
  const removeContributorMut = useRemoveContributor();

  const [pending, setPending] = useState<LifecycleKind | null>(null);
  const [comment, setComment] = useState('');
  const [contributorUserId, setContributorUserId] = useState('');

  const isRejected = version?.status === 'REVIEW' && version?.latestReviewAction === 'REJECT';

  const confirmLoading = pending
    ? ({
        submitReview: submitReviewMut.isPending,
        publish: publishMut.isPending,
        returnReview: returnMut.isPending,
        reject: rejectMut.isPending,
        deprecate: deprecateMut.isPending,
        createRevision: createRevisionMut.isPending,
      })[pending]
    : false;

  const handleConfirm = () => {
    if (!pending || !detail) return;
    const meta = ACTION_META[pending];
    const trimmed = comment.trim();
    if (meta.requireComment && !trimmed) {
      message.error('请填写评审意见');
      return;
    }
    const onOk = (text: string) => {
      setPending(null);
      setComment('');
      message.success(text);
    };
    const onErr = (error: unknown) => message.error(toApiError(error).userMessage);
    switch (pending) {
      case 'submitReview':
        if (!detail.draftVersion) {
          message.error('没有可提交的 Draft');
          return;
        }
        submitReviewMut.mutate({ masterId: detail.id, payload: { comment: trimmed } }, { onSuccess: () => onOk('已提交评审'), onError: onErr });
        break;
      case 'publish':
        publishMut.mutate({ masterId: detail.id, versionId: visibleVersionId, payload: { comment: trimmed } }, { onSuccess: () => onOk('已发布版本'), onError: onErr });
        break;
      case 'returnReview':
        returnMut.mutate({ masterId: detail.id, versionId: visibleVersionId, payload: { comment: trimmed } }, { onSuccess: () => onOk('已退回修改'), onError: onErr });
        break;
      case 'reject':
        rejectMut.mutate({ masterId: detail.id, versionId: visibleVersionId, payload: { comment: trimmed } }, { onSuccess: () => onOk('已驳回版本'), onError: onErr });
        break;
      case 'deprecate':
        deprecateMut.mutate({ masterId: detail.id, versionId: visibleVersionId, payload: { comment: trimmed } }, { onSuccess: () => onOk('已弃用版本'), onError: onErr });
        break;
      case 'createRevision':
        createRevisionMut.mutate({ masterId: detail.id, payload: { changeReason: trimmed } }, { onSuccess: () => onOk('已创建修订版本'), onError: onErr });
        break;
    }
  };

  const handleAddContributor = () => {
    if (!contributorUserId.trim()) {
      message.error('请填写用户 ID');
      return;
    }
    addContributorMut.mutate(
      { masterId, userId: contributorUserId.trim() },
      {
        onSuccess: () => {
          setContributorUserId('');
          message.success('已添加贡献者');
        },
        onError: (error) => message.error(toApiError(error).userMessage),
      },
    );
  };

  const handleRemoveContributor = (userId: string) => {
    removeContributorMut.mutate(
      { masterId, userId },
      {
        onSuccess: () => message.success('已移除贡献者'),
        onError: (error) => message.error(toApiError(error).userMessage),
      },
    );
  };

  const versionColumns: ColumnsType<VersionSummary> = [
    { title: '版本', dataIndex: 'versionLabel', key: 'versionLabel' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (status: string) => <Tag color={STATUS_COLOR[status as TestCaseStatus]}>{status}</Tag> },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', render: (value: string) => formatDate(value) },
    { title: '发布时间', dataIndex: 'publishedAt', key: 'publishedAt', render: (value: string | null) => formatDate(value) },
    { title: '当前', dataIndex: 'isCurrentVersion', key: 'isCurrentVersion', render: (current: boolean) => (current ? '是' : '否') },
  ];

  if (detailQuery.isLoading) return <Spin tip="加载测试用例…" />;
  if (detailQuery.isError || !detail || !version) {
    return <Alert type="error" showIcon message="加载测试用例失败" description={toApiError(detailQuery.error).userMessage} />;
  }

  const actions = detail.allowedActions;

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Space wrap>
        <Button type="link" onClick={() => navigate('/test-cases')}>返回测试库</Button>
        {actions.editDraft ? <Button type="primary" onClick={() => navigate(`/test-cases/${masterId}/edit`)}>编辑 Draft</Button> : null}
        {actions.submitReview ? <Button onClick={() => { setComment(''); setPending('submitReview'); }}>提交评审</Button> : null}
        {actions.publish ? <Button type="primary" onClick={() => { setComment(''); setPending('publish'); }}>发布</Button> : null}
        {actions.returnReview ? <Button onClick={() => { setComment(''); setPending('returnReview'); }}>退回修改</Button> : null}
        {actions.reject ? <Button danger onClick={() => { setComment(''); setPending('reject'); }}>驳回</Button> : null}
        {actions.deprecate ? <Button danger onClick={() => { setComment(''); setPending('deprecate'); }}>弃用</Button> : null}
        {actions.createRevision ? <Button onClick={() => { setComment(''); setPending('createRevision'); }}>创建修订</Button> : null}
      </Space>

      <Typography.Title level={3}>
        {detail.caseCode} · {version.caseName}
        {isRejected ? <Tag color="red" style={{ marginLeft: 8 }}>已驳回</Tag> : null}
      </Typography.Title>

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
          <Descriptions.Item label="Version / Status">
            {version.versionLabel} / <Tag color={STATUS_COLOR[version.status]}>{version.status}</Tag>
            {version.revisionClosed ? <Tag color="default">修订已关闭</Tag> : null}
          </Descriptions.Item>
          <Descriptions.Item label="Created">{formatDate(version.createdAt)}</Descriptions.Item>
          <Descriptions.Item label="Updated">{formatDate(version.updatedAt)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="Steps">
        <List
          dataSource={version.steps}
          renderItem={(step) => (
            <List.Item>
              <Typography.Text strong>{step.sequenceNo}. {step.title || 'Step'}</Typography.Text>
              <Typography.Paragraph style={{ margin: 0 }}>{step.content}</Typography.Paragraph>
            </List.Item>
          )}
        />
      </Card>

      <DecisionPointEditor
        masterId={masterId}
        versionId={version.id}
        readOnly={!actions.editDraft || version.id !== detail.draftVersion?.id}
      />

      <Card title="评审记录">
        {reviewRecords && reviewRecords.length > 0 ? (
          <Timeline
            items={reviewRecords.map((record) => ({
              color: REVIEW_ACTION_COLOR[record.action],
              children: (
                <Space direction="vertical" size={0}>
                  <Typography.Text strong>{REVIEW_ACTION_LABEL[record.action]}</Typography.Text>
                  <Typography.Text type="secondary">{record.reviewerName} · {formatDate(record.createdAt)}</Typography.Text>
                  {record.comment ? <Typography.Paragraph style={{ margin: 0 }}>{record.comment}</Typography.Paragraph> : null}
                </Space>
              ),
            }))}
          />
        ) : (
          <Empty description="暂无评审记录" />
        )}
      </Card>

      {canManageContributors ? (
        <Card title="修订贡献者">
          <Space direction="vertical" style={{ width: '100%' }}>
            <Space.Compact style={{ maxWidth: 480 }}>
              <Input
                placeholder="输入用户 ID (UUID) 以添加贡献者"
                value={contributorUserId}
                onChange={(event) => setContributorUserId(event.target.value)}
                onPressEnter={handleAddContributor}
              />
              <Button type="primary" loading={addContributorMut.isPending} onClick={handleAddContributor}>添加</Button>
            </Space.Compact>
            <List
              dataSource={contributors ?? []}
              locale={{ emptyText: <Empty description="暂无贡献者" /> }}
              renderItem={(contributor) => (
                <List.Item
                  actions={[
                    <Button key="remove" danger type="link" loading={removeContributorMut.isPending} onClick={() => handleRemoveContributor(contributor.userId)}>
                      移除
                    </Button>,
                  ]}
                >
                  <List.Item.Meta title={contributor.displayName} description={`${contributor.username} · ${contributor.userId}`} />
                </List.Item>
              )}
            />
          </Space>
        </Card>
      ) : null}

      <Card title="版本历史">
        <Table<VersionSummary>
          rowKey="id"
          columns={versionColumns}
          dataSource={versions}
          loading={versionsQuery.isLoading}
          pagination={false}
          onRow={(record) => ({ onClick: () => setSelectedVersionId(record.id), style: { cursor: 'pointer' } })}
        />
      </Card>

      <Modal
        open={pending !== null}
        title={pending ? ACTION_META[pending].title : ''}
        okText={pending ? ACTION_META[pending].okText : '确定'}
        okButtonProps={{ danger: pending ? ACTION_META[pending].danger : false }}
        confirmLoading={confirmLoading}
        onOk={handleConfirm}
        onCancel={() => { setPending(null); setComment(''); }}
      >
        <Input.TextArea
          rows={4}
          value={comment}
          onChange={(event) => setComment(event.target.value)}
          placeholder={pending && ACTION_META[pending].requireComment ? '请填写评审意见（必填）' : '评审意见（可选）'}
        />
      </Modal>
    </Space>
  );
}

export default TestCaseDetailPage;
