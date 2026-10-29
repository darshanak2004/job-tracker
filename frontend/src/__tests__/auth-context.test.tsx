import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '@/lib/api';
import { AuthProvider, useAuth } from '@/lib/auth-context';

jest.mock('@/lib/api', () => ({
  getToken: jest.fn(),
  setToken: jest.fn(),
  clearToken: jest.fn(),
  me: jest.fn(),
  login: jest.fn(),
  register: jest.fn(),
}));

const mocked = api as jest.Mocked<typeof api>;
const ann = { id: 1, name: 'Ann', email: 'ann@test.com', role: 'CANDIDATE' as const };

function Probe() {
  const { user, loading, login, logout } = useAuth();
  return (
    <div>
      <span data-testid="state">{loading ? 'loading' : (user?.name ?? 'anonymous')}</span>
      <button onClick={() => login('ann@test.com', 'pw')}>do-login</button>
      <button onClick={logout}>do-logout</button>
    </div>
  );
}

const renderProbe = () =>
  render(
    <AuthProvider>
      <Probe />
    </AuthProvider>,
  );

describe('AuthProvider', () => {
  beforeEach(() => jest.resetAllMocks());

  it('starts anonymous when there is no stored token', async () => {
    mocked.getToken.mockReturnValue(null);
    renderProbe();
    expect(await screen.findByText('anonymous')).toBeInTheDocument();
    expect(mocked.me).not.toHaveBeenCalled();
  });

  it('restores the session from a stored token', async () => {
    mocked.getToken.mockReturnValue('stored.jwt');
    mocked.me.mockResolvedValue(ann);
    renderProbe();
    expect(await screen.findByText('Ann')).toBeInTheDocument();
  });

  it('clears an invalid stored token', async () => {
    mocked.getToken.mockReturnValue('expired.jwt');
    mocked.me.mockRejectedValue(new Error('401'));
    renderProbe();
    expect(await screen.findByText('anonymous')).toBeInTheDocument();
    expect(mocked.clearToken).toHaveBeenCalled();
  });

  it('login stores the token and sets the user; logout clears both', async () => {
    mocked.getToken.mockReturnValue(null);
    mocked.login.mockResolvedValue({ token: 'new.jwt', user: ann });
    const user = userEvent.setup();
    renderProbe();
    await screen.findByText('anonymous');

    await user.click(screen.getByText('do-login'));
    expect(await screen.findByText('Ann')).toBeInTheDocument();
    expect(mocked.setToken).toHaveBeenCalledWith('new.jwt');

    await user.click(screen.getByText('do-logout'));
    expect(await screen.findByText('anonymous')).toBeInTheDocument();
    expect(mocked.clearToken).toHaveBeenCalled();
  });
});
