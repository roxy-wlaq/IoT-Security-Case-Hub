import { Button, Card, Col, List, Row, Skeleton, Space, Tag, Typography, theme } from 'antd';
import {
  AuditOutlined,
  CheckCircleOutlined,
  DatabaseOutlined,
  EditOutlined,
  ExperimentOutlined,
  ProjectOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import { useTestCases } from '@/features/testcase/hooks/useTestCases';
import { BrandEmpty } from '@/shared/components/BrandEmpty';
import { ErrorState } from '@/shared/components/ErrorState';
import { PageContainer } from '@/shared/components/PageContainer';
import { PermissionGuard } from '@/shared/components/PermissionGuard';
import { StatCard } from '@/shared/components/StatCard';
import { StatusTag, testCaseStatusMeta, toneTokenColor } from '@/shared/components/StatusTag';
import type { TestCaseStatus, TestCaseSummary } from '@/shared/types/testCase';

const STATUS_ORDER: TestCaseStatus[] = ['DRAFT', 'REVIEW', 'PUBLISHED', 'DEPRECATED'];

function relativeTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes} 分钟前`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} 小时前`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days} 天前`;
  return new Date(iso).toLocaleDateString();
}

/**
 * 工作台：欢迎区 + 用例统计卡 + 状态分布 + 最近更新。
 * 统计复用 listTestCases 的 totalElements（size=1 的轻量探测查询）。
 */
export function Dashboard() {
  const navigate = useNavigate();
  const { data: user } = useCurrentUser();
  const { token } = theme.useToken();

  const totalQuery = useTestCases({ page: 0, size: 1 });
  const draftQuery = useTestCases({ page: 0, size: 1, status: 'DRAFT' });
  const reviewQuery = useTestCases({ page: 0, size: 1, status: 'REVIEW' });
  const publishedQuery = useTestCases({ page: 0, size: 1, status: 'PUBLISHED' });
  const deprecatedQuery = useTestCases({ page: 0, size: 1, status: 'DEPRECATED' });
  const recentQuery = useTestCases({ page: 0, size: 6, sort: 'updatedAt,desc' });

  const statusCounts: Record<TestCaseStatus, { count: number; loading: boolean }> = {
    DRAFT: { count: draftQuery.data?.totalElements ?? 0, loading: draftQuery.isLoading },
    REVIEW: { count: reviewQuery.data?.totalElements ?? 0, loading: reviewQuery.isLoading },
    PUBLISHED: { count: publishedQuery.data?.totalElements ?? 0, loading: publishedQuery.isLoading },
    DEPRECATED: { count: deprecatedQuery.data?.totalElements ?? 0, loading: deprecatedQuery.isLoading },
  };
  const distribution = STATUS_ORDER.map((status) => ({ status, count: statusCounts[status].count }));
  const distributionLoading = STATUS_ORDER.some((status) => statusCounts[status].loading);
  const totalAll = distribution.reduce((sum, item) => sum + item.count, 0);

  const today = new Date().toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  });

  return (
    <PageContainer noCard>
      <Space direction="vertical" size={token.paddingLG} style={{ width: '100%' }}>
        <Card bordered={false} style={{ boxShadow: token.boxShadowSecondary }}>
          <Row gutter={[token.padding, token.padding]} align="middle" justify="space-between" wrap>
            <Col xs={24} md="auto">
              <Space direction="vertical" size={2}>
                <Space size={12} wrap>
                  <Typography.Title level={4} style={{ margin: 0 }}>
                    你好，{user?.displayName ?? user?.username ?? '用户'}
                  </Typography.Title>
                  {user?.roles?.map((role) => (
                    <Tag key={role} bordered={false} color="blue">
                      {role}
                    </Tag>
                  ))}
                </Space>
                <Typography.Text type="secondary">今天是 {today}，以下是平台最新动态</Typography.Text>
              </Space>
            </Col>
            <Col xs={24} md="auto">
              <Space wrap>
                <PermissionGuard permission="test_case:draft_create">
                  <Button type="primary" icon={<EditOutlined />} onClick={() => navigate('/test-cases/new')}>
                    新建用例
                  </Button>
                </PermissionGuard>
                <Button icon={<ExperimentOutlined />} onClick={() => navigate('/test-cases')}>
                  进入测试库
                </Button>
                <PermissionGuard permission="project:read">
                  <Button icon={<ProjectOutlined />} onClick={() => navigate('/projects')}>
                    项目管理
                  </Button>
                </PermissionGuard>
              </Space>
            </Col>
          </Row>
        </Card>

        <Row gutter={[token.padding, token.padding]}>
          <Col xs={12} lg={6}>
            <StatCard
              tone="info"
              icon={<DatabaseOutlined />}
              label="用例总数"
              value={totalQuery.data?.totalElements}
              loading={totalQuery.isLoading}
              onClick={() => navigate('/test-cases')}
            />
          </Col>
          <Col xs={12} lg={6}>
            <StatCard
              tone="neutral"
              icon={<EditOutlined />}
              label="草稿"
              value={statusCounts.DRAFT.count}
              loading={statusCounts.DRAFT.loading}
              onClick={() => navigate('/test-cases')}
            />
          </Col>
          <Col xs={12} lg={6}>
            <StatCard
              tone="review"
              icon={<AuditOutlined />}
              label="审核中"
              value={statusCounts.REVIEW.count}
              loading={statusCounts.REVIEW.loading}
              onClick={() => navigate('/test-cases')}
            />
          </Col>
          <Col xs={12} lg={6}>
            <StatCard
              tone="success"
              icon={<CheckCircleOutlined />}
              label="已发布"
              value={statusCounts.PUBLISHED.count}
              loading={statusCounts.PUBLISHED.loading}
              onClick={() => navigate('/test-cases')}
            />
          </Col>
        </Row>

        <Row gutter={[token.padding, token.padding]}>
          <Col xs={24} lg={14}>
            <Card bordered={false} title="用例状态分布" style={{ boxShadow: token.boxShadowSecondary }}>
              {distributionLoading ? (
                <Skeleton active paragraph={{ rows: 3 }} />
              ) : totalAll === 0 ? (
                <BrandEmpty size="small" description="暂无用例数据" />
              ) : (
                <>
                  <div
                    style={{
                      display: 'flex',
                      height: 12,
                      borderRadius: token.borderRadius,
                      overflow: 'hidden',
                      background: token.colorFillQuaternary,
                      marginBottom: token.paddingLG,
                    }}
                  >
                    {distribution
                      .filter((item) => item.count > 0)
                      .map(({ status, count }) => (
                        <div
                          key={status}
                          title={`${testCaseStatusMeta(status).label} ${count} 条`}
                          style={{
                            width: `${(count / totalAll) * 100}%`,
                            background: toneTokenColor(token, testCaseStatusMeta(status).tone),
                          }}
                        />
                      ))}
                  </div>
                  <Space direction="vertical" size={token.paddingSM} style={{ width: '100%' }}>
                    {distribution.map(({ status, count }) => {
                      const meta = testCaseStatusMeta(status);
                      const percent = totalAll > 0 ? Math.round((count / totalAll) * 100) : 0;
                      return (
                        <div key={status} style={{ display: 'flex', alignItems: 'center', gap: token.paddingSM }}>
                          <span
                            style={{
                              width: 8,
                              height: 8,
                              borderRadius: '50%',
                              background: toneTokenColor(token, meta.tone),
                              flexShrink: 0,
                            }}
                          />
                          <span style={{ flex: 1 }}>{meta.label}</span>
                          <Typography.Text type="secondary">{count} 条</Typography.Text>
                          <Typography.Text type="secondary" style={{ width: 44, textAlign: 'right' }}>
                            {percent}%
                          </Typography.Text>
                        </div>
                      );
                    })}
                  </Space>
                </>
              )}
            </Card>
          </Col>
          <Col xs={24} lg={10}>
            <Card
              bordered={false}
              title="最近更新"
              extra={
                <Button type="link" size="small" onClick={() => navigate('/test-cases')}>
                  查看全部
                </Button>
              }
              style={{ boxShadow: token.boxShadowSecondary }}
            >
              {recentQuery.isError ? (
                <ErrorState error={recentQuery.error} onRetry={() => void recentQuery.refetch()} />
              ) : (
                <List
                  dataSource={recentQuery.data?.content ?? []}
                  loading={recentQuery.isLoading}
                  locale={{ emptyText: <BrandEmpty size="small" description="暂无最近更新" /> }}
                  renderItem={(item: TestCaseSummary) => {
                    const meta = testCaseStatusMeta(item.status);
                    return (
                      <List.Item
                        style={{ cursor: 'pointer', padding: `${token.paddingSM}px 0` }}
                        onClick={() => navigate(`/test-cases/${item.id}`)}
                      >
                        <List.Item.Meta
                          avatar={
                            <div
                              style={{
                                width: 34,
                                height: 34,
                                borderRadius: token.borderRadiusLG,
                                background: token.colorFillQuaternary,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                              }}
                            >
                              <ExperimentOutlined style={{ color: token.colorPrimary }} />
                            </div>
                          }
                          title={
                            <Space size={8}>
                              <Typography.Text strong style={{ fontFamily: token.fontFamilyCode }}>
                                {item.caseCode}
                              </Typography.Text>
                              <StatusTag tone={meta.tone} label={meta.label} />
                            </Space>
                          }
                          description={
                            <span style={{ color: token.colorTextTertiary }}>
                              {item.caseName} · {relativeTime(item.updatedAt)}
                            </span>
                          }
                        />
                      </List.Item>
                    );
                  }}
                />
              )}
            </Card>
          </Col>
        </Row>
      </Space>
    </PageContainer>
  );
}

export default Dashboard;
