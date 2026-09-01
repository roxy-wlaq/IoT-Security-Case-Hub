import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Form, Input } from 'antd';
import { useForm } from 'react-hook-form';
import { useChangePassword } from '@/features/auth/hooks/useChangePassword';
import {
  changePasswordSchema,
  type ChangePasswordFormValues,
} from '@/features/auth/schemas/authSchemas';
import { API_ERROR_CODES, toApiError } from '@/shared/api/apiError';
import type { CurrentUser } from '@/shared/types/auth';

interface ChangePasswordFormProps {
  user: CurrentUser;
  onSuccess?: () => void;
}

export function ChangePasswordForm({ user, onSuccess }: ChangePasswordFormProps) {
  const changePasswordMutation = useChangePassword();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: {
      username: user.username,
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
    mode: 'onSubmit',
  });

  const submitError = changePasswordMutation.error ? toApiError(changePasswordMutation.error) : null;

  const onSubmit = handleSubmit(async (values) => {
    changePasswordMutation.reset();
    try {
      await changePasswordMutation.mutateAsync({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      onSuccess?.();
    } catch {
      // 错误信息已由 changePasswordMutation.error 承载
    }
  });

  return (
    <Form layout="vertical" onFinish={onSubmit} requiredMark={false} disabled={changePasswordMutation.isPending}>
      <Alert
        type="warning"
        showIcon
        style={{ marginBottom: 16 }}
        message="请先修改密码"
        description="出于安全策略，首次登录后需要设置新密码才能继续使用系统。"
      />

      {submitError ? (
        <Form.Item style={{ marginBottom: 16 }}>
          <Alert
            type="error"
            showIcon
            message={submitError.userMessage}
            description={
              submitError.traceId &&
              (submitError.code === API_ERROR_CODES.NETWORK_ERROR || submitError.status >= 500)
                ? `traceId：${submitError.traceId}`
                : undefined
            }
          />
        </Form.Item>
      ) : null}

      <Form.Item
        label="当前密码"
        validateStatus={errors.currentPassword ? 'error' : undefined}
        help={errors.currentPassword?.message}
        required
      >
        <Input.Password {...register('currentPassword')} autoComplete="current-password" placeholder="请输入当前密码" />
      </Form.Item>

      <Form.Item
        label="新密码"
        validateStatus={errors.newPassword ? 'error' : undefined}
        help={errors.newPassword?.message}
        required
      >
        <Input.Password {...register('newPassword')} autoComplete="new-password" placeholder="至少 12 位" />
      </Form.Item>

      <Form.Item
        label="确认新密码"
        validateStatus={errors.confirmPassword ? 'error' : undefined}
        help={errors.confirmPassword?.message}
        required
      >
        <Input.Password {...register('confirmPassword')} autoComplete="new-password" placeholder="请再次输入新密码" />
      </Form.Item>

      <Form.Item style={{ marginBottom: 0 }}>
        <Button type="primary" htmlType="submit" block loading={changePasswordMutation.isPending}>
          修改密码
        </Button>
      </Form.Item>
    </Form>
  );
}

export default ChangePasswordForm;
