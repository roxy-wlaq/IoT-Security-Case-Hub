import { Alert, Button, Form, Input, Select, Table, Tag, Typography } from 'antd';
import { useCallback, useEffect, useState } from 'react';
import { AUDIT_ACTIONS, listAuditLogs, type AuditAction, type AuditLog } from '@/features/audit/api/auditApi';

export function AuditPage() {
  const [rows, setRows] = useState<AuditLog[]>([]);
  const [page, setPage] = useState({ page: 0, size: 20, total: 0 });
  const [filters, setFilters] = useState<{ action?: AuditAction; resourceType?: string; resourceId?: string; actorUsername?: string }>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>();
  const load = useCallback(async (nextPage = 0, nextFilters = filters) => {
    setLoading(true); setError(undefined);
    try { const result = await listAuditLogs({ ...nextFilters, page: nextPage, size: page.size }); setRows(result.content); setPage((old) => ({ ...old, page: result.page, total: result.totalElements })); }
    catch { setError('审计日志加载失败'); }
    finally { setLoading(false); }
  }, [filters, page.size]);
  useEffect(() => { void load(0, filters); }, [load, filters]);
  return <div>
    <Typography.Title level={3}>审计日志</Typography.Title>
    {error ? <Alert type="error" message={error} showIcon /> : null}
    <Form layout="inline" onFinish={(values) => { const next = { ...values, action: values.action || undefined }; setFilters(next); }} style={{ marginBottom: 16 }}>
      <Form.Item name="action"><Select allowClear placeholder="Action" style={{ width: 220 }} options={AUDIT_ACTIONS.map((value) => ({ value, label: value }))} /></Form.Item>
      <Form.Item name="resourceType"><Input placeholder="Resource type" /></Form.Item>
      <Form.Item name="resourceId"><Input placeholder="Resource ID" /></Form.Item>
      <Form.Item name="actorUsername"><Input placeholder="Actor username" /></Form.Item>
      <Button htmlType="submit" type="primary">查询</Button>
    </Form>
    <Table rowKey="id" loading={loading} dataSource={rows} pagination={{ current: page.page + 1, pageSize: page.size, total: page.total, showSizeChanger: false, onChange: (next) => void load(next - 1) }} columns={[
      { title: '时间', dataIndex: 'occurredAt', render: (value: string) => new Date(value).toLocaleString() },
      { title: 'Action', dataIndex: 'action', render: (value: AuditAction) => <Tag>{value}</Tag> },
      { title: 'Actor', dataIndex: 'actorUsername' },
      { title: 'Resource', render: (_: unknown, row: AuditLog) => `${row.resourceType}${row.resourceLabel ? ` · ${row.resourceLabel}` : ''}` },
      { title: 'Resource ID', dataIndex: 'resourceId' },
      { title: 'Detail', dataIndex: 'detail', render: (value: Record<string, unknown> | null) => value ? JSON.stringify(value) : '' },
    ]} />
  </div>;
}

export default AuditPage;
