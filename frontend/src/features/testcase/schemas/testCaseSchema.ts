import { z } from 'zod';

const stepSchema = z.object({
  title: z.string().trim().max(255, '步骤标题最多 255 个字符').optional().or(z.literal('')),
  content: z.string().trim().min(1, '请输入步骤内容'),
});

export const testCaseDraftSchema = z.object({
  caseCode: z.string().trim().max(100, '编码最多 100 个字符').optional().or(z.literal('')),
  categoryId: z.string().min(1, '请选择分类').optional().or(z.literal('')),
  caseName: z.string().trim().min(1, '请输入测试用例名称').max(255, '名称最多 255 个字符'),
  testPurpose: z.string().optional().or(z.literal('')),
  preconditions: z.string().optional().or(z.literal('')),
  selectionMode: z.enum(['SINGLE', 'MULTIPLE']),
  evidenceRequired: z.boolean(),
  evidenceRequirement: z.string().optional().or(z.literal('')),
  remarkRequirement: z.string().optional().or(z.literal('')),
  progressiveRole: z.enum(['ENTRY', 'NORMAL']).nullable().optional(),
  steps: z.array(stepSchema),
  tagIds: z.array(z.string()),
  toolIds: z.array(z.string()),
  standardMappings: z.array(z.object({ standardTaskTypeId: z.string(), mappingNote: z.string().optional() })),
});

export type TestCaseDraftFormValues = z.infer<typeof testCaseDraftSchema>;

export const TEST_CASE_DRAFT_DEFAULTS: TestCaseDraftFormValues = {
  caseCode: '', categoryId: '', caseName: '', testPurpose: '', preconditions: '', selectionMode: 'SINGLE',
  evidenceRequired: false, evidenceRequirement: '', remarkRequirement: '', progressiveRole: null,
  steps: [], tagIds: [], toolIds: [], standardMappings: [],
};

// ---------------------------------------------------------------------------
// Phase 7 — Test Case Lifecycle schemas
// ---------------------------------------------------------------------------

/** Comment required by the backend for Return and Reject (VALIDATION_FAILED if blank). */
export const lifecycleCommentSchema = z.object({
  comment: z.string().trim().min(1, '请填写评审意见').max(2000, '评审意见最多 2000 个字符'),
});
export type LifecycleCommentValues = z.infer<typeof lifecycleCommentSchema>;

/** Optional change reason recorded on the new revision draft. */
export const createRevisionSchema = z.object({
  changeReason: z.string().trim().max(2000, '变更说明最多 2000 个字符').optional().or(z.literal('')),
});
export type CreateRevisionFormValues = z.infer<typeof createRevisionSchema>;

/** Add-contributor payload: userId is mandatory. */
export const addContributorSchema = z.object({
  userId: z.string().min(1, '请选择贡献者'),
});
export type AddContributorFormValues = z.infer<typeof addContributorSchema>;
