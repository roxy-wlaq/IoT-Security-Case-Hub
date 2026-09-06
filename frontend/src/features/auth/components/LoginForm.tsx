import { Alert, Button, Form, Input } from 'antd';
import { useState, type FormEvent } from 'react';
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
  const [values, setValues] = useState<LoginFormValues>(DEFAULT_VALUES);
  const [errors, setErrors] = useState<Partial<Record<keyof LoginFormValues, string>>>({});

  const submitError = loginMutation.error ? toApiError(loginMutation.error) : null;

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const parsed = loginSchema.safeParse(values);
    if (!parsed.success) {
      const nextErrors: Partial<Record<keyof LoginFormValues, string>> = {};
      for (const issue of parsed.error.issues) {
        const field = issue.path[0];
        if ((field === 'username' || field === 'password') && !nextErrors[field]) {
          nextErrors[field] = issue.message;
        }
      }
      setErrors(nextErrors);
      return;
    }
    setErrors({});
    loginMutation.reset();
    try {
      const user = await loginMutation.mutateAsync(parsed.data);
      onSuccess?.(user);
    } catch {
      // 错误信息已由 loginMutation.error 承载，在此处吞掉以避免 unhandled rejection
    }
  };

  return (
    <form className="ant-form ant-form-vertical ant-form-hide-required-mark" onSubmit={onSubmit}>
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
        help={errors.username}
        required
      >
        <Input
          name="username"
          value={values.username}
          onChange={(event) => setValues((current) => ({ ...current, username: event.target.value }))}
          autoComplete="username"
          placeholder="请输入用户名"
          autoFocus
        />
      </Form.Item>

      <Form.Item
        label="密码"
        validateStatus={errors.password ? 'error' : undefined}
        help={errors.password}
        required
      >
        <Input.Password
          name="password"
          value={values.password}
          onChange={(event) => setValues((current) => ({ ...current, password: event.target.value }))}
          autoComplete="current-password"
          placeholder="请输入密码"
        />
      </Form.Item>

      <Form.Item style={{ marginBottom: 0 }}>
        <Button
          type="primary"
          htmlType="submit"
          block
          loading={loginMutation.isPending}
        >
          登录
        </Button>
      </Form.Item>
    </form>
  );
}

export default LoginForm;
