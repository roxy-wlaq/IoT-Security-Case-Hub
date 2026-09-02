import { Alert, Button, Card, Typography } from 'antd';
import { useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { TestCaseDraftForm } from '@/features/testcase/components/TestCaseDraftForm';
import type { TestCaseDraftFormValues } from '@/features/testcase/schemas/testCaseSchema';
import { useCreateTestCase, useTestCase, useUpdateTestCaseDraft } from '@/features/testcase/hooks/useTestCases';
import { useCategories } from '@/features/dictionary/hooks/useCategories';
import { useTags } from '@/features/dictionary/hooks/useTags';
import { useTools } from '@/features/dictionary/hooks/useTools';
import { useStandards } from '@/features/dictionary/hooks/useStandards';
import { toApiError } from '@/shared/api/apiError';

export function TestCaseDraftPage() {
  const { masterId } = useParams<{ masterId: string }>();
  const isCreate = !masterId || masterId === 'new';
  const navigate = useNavigate();
  const detailQuery = useTestCase(masterId ?? '', !isCreate);
  const categories = useCategories({ enabled: true });
  const tags = useTags({ enabled: true });
  const tools = useTools({ enabled: true });
  const standards = useStandards({ enabled: true });
  const createMutation = useCreateTestCase();
  const updateMutation = useUpdateTestCaseDraft();
  const draft = detailQuery.data?.draftVersion ?? detailQuery.data?.visibleVersion;
  const initialValues = useMemo(() => draft ? {
    caseName: draft.caseName, testPurpose: draft.testPurpose ?? '', preconditions: draft.preconditions ?? '', selectionMode: draft.selectionMode,
    evidenceRequired: draft.evidenceRequired, evidenceRequirement: draft.evidenceRequirement ?? '', remarkRequirement: draft.remarkRequirement ?? '',
    progressiveRole: draft.progressiveRole ?? null, steps: draft.steps.map((step) => ({ title: step.title ?? '', content: step.content })),
    tagIds: detailQuery.data?.tags.map((tag) => tag.id) ?? [], toolIds: draft.tools.map((tool) => tool.id),
    standardMappings: draft.standardMappings.map((mapping) => ({ standardTaskTypeId: mapping.standardTaskTypeId, mappingNote: mapping.mappingNote ?? '' })),
  } : undefined, [draft, detailQuery.data]);
  const formOptions = {
    categoryOptions: (categories.data ?? []).flatMap((category) => [category, ...(category.children ?? [])]).map((category) => ({ value: category.id, label: category.name })),
    tagOptions: (tags.data ?? []).map((tag) => ({ value: tag.id, label: tag.name })),
    toolOptions: (tools.data ?? []).map((tool) => ({ value: tool.id, label: tool.name })),
    standardOptions: (standards.data ?? []).map((standard) => ({ value: standard.id, label: standard.name })),
  };
  const onSubmit = async (values: TestCaseDraftFormValues) => {
    const payload = { ...values, progressiveRole: values.progressiveRole ?? undefined };
    if (isCreate) {
      await createMutation.mutateAsync({ ...payload, caseCode: values.caseCode || undefined, categoryId: values.categoryId || undefined });
    } else if (masterId) {
      await updateMutation.mutateAsync({ masterId, payload });
    }
    navigate('/test-cases');
  };
  const error = createMutation.error ?? updateMutation.error ?? detailQuery.error;
  if (!isCreate && detailQuery.isLoading) return <Typography.Text>加载中…</Typography.Text>;
  return <div>
    <Button type="link" onClick={() => navigate('/test-cases')}>返回测试库</Button>
    <Typography.Title level={3}>{isCreate ? '新建测试用例 Draft' : '编辑测试用例 Draft'}</Typography.Title>
    {error ? <Alert type="error" showIcon message={toApiError(error).userMessage} style={{ marginBottom: 16 }} /> : null}
    <Card><TestCaseDraftForm initialValues={initialValues} isCreate={isCreate} readOnly={!isCreate && !detailQuery.data?.allowedActions.editDraft}
      onSubmit={onSubmit} pending={createMutation.isPending || updateMutation.isPending} {...formOptions} /></Card>
  </div>;
}

export default TestCaseDraftPage;
