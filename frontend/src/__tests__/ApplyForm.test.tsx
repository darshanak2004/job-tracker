import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ApplyForm from '@/components/ApplyForm';

const mockApply = jest.fn();
jest.mock('@/lib/api', () => ({
  applyToJob: (...args: unknown[]) => mockApply(...args),
}));

describe('ApplyForm', () => {
  beforeEach(() => mockApply.mockReset());

  it('submits the cover letter for the given job and shows success', async () => {
    mockApply.mockResolvedValue({ id: 1 });
    const user = userEvent.setup();
    render(<ApplyForm jobId={42} />);

    await user.type(screen.getByLabelText(/Cover letter/), 'Please hire me');
    await user.click(screen.getByRole('button', { name: 'Submit application' }));

    expect(mockApply).toHaveBeenCalledWith(42, 'Please hire me');
    expect(await screen.findByText(/Application submitted/)).toBeInTheDocument();
  });

  it('shows the duplicate-application error from the API', async () => {
    mockApply.mockRejectedValue(new Error('You have already applied to this job'));
    const user = userEvent.setup();
    render(<ApplyForm jobId={42} />);

    await user.click(screen.getByRole('button', { name: 'Submit application' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('You have already applied to this job');
    expect(screen.queryByText(/Application submitted/)).not.toBeInTheDocument();
  });
});
