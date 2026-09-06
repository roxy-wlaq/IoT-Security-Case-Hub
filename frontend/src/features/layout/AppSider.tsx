import { Layout, Menu, theme } from 'antd';
import type { MenuProps } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useCurrentUser } from '@/features/auth/hooks/useCurrentUser';
import {
  filterNavigation,
  NAVIGATION_ITEMS,
  NAV_GROUP_META,
  type NavGroup,
} from '@/shared/config/navigation';
import { useThemeMode } from '@/shared/contexts/ThemeModeContext';
import { BrandLogo } from '@/shared/components/BrandLogo';

const { Sider } = Layout;
const SIDER_COLLAPSED_KEY = 'casehub.sider.collapsed';

function readCollapsed(): boolean {
  if (typeof window === 'undefined') return false;
  return window.localStorage.getItem(SIDER_COLLAPSED_KEY) === 'true';
}

const GROUP_ORDER: NavGroup[] = ['business', 'admin', 'system'];

type MenuItem = NonNullable<MenuProps['items']>[number];

export function AppSider() {
  const { data: user } = useCurrentUser();
  const navigate = useNavigate();
  const location = useLocation();
  const { mode } = useThemeMode();
  const { token } = theme.useToken();
  const [collapsed, setCollapsed] = useState<boolean>(readCollapsed);

  const handleCollapse = (value: boolean) => {
    setCollapsed(value);
    try {
      window.localStorage.setItem(SIDER_COLLAPSED_KEY, String(value));
    } catch {
      /* ignore */
    }
  };

  const visible = filterNavigation(NAVIGATION_ITEMS, user);

  const selectedKey =
    NAVIGATION_ITEMS.find(
      (item) => item.path !== '/' && location.pathname.startsWith(item.path),
    )?.path ?? (location.pathname === '/' ? '/' : '');

  const buildLeaf = (item: (typeof visible)[number]): MenuItem => {
    const Icon = item.icon;
    return {
      key: item.path,
      icon: Icon ? <Icon /> : null,
      label: item.label,
    };
  };

  const menuItems: MenuItem[] = [];
  const top = visible.find((i) => !i.group);
  if (top) menuItems.push(buildLeaf(top));
  for (const g of GROUP_ORDER) {
    const children = visible.filter((i) => i.group === g);
    if (!children.length) continue;
    menuItems.push({
      type: 'group',
      key: `g-${g}`,
      label: NAV_GROUP_META[g].label,
      children: children.map(buildLeaf),
    });
  }

  return (
    <Sider
      width={232}
      collapsedWidth={64}
      collapsible
      collapsed={collapsed}
      onCollapse={handleCollapse}
      theme={mode === 'dark' ? 'dark' : 'light'}
      style={{ borderRight: '1px solid var(--color-border-secondary)' }}
    >
      <div
        style={{
          height: 64,
          display: 'flex',
          alignItems: 'center',
          padding: collapsed ? '0 16px' : '0 20px',
          borderBottom: '1px solid var(--color-border-secondary)',
        }}
      >
        <BrandLogo collapsed={collapsed} size={28} />
      </div>
      <Menu
        mode="inline"
        selectedKeys={selectedKey ? [selectedKey] : []}
        items={menuItems}
        style={{ borderRight: 0, paddingTop: token.paddingSM }}
        onClick={({ key }) => navigate(key)}
      />
    </Sider>
  );
}

export default AppSider;
