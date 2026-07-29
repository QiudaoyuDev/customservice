import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import Login from './Login';

const login = vi.fn();
const navigate = vi.fn();

vi.mock('../lib/auth', () => ({ useAuth: () => ({ login }) }));
vi.mock('react-router-dom', () => ({ useNavigate: () => navigate }));

describe('Login', () => {
  afterEach(() => {
    login.mockReset();
    navigate.mockReset();
  });

  it('submits typed credentials and navigates only after authentication succeeds', async () => {
    login.mockResolvedValue(undefined);
    render(<Login />);

    fireEvent.change(screen.getByPlaceholderText('you@example.com'), {
      target: { value: 'operator@example.test' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), { target: { value: 'a-password' } });
    fireEvent.click(screen.getByRole('button'));

    await waitFor(() => expect(login).toHaveBeenCalledWith('operator@example.test', 'a-password'));
    expect(navigate).toHaveBeenCalledWith('/console');
  });

  it('does not navigate when authentication is rejected', async () => {
    login.mockRejectedValue(new Error('invalid credentials'));
    render(<Login />);

    fireEvent.click(screen.getByRole('button'));

    expect(await screen.findByText('登录失败，请检查邮箱和密码')).toBeInTheDocument();
    expect(navigate).not.toHaveBeenCalled();
  });
});
