import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Form, Input } from 'antd';
import { useForm } from 'react-hook-form';
import { useLogin } from '@/features/auth/hooks/useLogin';
import { loginSchema } from '@/features/auth/schemas/authSchemas';
import type { LoginFormValues } from '@/features/auth/schemas/authSchemas';
import { API_ERROR_CODES, toApiError } from '@/shared/api/apiError';
import type { CurrentUser } from '@/shared/types/auth';

export interface LoginFormProps {
  onSuccess?: (user: CurrentUser) => void;
}

const DEFAULT_VALUES: LoginFormValues = { username: '', password: '' };

export function LoginForm({ onSuccess }: LoginFormProps) {
  const loginMutation = useLogin();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: DEFAULT_VALUES,
    mode: 'onSubmit',
  });

  const submitError = loginMutation.error ? toApiError(loginMutation.error) : null;

  const onSubmit = handleSubmit(async (values) => {
    loginMutation.reset();
    try {
      const user = await loginMutation.mutateAsync(values);
      onSuccess?.(user);
    } catch {
      // 错误信息已由 loginMutation.error 承载，在此处吞掉以避免 unhandled rejection
    }
  });

  return (
    <Form layout="vertical" onFinish={onSubmit} requiredMark={false} disabled={loginMutation.isPending}>
      {submitError ? (
        <Form.Item style={{ marginBottom: 16 }}>
          <Alert
            type="error"
            showIcon
            message={submitError.userMessage}
            description={
              submitError.traceId && (submitError.code === API_ERROR_CODES.NETWORK_ERROR || submitError.status >= 500)
                ? `traceId：${submitError.traceId}`
                : undefined
            }
          />
        </Form.Item>
      ) : null}

      <Form.Item
        label="用户名"
        validateStatus={errors.username ? 'error' : undefined}
        help={errors.username?.message}
        required
      >
        <Input {...register('username')} autoComplete="username" placeholder="请输入用户名" autoFocus />
      </Form.Item>

      <Form.Item
        label="密码"
        validateStatus={errors.password ? 'error' : undefined}
        help={errors.password?.message}
        required
      >
        <Input.Password {...register('password')} autoComplete="current-password" placeholder="请输入密码" />
      </Form.Item>

      <Form.Item style={{ marginBottom: 0 }}>
        <Button type="primary" htmlType="submit" block loading={loginMutation.isPending}>
          登录
        </Button>
      </Form.Item>
    </Form>
  );
}

export default LoginForm;
