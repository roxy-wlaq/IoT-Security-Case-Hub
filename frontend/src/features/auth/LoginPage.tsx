import type { ReactNode } from 'react';
import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Card } from 'antd';
import { LoginForm } from '@/features/auth/components/LoginForm';
import { ChangePasswordForm } from '@/features/auth/ChangePasswordForm';
import type { CurrentUser } from '@/shared/types/auth';

interface FromState {
  from?: { pathname: string };
}

function AuthShell({ children }: { children: ReactNode }) {
  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#f0f2f5',
        padding: 16,
      }}
    >
      <Card style={{ width: 380 }} title="IoT Security Case Hub">
        {children}
      </Card>
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
