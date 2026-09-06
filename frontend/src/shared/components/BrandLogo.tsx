import type { CSSProperties } from 'react';

export interface BrandLogoProps {
  size?: number;
  /** Show the wordmark next to the mark. */
  showText?: boolean;
  /** Collapsed sider: mark only. */
  collapsed?: boolean;
  style?: CSSProperties;
}

/**
 * Product logo: a shield (security) with connected nodes (IoT) — the visual
 * identity for the platform. Uses CSS brand tokens so it follows light/dark.
 */
export function BrandLogo({
  size = 28,
  showText = true,
  collapsed = false,
  style,
}: BrandLogoProps) {
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 10, ...style }}>
      <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
        <path
          d="M16 2 L28 7 V16 C28 23 23 28 16 30 C9 28 4 23 4 16 V7 Z"
          fill="var(--color-brand)"
        />
        <circle cx="16" cy="11" r="2.3" fill="#fff" />
        <circle cx="10.4" cy="19.2" r="2.3" fill="#fff" />
        <circle cx="21.6" cy="19.2" r="2.3" fill="#fff" />
        <path
          d="M16 11 L10.4 19.2 M16 11 L21.6 19.2 M10.4 19.2 L21.6 19.2"
          stroke="#fff"
          strokeWidth="1.4"
          fill="none"
        />
      </svg>
      {showText && !collapsed ? (
        <span
          style={{
            fontWeight: 500,
            fontSize: 16,
            color: 'var(--color-text-primary)',
            letterSpacing: 0.2,
            whiteSpace: 'nowrap',
          }}
        >
          IoT Case Hub
        </span>
      ) : null}
    </span>
  );
}
