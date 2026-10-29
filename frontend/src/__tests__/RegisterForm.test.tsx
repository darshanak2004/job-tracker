import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RegisterForm from '@/components/RegisterForm';
import { ApiError } from '@/lib/api';

const mockPush = jest.fn();
const mockRegister = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush, replace: jest.fn() }),
}));
jest.mock('@/lib/auth-context', () => ({
  useAuth: () => ({ register: mockRegister }),
}));

async function fillValid(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Full name'), 'Rita Recruiter');
  await user.type(screen.getByLabelText('Email'), 'rita@test.com');
  await user.type(screen.getByLabelText('Password'), 'password123');
}

describe('RegisterForm', () => {
  beforeEach(() => {
    mockPush.mockReset();
    mockRegister.mockReset();
  });

  it('validates name, email and password length', async () => {
    const user = userEvent.setup();
    render(<RegisterForm />);

    await user.type(screen.getByLabelText('Email'), 'not-an-email');
    await user.type(screen.getByLabelText('Password'), 'short');
    await user.click(screen.getByRole('button', { name: 'Sign up' }));

    expect(screen.getByText('Name is required')).toBeInTheDocument();
    expect(screen.getByText('Enter a valid email')).toBeInTheDocument();
    expect(screen.getByText('Password must be at least 8 characters')).toBeInTheDocument();
    expect(mockRegister).not.toHaveBeenCalled();
  });

  it('registers with the selected role and redirects', async () => {
    mockRegister.mockResolvedValue({ id: 1 });
    const user = userEvent.setup();
    render(<RegisterForm />);

    await fillValid(user);
    await user.selectOptions(screen.getByLabelText('I am a'), 'RECRUITER');
    await user.click(screen.getByRole('button', { name: 'Sign up' }));

    expect(mockRegister).toHaveBeenCalledWith('Rita Recruiter', 'rita@test.com', 'password123', 'RECRUITER');
    expect(mockPush).toHaveBeenCalledWith('/');
  });

  it('defaults to the candidate role', async () => {
    mockRegister.mockResolvedValue({ id: 1 });
    const user = userEvent.setup();
    render(<RegisterForm />);

    await fillValid(user);
    await user.click(screen.getByRole('button', { name: 'Sign up' }));

    expect(mockRegister).toHaveBeenCalledWith('Rita Recruiter', 'rita@test.com', 'password123', 'CANDIDATE');
  });

  it('shows the backend error (e.g. email already registered)', async () => {
    mockRegister.mockRejectedValue(new ApiError(409, 'Email is already registered'));
    const user = userEvent.setup();
    render(<RegisterForm />);

    await fillValid(user);
    await user.click(screen.getByRole('button', { name: 'Sign up' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Email is already registered');
    expect(mockPush).not.toHaveBeenCalled();
  });
});
