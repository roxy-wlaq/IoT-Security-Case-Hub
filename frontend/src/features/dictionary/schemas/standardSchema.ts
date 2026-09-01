import { z } from 'zod';
import { STANDARD_TYPES } from '@/shared/types/dictionary';

/**
 * 标准任务类型 / 任务类型 表单校验。
 */
export const standardSchema = z.object({
  code: z.string().trim().min(1, '请输入编码').max(100, '编码最多 100 个字符'),
  name: z.string().trim().min(1, '请输入名称').max(200, '名称最多 200 个字符'),
  type: z.enum(STANDARD_TYPES as unknown as [string, ...string[]], {
    errorMap: () => ({ message: '请选择类型' }),
  }),
  description: z.string().trim().max(500, '描述最多 500 个字符').optional().or(z.literal('')),
  enabled: z.boolean(),
});

export type StandardFormValues = z.infer<typeof standardSchema>;

export const STANDARD_FORM_DEFAULTS: StandardFormValues = {
  code: '',
  name: '',
  type: 'STANDARD',
  description: '',
  enabled: true,
};
