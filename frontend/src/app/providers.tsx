import { useEffect } from 'react';
import type { ReactNode } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { App as AntdApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { queryClient } from '@/app/queryClient';
import { setUnauthorizedHandler } from '@/shared/api/httpClient';
import { currentUserQueryKey } from '@/features/auth/hooks/useCurrentUser';

/**
 * 应用级 Provider：TanStack Query + Ant Design（中文语言包 + 主题）。
 *
 * 全局 401 处理：非静默探测路径（/auth/me、/auth/csrf 除外）收到 401 时，
 * 清空当前用户缓存并触发重新拉取 —— RouteGuard 会因 /me 返回 401 而重定向到登录页。
 */
export function AppProviders({ children }: { children: ReactNode }) {
  useEffect(() => {
    setUnauthorizedHandler(() => {
      queryClient.setQueryData(currentUserQueryKey, null);
      void queryClient.invalidateQueries({ queryKey: currentUserQueryKey });
    });
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider
        locale={zhCN}
        theme={{
          token: {
            colorPrimary: '#1677ff',
            borderRadius: 6,
          },
        }}
      >
        <AntdApp>{children}</AntdApp>
      </ConfigProvider>
    </QueryClientProvider>
  );
}

export default AppProviders;
