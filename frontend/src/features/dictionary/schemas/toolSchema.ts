import { z } from 'zod';

/**
 * 工具表单校验。
 */
export const toolSchema = z.object({
  code: z.string().trim().min(1, '请输入编码').max(100, '编码最多 100 个字符'),
  name: z.string().trim().min(1, '请输入名称').max(150, '名称最多 150 个字符'),
  platform: z.string().trim().max(100, '平台最多 100 个字符').optional().or(z.literal('')),
  website: z
    .string()
    .trim()
    .max(500, '网址最多 500 个字符')
    .optional()
    .or(z.literal(''))
    .refine((value) => !value || /^https?:\/\//i.test(value), '网址需以 http:// 或 https:// 开头'),
  description: z.string().trim().max(500, '描述最多 500 个字符').optional().or(z.literal('')),
  enabled: z.boolean(),
});

export type ToolFormValues = z.infer<typeof toolSchema>;

export const TOOL_FORM_DEFAULTS: ToolFormValues = {
  code: '',
  name: '',
  platform: '',
  website: '',
  description: '',
  enabled: true,
};
