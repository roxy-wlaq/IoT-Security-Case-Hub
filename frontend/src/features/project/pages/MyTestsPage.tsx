import { Alert, Table, Tabs, Tag, Typography } from 'antd';
import { Link } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { httpClient } from '@/shared/api/httpClient';
import type { ProjectStatus, ProjectGenerationMode } from '@/shared/types/project';

type MyProject = { id: string; projectNumber: string; projectName: string; deviceName: string; generationMode: ProjectGenerationMode; status: ProjectStatus };
type MyCase = { projectTestCaseId: string; projectNumber: string; caseCode: string; executionStatus: string; removed: boolean; assignedToMe: boolean; newCase: boolean; readOnly: boolean };
export function MyTestsPage() {
  const [projects, setProjects] = useState<MyProject[]>([]); const [cases, setCases] = useState<MyCase[]>([]); const [error, setError] = useState(false);
  useEffect(() => { void Promise.all([httpClient.get<MyProject[]>('/my-projects').then((r) => setProjects(r.data)), httpClient.get<MyCase[]>('/my-cases').then((r) => setCases(r.data))]).catch(() => setError(true)); }, []);
  return <div><Typography.Title level={3}>我的测试</Typography.Title>{error ? <Alert type="error" message="我的测试加载失败" /> : null}<Tabs items={[{ key: 'projects', label: 'My Projects', children: <Table rowKey="id" dataSource={projects} columns={[{ title: '项目', dataIndex: 'projectName' }, { title: '编号', dataIndex: 'projectNumber' }, { title: '设备', dataIndex: 'deviceName' }, { title: '状态', dataIndex: 'status', render: (v: string) => <Tag>{v}</Tag> }]} /> }, { key: 'cases', label: 'My Cases', children: <Table rowKey="projectTestCaseId" dataSource={cases} columns={[{ title: '项目', dataIndex: 'projectNumber' }, { title: 'Case', dataIndex: 'caseCode', render: (v: string, r: MyCase) => <Link to={`/my-tests/${r.projectTestCaseId}`}>{v}</Link> }, { title: '执行状态', dataIndex: 'executionStatus' }, { title: '标记', render: (_: unknown, r: MyCase) => r.newCase ? <Tag color="blue">NEW</Tag> : null }, { title: '权限', render: (_: unknown, r: MyCase) => r.readOnly ? <Tag>只读</Tag> : <Tag color="green">可执行</Tag> }]} /> }]} /></div>;
}
export default MyTestsPage;
