import { Button, Result } from 'antd';
import { resolveErrorMessage, toApiError } from '@/shared/api/apiError';

export interface ErrorStateProps {
  error: unknown;
  onRetry?: () => void;
  title?: string;
  description?: string;
}

/**
 * 统一的错误展示。403 由页面渲染 ErrorState / ForbiddenPage，不做全局弹窗轰炸。
 */
export function ErrorState({ error, onRetry, title = '加载失败', description }: ErrorStateProps) {
  const apiError = toApiError(error);
  const subTitle = description ?? resolveErrorMessage(apiError.code, apiError.message);

  return (
    <Result
      status="error"
      title={title}
      subTitle={
        <span>
          {subTitle}
          {apiError.traceId ? (
            <span style={{ display: 'block', marginTop: 8, color: 'rgba(0, 0, 0, 0.45)' }}>traceId：{apiError.traceId}</span>
          ) : null}
        </span>
      }
      extra={
        onRetry ? (
          <Button type="primary" onClick={onRetry}>
            重试
          </Button>
        ) : null
      }
    />
  );
}

export default ErrorState;
