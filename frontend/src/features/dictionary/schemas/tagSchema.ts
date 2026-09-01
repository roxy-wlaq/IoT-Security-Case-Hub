import { z } from 'zod';

/**
 * 标签表单校验。
 */
export const tagSchema = z.object({
  name: z.string().trim().min(1, '请输入名称').max(100, '名称最多 100 个字符'),
  description: z.string().trim().max(500, '描述最多 500 个字符').optional().or(z.literal('')),
  enabled: z.boolean(),
});

export type TagFormValues = z.infer<typeof tagSchema>;

export const TAG_FORM_DEFAULTS: TagFormValues = {
  name: '',
  description: '',
  enabled: true,
};
