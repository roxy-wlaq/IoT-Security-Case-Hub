import { Button, Empty, theme } from 'antd';
import type { ReactNode } from 'react';

export interface BrandEmptyProps {
  description?: ReactNode;
  /** Optional primary call-to-action button label. */
  cta?: ReactNode;
  onCta?: () => void;
  size?: 'default' | 'small';
}

/**
 * Branded empty state: shield + nodes illustration drawn with CSS design
 * tokens, so it follows light/dark automatically. Used as table/list
 * emptyText everywhere for consistency.
 */
export function BrandEmpty({ description = '暂无数据', cta, onCta, size = 'default' }: BrandEmptyProps) {
  const { token } = theme.useToken();
  const scale = size === 'small' ? 0.7 : 1;
  const w = Math.round(120 * scale);
  const h = Math.round(90 * scale);

  return (
    <Empty
      image={
        <svg width={w} height={h} viewBox="0 0 120 90" aria-hidden="true">
          <path
            d="M60 10 L80 18 V40 C80 53 71 62 60 66 C49 62 40 53 40 40 V18 Z"
            fill="none"
            stroke="var(--color-brand)"
            strokeOpacity="0.4"
            strokeWidth="2.5"
            strokeLinejoin="round"
          />
          <circle cx="60" cy="31" r="3.2" fill="var(--color-brand)" fillOpacity="0.5" />
          <circle cx="51.5" cy="45" r="3.2" fill="var(--color-brand)" fillOpacity="0.3" />
          <circle cx="68.5" cy="45" r="3.2" fill="var(--color-brand)" fillOpacity="0.3" />
          <path
            d="M60 31 L51.5 45 M60 31 L68.5 45 M51.5 45 L68.5 45"
            stroke="var(--color-brand)"
            strokeOpacity="0.28"
            strokeWidth="1.5"
          />
          <path
            d="M28 78 H92"
            stroke="var(--color-border)"
            strokeWidth="2"
            strokeDasharray="4 5"
            strokeLinecap="round"
          />
        </svg>
      }
      imageStyle={{ height: h, display: 'flex', justifyContent: 'center', marginTop: size === 'small' ? 0 : 8 }}
      description={<span style={{ color: token.colorTextTertiary }}>{description}</span>}
    >
      {cta && onCta ? (
        <Button type="primary" ghost onClick={onCta}>
          {cta}
        </Button>
      ) : null}
    </Empty>
  );
}

export default BrandEmpty;
