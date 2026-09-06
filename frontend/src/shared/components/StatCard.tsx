import { Card, Skeleton, theme } from 'antd';
import type { ReactNode } from 'react';
import type { StatusTone } from '@/shared/components/StatusTag';

interface ChipTokenShape {
  colorInfo: string;
  colorInfoBg: string;
  colorSuccess: string;
  colorSuccessBg: string;
  colorWarning: string;
  colorWarningBg: string;
  colorError: string;
  colorErrorBg: string;
  colorText: string;
  colorFillSecondary: string;
}

function toneChip(token: ChipTokenShape, tone: StatusTone): { fg: string; bg: string } {
  switch (tone) {
    case 'info':
      return { fg: token.colorInfo, bg: token.colorInfoBg };
    case 'review':
    case 'warning':
      return { fg: token.colorWarning, bg: token.colorWarningBg };
    case 'success':
      return { fg: token.colorSuccess, bg: token.colorSuccessBg };
    case 'danger':
      return { fg: token.colorError, bg: token.colorErrorBg };
    default:
      return { fg: token.colorText, bg: token.colorFillSecondary };
  }
}

export interface StatCardProps {
  icon: ReactNode;
  label: string;
  value?: ReactNode;
  tone?: StatusTone;
  loading?: boolean;
  onClick?: () => void;
}

/** Dashboard stat card: tinted icon chip + label + big number, dark-safe. */
export function StatCard({ icon, label, value, tone = 'info', loading, onClick }: StatCardProps) {
  const { token } = theme.useToken();
  const chip = toneChip(token, tone);

  return (
    <Card
      bordered={false}
      hoverable={Boolean(onClick)}
      style={{ boxShadow: token.boxShadowSecondary, cursor: onClick ? 'pointer' : undefined }}
      onClick={onClick}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: token.padding }}>
        {loading ? (
          <div
            style={{
              width: 48,
              height: 48,
              borderRadius: token.borderRadiusLG,
              background: token.colorFillQuaternary,
              flexShrink: 0,
            }}
          />
        ) : (
          <div
            style={{
              width: 48,
              height: 48,
              borderRadius: token.borderRadiusLG,
              background: chip.bg,
              color: chip.fg,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 22,
              flexShrink: 0,
            }}
          >
            {icon}
          </div>
        )}
        <div style={{ minWidth: 0 }}>
          <div style={{ color: token.colorTextSecondary, fontSize: token.fontSizeSM, marginBottom: 2 }}>
            {label}
          </div>
          {loading ? (
            <Skeleton active title={{ width: 72 }} paragraph={false} />
          ) : (
            <div style={{ fontSize: token.fontSizeHeading2, fontWeight: 500, lineHeight: 1.3 }}>
              {value ?? '—'}
            </div>
          )}
        </div>
      </div>
    </Card>
  );
}

export default StatCard;
