import { useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { App as AntdApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { queryClient } from '@/app/queryClient';
import { setUnauthorizedHandler } from '@/shared/api/httpClient';
import { currentUserQueryKey } from '@/features/auth/hooks/useCurrentUser';
import { getThemeConfig, type ThemeMode } from '@/app/theme';
import { ThemeModeContext } from '@/shared/contexts/ThemeModeContext';

const STORAGE_KEY = 'casehub.theme.mode';
const DEFAULT_MODE: ThemeMode = 'light';

function applyThemeAttr(mode: ThemeMode) {
  document.documentElement.setAttribute('data-theme', mode);
  document.documentElement.style.colorScheme = mode;
}

function readStoredMode(): ThemeMode {
  if (typeof window === 'undefined') return DEFAULT_MODE;
  const stored = window.localStorage.getItem(STORAGE_KEY);
  return stored === 'dark' || stored === 'light' ? stored : DEFAULT_MODE;
}

/**
 * 应用级 Provider：TanStack Query + Ant Design（中文语言包 + 全量品牌主题，
 * 支持亮/暗双模）。全局 401 处理保留不变。
 */
export function AppProviders({ children }: { children: ReactNode }) {
  const [mode, setMode] = useState<ThemeMode>(readStoredMode);

  useEffect(() => {
    applyThemeAttr(mode);
    try {
      window.localStorage.setItem(STORAGE_KEY, mode);
    } catch {
      /* ignore storage errors (private mode) */
    }
  }, [mode]);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      queryClient.setQueryData(currentUserQueryKey, null);
      void queryClient.invalidateQueries({ queryKey: currentUserQueryKey });
    });
  }, []);

  const themeConfig = useMemo(() => getThemeConfig(mode), [mode]);
  const contextValue = useMemo(
    () => ({
      mode,
      setMode,
      toggle: () => setMode((m) => (m === 'dark' ? 'light' : 'dark')),
    }),
    [mode],
  );

  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={zhCN} theme={themeConfig}>
        <AntdApp>
          <ThemeModeContext.Provider value={contextValue}>
            {children}
          </ThemeModeContext.Provider>
        </AntdApp>
      </ConfigProvider>
    </QueryClientProvider>
  );
}

export default AppProviders;
