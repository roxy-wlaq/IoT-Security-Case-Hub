import { z } from 'zod';

/**
 * 能力表单校验。与后端 Create/UpdateCapabilityRequest 的约束保持一致：
 *   code        @NotBlank @Size(max=120)  且不含空白字符（大小写不敏感唯一）
 *   name        @NotBlank @Size(max=180)
 *   description @Size(max=500)
 */
export const capabilitySchema = z.object({
  code: z
    .string()
    .trim()
    .min(1, '请输入能力编码')
    .max(120, '能力编码最多 120 个字符')
    .regex(/^[^\s]+$/, '能力编码不能包含空白字符'),
  name: z.string().trim().min(1, '请输入能力名称').max(180, '能力名称最多 180 个字符'),
  description: z.string().trim().max(500, '描述最多 500 个字符').optional().or(z.literal('')),
  sortOrder: z.coerce
    .number({ invalid_type_error: '排序值必须是数字' })
    .int('排序值必须是整数')
    .min(0, '排序值不能为负数')
    .max(9999, '排序值最多 9999'),
});

export type CapabilityFormValues = z.infer<typeof capabilitySchema>;

export const CAPABILITY_FORM_DEFAULTS: CapabilityFormValues = {
  code: '',
  name: '',
  description: '',
  sortOrder: 0,
};
