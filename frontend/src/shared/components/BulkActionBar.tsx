import { Button, Space, theme } from 'antd';
import type { ReactNode } from 'react';
import { CloseOutlined } from '@ant-design/icons';

export interface BulkActionBarProps {
  count: number;
  onClear: () => void;
  /** Action buttons rendered when the bar is visible. */
  children?: ReactNode;
}

/** Sticky-tinted action bar shown while table rows are selected. */
export function BulkActionBar({ count, onClear, children }: BulkActionBarProps) {
  const { token } = theme.useToken();
  if (count <= 0) {
    return null;
  }
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: token.paddingSM,
        flexWrap: 'wrap',
        padding: `${token.paddingSM}px ${token.padding}px`,
        marginBottom: token.padding,
        borderRadius: token.borderRadius,
        background: 'var(--color-brand-soft)',
        border: `1px solid ${token.colorBorderSecondary}`,
      }}
    >
      <span style={{ color: 'var(--color-text-primary)', fontWeight: 500 }}>
        已选择 {count} 项
      </span>
      <Space wrap>
        {children}
        <Button size="small" icon={<CloseOutlined />} onClick={onClear}>
          清除选择
        </Button>
      </Space>
    </div>
  );
}

export default BulkActionBar;
