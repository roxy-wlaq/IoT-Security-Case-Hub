import { Skeleton, theme } from 'antd';

export interface TableSkeletonProps {
  rows?: number;
}

/**
 * Skeleton placeholder for table first-load. Replaces the spinner-only
 * LoadingState inside data tables so the page doesn't "jump" on load.
 */
export function TableSkeleton({ rows = 6 }: TableSkeletonProps) {
  const { token } = theme.useToken();
  return (
    <div style={{ padding: `${token.paddingLG}px 0` }}>
      <div
        style={{
          height: 40,
          borderRadius: token.borderRadius,
          background: token.colorFillQuaternary,
          marginBottom: token.padding,
        }}
      />
      <Skeleton active title={false} paragraph={{ rows }} />
    </div>
  );
}

export default TableSkeleton;
