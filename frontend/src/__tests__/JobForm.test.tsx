import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import JobForm from '@/components/JobForm';
import { ApiError } from '@/lib/api';
import type { Job } from '@/lib/types';

const existing: Job = {
  id: 3,
  title: 'Old title',
  company: 'Old Co',
  location: 'Pune',
  description: 'Old description',
  salaryRange: '5 LPA',
  status: 'CLOSED',
  recruiterId: 1,
  recruiterName: 'Rita',
  createdAt: '2026-01-01T00:00:00Z',
};

describe('JobForm', () => {
  it('requires title, company, location and description', async () => {
    const onSubmit = jest.fn();
    const user = userEvent.setup();
    render(<JobForm submitLabel="Publish" onSubmit={onSubmit} />);

    await user.click(screen.getByRole('button', { name: 'Publish' }));

    expect(screen.getByText('Title is required')).toBeInTheDocument();
    expect(screen.getByText('Company is required')).toBeInTheDocument();
    expect(screen.getByText('Location is required')).toBeInTheDocument();
    expect(screen.getByText('Description is required')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('submits the typed values', async () => {
    const onSubmit = jest.fn().mockResolvedValue(undefined);
    const user = userEvent.setup();
    render(<JobForm submitLabel="Publish" onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText('Title'), 'Java Dev');
    await user.type(screen.getByLabelText('Company'), 'Acme');
    await user.type(screen.getByLabelText('Location'), 'Remote');
    await user.type(screen.getByLabelText('Description'), 'Build things');
    await user.click(screen.getByRole('button', { name: 'Publish' }));

    expect(onSubmit).toHaveBeenCalledWith({
      title: 'Java Dev',
      company: 'Acme',
      location: 'Remote',
      description: 'Build things',
      salaryRange: undefined,
      status: 'OPEN',
    });
  });

  it('pre-fills the form when editing', () => {
    render(<JobForm initial={existing} submitLabel="Save" onSubmit={jest.fn()} />);

    expect(screen.getByLabelText('Title')).toHaveValue('Old title');
    expect(screen.getByLabelText(/Salary range/)).toHaveValue('5 LPA');
    expect(screen.getByLabelText('Status')).toHaveValue('CLOSED');
  });

  it('shows the error returned by the server', async () => {
    const onSubmit = jest.fn().mockRejectedValue(new ApiError(403, 'You can only modify your own job postings'));
    const user = userEvent.setup();
    render(<JobForm initial={existing} submitLabel="Save" onSubmit={onSubmit} />);

    await user.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('You can only modify your own job postings');
  });
});
