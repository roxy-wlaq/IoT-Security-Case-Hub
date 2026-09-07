import type { ReactNode } from 'react';
import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Button, Card, Typography, theme } from 'antd';
import { MoonOutlined, SunOutlined } from '@ant-design/icons';
import { LoginForm } from '@/features/auth/components/LoginForm';
import { ChangePasswordForm } from '@/features/auth/ChangePasswordForm';
import { BrandLogo } from '@/shared/components/BrandLogo';
import { useThemeMode } from '@/shared/contexts/ThemeModeContext';
import type { CurrentUser } from '@/shared/types/auth';

interface FromState {
  from?: { pathname: string };
}

function AuthShell({ children }: { children: ReactNode }) {
  const { token } = theme.useToken();
  const { mode, toggle } = useThemeMode();

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: token.paddingLG,
        background: 'var(--color-bg-page)',
        padding: 24,
        position: 'relative',
      }}
    >
      <Button
        type="text"
        aria-label="切换主题"
        icon={mode === 'dark' ? <SunOutlined /> : <MoonOutlined />}
        onClick={toggle}
        style={{ position: 'absolute', top: 16, right: 16 }}
      />
      <Card
        bordered={false}
        style={{ width: 400, maxWidth: '100%', boxShadow: token.boxShadowSecondary }}
        styles={{ body: { padding: token.paddingLG } }}
      >
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            textAlign: 'center',
            gap: 4,
            marginBottom: token.paddingLG,
          }}
        >
          <BrandLogo size={44} />
          <Typography.Text type="secondary" style={{ fontSize: token.fontSizeSM }}>
            IoT 安全测试用例管理平台
          </Typography.Text>
        </div>
        {children}
      </Card>
      <Typography.Text type="secondary" style={{ fontSize: token.fontSizeSM }}>
        © IoT Case Hub · 企业级 IoT 安全测试中枢
      </Typography.Text>
    </div>
  );
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [pendingUser, setPendingUser] = useState<CurrentUser | null>(null);

  const from = (location.state as FromState | null)?.from?.pathname ?? '/';

  // 登录成功但被要求强制改密：先停留在登录卡片内完成改密。
  if (pendingUser?.mustChangePassword) {
    return (
      <AuthShell>
        <ChangePasswordForm
          user={pendingUser}
          onSuccess={() => navigate('/', { replace: true })}
        />
      </AuthShell>
    );
  }

  return (
    <AuthShell>
      <LoginForm
        onSuccess={(user) => {
          if (user.mustChangePassword) {
            setPendingUser(user);
          } else {
            navigate(from, { replace: true });
          }
        }}
      />
    </AuthShell>
  );
}

export default LoginPage;
