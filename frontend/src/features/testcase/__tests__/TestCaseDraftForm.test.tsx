import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TestCaseDraftForm } from '@/features/testcase/components/TestCaseDraftForm';

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: () => ({ matches: false, addListener: vi.fn(), removeListener: vi.fn(), addEventListener: vi.fn(), removeEventListener: vi.fn() }),
});

describe('TestCaseDraftForm', () => {
  it('requires a case name and step content before submit', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<TestCaseDraftForm onSubmit={onSubmit} />);

    await user.click(screen.getByRole('button', { name: '保存 Draft' }));

    expect(await screen.findByText('请输入测试用例名称')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });
});
