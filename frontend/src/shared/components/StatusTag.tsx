import { Tag } from 'antd';
import type { ReactNode } from 'react';
import type { CSSProperties } from 'react';
import type { TestCaseStatus } from '@/shared/types/testCase';

export type StatusTone =
  | 'neutral'
  | 'info'
  | 'review'
  | 'success'
  | 'warning'
  | 'danger';

const TONE_COLOR: Record<StatusTone, string> = {
  neutral: 'default',
  info: 'blue',
  review: 'gold',
  success: 'green',
  warning: 'orange',
  danger: 'red',
};

export interface StatusTagProps {
  tone: StatusTone;
  label?: ReactNode;
  /** Leading coloured dot for a denser, dashboard-style chip. */
  dot?: boolean;
  icon?: ReactNode;
  style?: CSSProperties;
}

/**
 * Semantic status chip. Replaces raw `<Tag>{rawEnum}</Tag>` usage so status
 * colour is consistent across every list/detail page and follows the design
 * token palette (light + dark).
 */
export function StatusTag({ tone, label, dot, icon, style }: StatusTagProps) {
  return (
    <Tag color={TONE_COLOR[tone]} style={{ borderRadius: 4, ...style }}>
      {dot ? (
        <span
          style={{
            display: 'inline-block',
            width: 6,
            height: 6,
            borderRadius: '50%',
            background: 'currentColor',
            marginRight: 6,
            verticalAlign: 'middle',
          }}
        />
      ) : null}
      {icon}
      {label}
    </Tag>
  );
}

export interface TestCaseStatusMeta {
  tone: StatusTone;
  label: string;
}

const TEST_CASE_STATUS_META: Record<TestCaseStatus, TestCaseStatusMeta> = {
  DRAFT: { tone: 'neutral', label: '草稿' },
  REVIEW: { tone: 'review', label: '审核中' },
  PUBLISHED: { tone: 'success', label: '已发布' },
  DEPRECATED: { tone: 'danger', label: '已弃用' },
};

/**
 * Resolve a test-case status to tone + label. A REVIEW version whose latest
 * review record is REJECT renders as "已驳回" (danger) — there is no REJECTED
 * enum value per the Phase 7 lifecycle contract.
 */
export function testCaseStatusMeta(
  status: TestCaseStatus,
  rejected = false,
): TestCaseStatusMeta {
  if (status === 'REVIEW' && rejected) {
    return { tone: 'danger', label: '已驳回' };
  }
  return TEST_CASE_STATUS_META[status];
}

/** Minimal antd token subset needed to resolve a tone to a raw colour. */
interface ToneTokenShape {
  colorSuccess: string;
  colorWarning: string;
  colorError: string;
  colorInfo: string;
  colorTextQuaternary: string;
}

/**
 * Resolve a StatusTone to a raw colour from the active antd token, so custom
 * visuals (distribution bars, dots, charts) match Tag colours in light + dark.
 */
export function toneTokenColor(token: ToneTokenShape, tone: StatusTone): string {
  switch (tone) {
    case 'success':
      return token.colorSuccess;
    case 'review':
    case 'warning':
      return token.colorWarning;
    case 'danger':
      return token.colorError;
    case 'info':
      return token.colorInfo;
    default:
      return token.colorTextQuaternary;
  }
}
