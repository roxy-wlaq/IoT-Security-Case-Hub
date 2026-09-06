import { Breadcrumb, Card, Space, Typography, theme } from 'antd';
import type { CSSProperties, ReactNode } from 'react';
import { Link } from 'react-router-dom';

export interface BreadcrumbItem {
  label: ReactNode;
  to?: string;
}

export interface PageContainerProps {
  title?: ReactNode;
  subtitle?: ReactNode;
  breadcrumbs?: BreadcrumbItem[];
  /** Right-aligned actions (primary button, etc.). */
  extra?: ReactNode;
  /** Render content directly without a card wrapper. */
  noCard?: boolean;
  style?: CSSProperties;
  children?: ReactNode;
}

/**
 * Standard page shell: breadcrumb row + header (title/subtitle left, actions
 * right) + content. Replaces per-page `<Typography.Title>` + bare `<div>`
 * boilerplate so every page shares one rhythm and is dark-mode aware via
 * `theme.useToken()`.
 */
export function PageContainer({
  title,
  subtitle,
  breadcrumbs,
  extra,
  noCard,
  style,
  children,
}: PageContainerProps) {
  const { token } = theme.useToken();
  const items = breadcrumbs?.map((b) =>
    b.to ? { title: <Link to={b.to}>{b.label}</Link> } : { title: b.label },
  );

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        gap: token.paddingLG,
        ...style,
      }}
    >
      {items?.length ? <Breadcrumb items={items} /> : null}
      {title || extra ? (
        <div
          style={{
            display: 'flex',
            alignItems: 'flex-start',
            justifyContent: 'space-between',
            gap: token.padding,
            flexWrap: 'wrap',
          }}
        >
          <div>
            {title ? (
              <Typography.Title level={3} style={{ margin: 0 }}>
                {title}
              </Typography.Title>
            ) : null}
            {subtitle ? (
              <Typography.Text
                type="secondary"
                style={{ marginTop: token.paddingXS, display: 'block' }}
              >
                {subtitle}
              </Typography.Text>
            ) : null}
          </div>
          {extra ? <Space wrap>{extra}</Space> : null}
        </div>
      ) : null}
      {noCard ? (
        children
      ) : (
        <Card bordered={false} style={{ boxShadow: token.boxShadowSecondary }}>
          {children}
        </Card>
      )}
    </div>
  );
}
