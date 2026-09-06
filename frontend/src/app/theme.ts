import { theme as antdTheme } from 'antd';
import type { ThemeConfig } from 'antd';

export type ThemeMode = 'light' | 'dark';

/** Enterprise brand blue — distinct from antd's stock #1677ff. Tunable. */
export const BRAND_PRIMARY = '#1d4ed8';

const FONT_STACK = `-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'PingFang SC', 'Microsoft YaHei', sans-serif`;

const sharedToken: ThemeConfig['token'] = {
  colorPrimary: BRAND_PRIMARY,
  colorInfo: '#1677ff',
  colorSuccess: '#52c41a',
  colorWarning: '#faad14',
  colorError: '#ff4d4f',
  borderRadius: 8,
  borderRadiusLG: 12,
  wireframe: false,
  fontFamily: FONT_STACK,
};

/**
 * Full Ant Design theme config for the given mode. Drives ConfigProvider so
 * every antd component (Table, Menu, Card, …) follows the brand + dark mode.
 * Custom layout components read CSS tokens from tokens.css instead, which are
 * kept in sync with the values below.
 */
export function getThemeConfig(mode: ThemeMode): ThemeConfig {
  const isDark = mode === 'dark';
  return {
    algorithm: isDark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
    token: sharedToken,
    components: {
      Layout: {
        headerBg: isDark ? '#141414' : '#ffffff',
        siderBg: isDark ? '#141414' : '#ffffff',
        bodyBg: isDark ? '#000000' : '#f0f2f5',
      },
      Menu: {
        itemHeight: 40,
        itemMarginInline: 8,
        subMenuItemBg: 'transparent',
        iconSize: 16,
      },
      Table: {
        headerBg: isDark ? '#1f1f1f' : '#fafafa',
        rowHoverBg: isDark ? '#1f1f1f' : '#fafafa',
        cellPaddingBlock: 14,
      },
      Card: {
        paddingLG: 24,
      },
      Button: {
        controlHeight: 32,
      },
    },
  };
}
