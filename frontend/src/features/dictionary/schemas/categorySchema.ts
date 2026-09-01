import { z } from 'zod';

/**
 * 分类表单校验。
 *
 * - level=1 时 parentId 可为空
 * - level=2 时 parentId 必填
 */
export const categorySchema = z
  .object({
    code: z.string().trim().min(1, '请输入编码').max(100, '编码最多 100 个字符'),
    name: z.string().trim().min(1, '请输入名称').max(200, '名称最多 200 个字符'),
    level: z.union([z.literal(1), z.literal(2)], {
      errorMap: () => ({ message: '请选择层级' }),
    }),
    parentId: z.string().nullable().optional(),
    description: z.string().trim().max(500, '描述最多 500 个字符').optional().or(z.literal('')),
    sortOrder: z.number().int('排序需为整数').min(0, '排序不能为负').default(0),
    enabled: z.boolean(),
  })
  .superRefine((values, ctx) => {
    if (values.level === 2 && !values.parentId) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['parentId'],
        message: '二级分类必须选择父分类',
      });
    }
    if (values.level === 1 && values.parentId) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['parentId'],
        message: '一级分类不应有父分类',
      });
    }
  });

export type CategoryFormValues = z.infer<typeof categorySchema>;

export const CATEGORY_FORM_DEFAULTS: CategoryFormValues = {
  code: '',
  name: '',
  level: 1,
  parentId: null,
  description: '',
  sortOrder: 0,
  enabled: true,
};
