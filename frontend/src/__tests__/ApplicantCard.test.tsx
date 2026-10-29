import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ApplicantCard from '@/components/ApplicantCard';
import type { Application } from '@/lib/types';

const application: Application = {
  id: 5,
  jobId: 1,
  jobTitle: 'Dev',
  company: 'Acme',
  candidateId: 9,
  candidateName: 'Ann Candidate',
  candidateEmail: 'ann@test.com',
  coverLetter: 'I love Spring',
  status: 'APPLIED',
  appliedAt: '2026-01-01T00:00:00Z',
};

describe('ApplicantCard', () => {
  it('shows the candidate and cover letter', () => {
    render(<ApplicantCard application={application} onStatusChange={jest.fn()} />);

    expect(screen.getByText('Ann Candidate')).toBeInTheDocument();
    expect(screen.getByText('ann@test.com')).toBeInTheDocument();
    expect(screen.getByText('I love Spring')).toBeInTheDocument();
    expect(screen.getByLabelText('Status for Ann Candidate')).toHaveValue('APPLIED');
  });

  it('reports a status change to the parent', async () => {
    const onStatusChange = jest.fn();
    const user = userEvent.setup();
    render(<ApplicantCard application={application} onStatusChange={onStatusChange} />);

    await user.selectOptions(screen.getByLabelText('Status for Ann Candidate'), 'INTERVIEW');

    expect(onStatusChange).toHaveBeenCalledWith(5, 'INTERVIEW');
  });
});
