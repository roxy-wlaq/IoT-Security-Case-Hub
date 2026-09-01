import { Spin } from 'antd';

export interface LoadingStateProps {
  tip?: string;
  /** 是否需要撑满一屏（用于 RouteGuard / AppLayout 的首屏加载） */
  block?: boolean;
}

export function LoadingState({ tip = '加载中…', block = false }: LoadingStateProps) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 12,
        minHeight: block ? '60vh' : 120,
        padding: 24,
      }}
    >
      <Spin size="large" />
      <span style={{ color: 'rgba(0, 0, 0, 0.45)' }}>{tip}</span>
    </div>
  );
}

export default LoadingState;
