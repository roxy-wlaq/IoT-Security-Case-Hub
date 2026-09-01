import { z } from 'zod';

/**
 * 前端表单校验。
 *
 * 登录：用户名必填、密码必填且长度 >= 1。
 * 刻意不限制 12 位 —— 那是后端密码策略（Security & RBAC Detail V1.0 §18-§19），
 * 前端不应对存量/初始密码做长度假设。
 */
export const loginSchema = z.object({
  username: z.string().trim().min(1, '请输入用户名'),
  password: z.string().min(1, '请输入密码'),
});

export type LoginFormValues = z.infer<typeof loginSchema>;

export const NEW_PASSWORD_MIN_LENGTH = 12;
export const NEW_PASSWORD_MAX_LENGTH = 128;

/**
 * 修改密码校验（Security & RBAC Detail V1.0 §19）：
 * 长度 12-128、不能全空白、不能等于 username、两次输入一致。
 */
export const changePasswordSchema = z
  .object({
    username: z.string(),
    currentPassword: z.string().min(1, '请输入当前密码'),
    newPassword: z
      .string()
      .min(NEW_PASSWORD_MIN_LENGTH, `新密码至少 ${NEW_PASSWORD_MIN_LENGTH} 位`)
      .max(NEW_PASSWORD_MAX_LENGTH, `新密码最多 ${NEW_PASSWORD_MAX_LENGTH} 位`)
      .refine((value) => value.trim().length > 0, '新密码不能全部为空白字符'),
    confirmPassword: z.string().min(1, '请再次输入新密码'),
  })
  .superRefine((values, ctx) => {
    if (values.newPassword !== values.confirmPassword) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['confirmPassword'],
        message: '两次输入的新密码不一致',
      });
    }

    if (values.username && values.newPassword === values.username) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['newPassword'],
        message: '新密码不能与用户名相同',
      });
    }

    if (values.currentPassword && values.currentPassword === values.newPassword) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['newPassword'],
        message: '新密码不能与当前密码相同',
      });
    }
  });

export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>;
