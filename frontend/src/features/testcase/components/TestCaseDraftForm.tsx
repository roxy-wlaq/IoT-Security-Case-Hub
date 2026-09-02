import { zodResolver } from '@hookform/resolvers/zod';
import { Button, Form, Input, Select, Space, Switch } from 'antd';
import { useEffect } from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { TEST_CASE_DRAFT_DEFAULTS, testCaseDraftSchema } from '@/features/testcase/schemas/testCaseSchema';
import type { TestCaseDraftFormValues } from '@/features/testcase/schemas/testCaseSchema';

interface Option { value: string; label: string }
interface Props {
  initialValues?: Partial<TestCaseDraftFormValues>;
  isCreate?: boolean;
  readOnly?: boolean;
  onSubmit: (values: TestCaseDraftFormValues) => void | Promise<void>;
  pending?: boolean;
  categoryOptions?: Option[];
  tagOptions?: Option[];
  toolOptions?: Option[];
  standardOptions?: Option[];
}

export function TestCaseDraftForm({ initialValues, isCreate = false, readOnly = false, onSubmit, pending = false,
  categoryOptions = [], tagOptions = [], toolOptions = [], standardOptions = [] }: Props) {
  const { register, control, handleSubmit, reset, setValue, watch, formState: { errors } } = useForm<TestCaseDraftFormValues>({
    resolver: zodResolver(testCaseDraftSchema), defaultValues: { ...TEST_CASE_DRAFT_DEFAULTS, ...initialValues }, mode: 'onSubmit',
  });
  const { fields, append, remove, move } = useFieldArray({ control, name: 'steps' });
  const { remove: removeMapping, replace: replaceMappings } = useFieldArray({ control, name: 'standardMappings' });
  useEffect(() => {
    const nextValues = { ...TEST_CASE_DRAFT_DEFAULTS, ...initialValues };
    reset(nextValues);
    replaceMappings(nextValues.standardMappings);
  }, [initialValues, reset, replaceMappings]);
  const selectionMode = watch('selectionMode');
  const evidenceRequired = watch('evidenceRequired');
  const watchedStandardMappings = watch('standardMappings') ?? [];
  const standardMappings = watchedStandardMappings.length > 0 ? watchedStandardMappings : (initialValues?.standardMappings ?? []);
  const disabled = readOnly || pending;

  return (
    <Form layout="vertical" onFinish={() => void handleSubmit(onSubmit)()}>
      {isCreate ? <Form.Item label="用例编码" required validateStatus={errors.caseCode ? 'error' : undefined} help={errors.caseCode?.message}>
        <Input {...register('caseCode')} disabled={disabled} />
      </Form.Item> : null}
      {isCreate ? <Form.Item label="分类" required validateStatus={errors.categoryId ? 'error' : undefined} help={errors.categoryId?.message}>
        <Select value={watch('categoryId')} options={categoryOptions} onChange={(value) => setValue('categoryId', value, { shouldValidate: true })} disabled={disabled} />
      </Form.Item> : null}
      <Form.Item label="测试用例名称" required validateStatus={errors.caseName ? 'error' : undefined} help={errors.caseName?.message}>
        <Input {...register('caseName')} disabled={disabled} />
      </Form.Item>
      <Form.Item label="测试目的"><Input.TextArea {...register('testPurpose')} rows={3} disabled={disabled} /></Form.Item>
      <Form.Item label="前置条件"><Input.TextArea {...register('preconditions')} rows={3} disabled={disabled} /></Form.Item>
      <Form.Item label="选择模式" required validateStatus={errors.selectionMode ? 'error' : undefined} help={errors.selectionMode?.message}>
        <Select value={selectionMode} options={[{ value: 'SINGLE', label: '单选' }, { value: 'MULTIPLE', label: '多选' }]} onChange={(value) => setValue('selectionMode', value, { shouldValidate: true })} disabled={disabled} />
      </Form.Item>
      <Form.Item label="需要证据"><Switch checked={evidenceRequired} onChange={(value) => setValue('evidenceRequired', value)} disabled={disabled} /></Form.Item>
      <Form.Item label="证据要求"><Input.TextArea {...register('evidenceRequirement')} rows={2} disabled={disabled} /></Form.Item>
      <Form.Item label="备注要求"><Input.TextArea {...register('remarkRequirement')} rows={2} disabled={disabled} /></Form.Item>
      <Form.Item label="渐进角色">
        <Select value={watch('progressiveRole') ?? undefined} allowClear placeholder="None" options={[{ value: 'ENTRY', label: 'ENTRY' }, { value: 'NORMAL', label: 'NORMAL' }]} onChange={(value) => setValue('progressiveRole', value ?? null, { shouldValidate: true })} disabled={disabled} />
      </Form.Item>
      <Form.Item label="步骤">
        {fields.map((field, index) => <Space key={field.id} direction="vertical" style={{ display: 'flex', marginBottom: 12 }}>
          <Input placeholder={`步骤 ${index + 1} 标题`} {...register(`steps.${index}.title`)} disabled={disabled} />
          <Input.TextArea aria-label={`步骤 ${index + 1} 内容`} placeholder="步骤内容" {...register(`steps.${index}.content`)} disabled={disabled} />
          {errors.steps?.[index]?.content ? <span role="alert">{errors.steps[index]?.content?.message}</span> : null}
          {!disabled ? <Space><Button onClick={() => index > 0 && move(index, index - 1)}>上移</Button><Button onClick={() => index < fields.length - 1 && move(index, index + 1)}>下移</Button><Button onClick={() => remove(index)}>删除</Button></Space> : null}
        </Space>)}
        {!disabled ? <Button onClick={() => append({ title: '', content: '' })}>新增步骤</Button> : null}
      </Form.Item>
      <Form.Item label="标签"><Select mode="multiple" options={tagOptions} value={watch('tagIds')} onChange={(value) => setValue('tagIds', value)} disabled={disabled} /></Form.Item>
      <Form.Item label="工具"><Select mode="multiple" options={toolOptions} value={watch('toolIds')} onChange={(value) => setValue('toolIds', value)} disabled={disabled} /></Form.Item>
      <Form.Item label="标准/任务类型"><Select mode="multiple" options={standardOptions} value={standardMappings.map((item) => item.standardTaskTypeId)} onChange={(values: string[]) => {
        const current = standardMappings;
        setValue('standardMappings', values.map((value) => ({ standardTaskTypeId: value, mappingNote: current.find((item) => item.standardTaskTypeId === value)?.mappingNote ?? '' })), { shouldDirty: true });
      }} disabled={disabled} /></Form.Item>
      {standardMappings.map((mapping, index) => <Space key={`${mapping.standardTaskTypeId}-${index}`} style={{ display: 'flex', marginBottom: 8 }}>
        <Input placeholder="映射备注" defaultValue={mapping.mappingNote ?? ''} {...register(`standardMappings.${index}.mappingNote`)} disabled={disabled} />
        {!disabled ? <Button onClick={() => removeMapping(index)}>移除映射</Button> : null}
      </Space>)}
      {!readOnly ? <Button type="primary" htmlType="submit" loading={pending}>保存 Draft</Button> : null}
    </Form>
  );
}
