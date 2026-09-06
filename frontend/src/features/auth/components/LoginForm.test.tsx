import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { LoginForm } from './LoginForm';
import { useLogin } from '@/features/auth/hooks/useLogin';

vi.mock('@/features/auth/hooks/useLogin', () => ({
  useLogin: vi.fn(),
}));

describe('LoginForm', () => {
  const mutateAsync = vi.fn();
  const reset = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    });
    mutateAsync.mockResolvedValue({
      id: 'admin-id',
      username: 'admin',
      displayName: 'System Administrator',
      enabled: true,
      mustChangePassword: false,
      roles: ['ADMIN'],
      permissions: [],
    });
    vi.mocked(useLogin).mockReturnValue({
      error: null,
      isPending: false,
      mutateAsync,
      reset,
    } as unknown as ReturnType<typeof useLogin>);
  });

  it('submits values supplied by browser autofill', async () => {
    render(<LoginForm />);

    const username = screen.getByPlaceholderText('请输入用户名');
    const password = screen.getByPlaceholderText('请输入密码');
    const form = screen.getByRole('button', { name: /登.*录/ }).closest('form');

    expect(form).not.toBeNull();

    const setNativeValue = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set;
    setNativeValue?.call(username, 'admin');
    setNativeValue?.call(password, '18031');

    fireEvent.submit(form as HTMLFormElement);

    await waitFor(() => {
      expect(mutateAsync).toHaveBeenCalledWith({ username: 'admin', password: '18031' });
    });
    expect(screen.queryByText('请输入用户名')).not.toBeInTheDocument();
    expect(screen.queryByText('请输入密码')).not.toBeInTheDocument();
  });
});
