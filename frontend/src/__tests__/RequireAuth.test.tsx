import { render, screen } from '@testing-library/react';
import RequireAuth from '@/components/RequireAuth';

const mockReplace = jest.fn();
const mockRouter = { replace: mockReplace };
let mockAuth: { user: { role: string } | null; loading: boolean };

jest.mock('next/navigation', () => ({
  useRouter: () => mockRouter,
}));
jest.mock('@/lib/auth-context', () => ({
  useAuth: () => mockAuth,
}));

describe('RequireAuth', () => {
  beforeEach(() => mockReplace.mockReset());

  it('redirects anonymous visitors to /login', () => {
    mockAuth = { user: null, loading: false };
    render(
      <RequireAuth>
        <p>secret</p>
      </RequireAuth>,
    );
    expect(mockReplace).toHaveBeenCalledWith('/login');
    expect(screen.queryByText('secret')).not.toBeInTheDocument();
  });

  it('does not redirect while the session is still loading', () => {
    mockAuth = { user: null, loading: true };
    render(
      <RequireAuth>
        <p>secret</p>
      </RequireAuth>,
    );
    expect(mockReplace).not.toHaveBeenCalled();
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('renders children for a logged-in user', () => {
    mockAuth = { user: { role: 'CANDIDATE' }, loading: false };
    render(
      <RequireAuth>
        <p>secret</p>
      </RequireAuth>,
    );
    expect(screen.getByText('secret')).toBeInTheDocument();
  });

  it('blocks the wrong role', () => {
    mockAuth = { user: { role: 'CANDIDATE' }, loading: false };
    render(
      <RequireAuth role="RECRUITER">
        <p>recruiters only</p>
      </RequireAuth>,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('You do not have access');
    expect(screen.queryByText('recruiters only')).not.toBeInTheDocument();
  });

  it('allows the right role', () => {
    mockAuth = { user: { role: 'RECRUITER' }, loading: false };
    render(
      <RequireAuth role="RECRUITER">
        <p>recruiters only</p>
      </RequireAuth>,
    );
    expect(screen.getByText('recruiters only')).toBeInTheDocument();
  });
});
