import { render, screen } from '@testing-library/react';
import JobCard from '@/components/JobCard';
import type { Job } from '@/lib/types';

const job: Job = {
  id: 7,
  title: 'Backend Engineer',
  company: 'Acme',
  location: 'Chennai',
  description: 'Build APIs with Spring Boot',
  salaryRange: '12-18 LPA',
  status: 'OPEN',
  recruiterId: 1,
  recruiterName: 'Rita',
  createdAt: '2026-01-01T00:00:00Z',
};

describe('JobCard', () => {
  it('shows the key job details', () => {
    render(<JobCard job={job} />);
    expect(screen.getByRole('heading', { name: 'Backend Engineer' })).toBeInTheDocument();
    expect(screen.getByText(/Acme/)).toHaveTextContent('Chennai');
    expect(screen.getByText('12-18 LPA')).toBeInTheDocument();
    expect(screen.getByText('Open')).toBeInTheDocument();
  });

  it('links to the job detail page', () => {
    render(<JobCard job={job} />);
    expect(screen.getByRole('link', { name: 'Backend Engineer' })).toHaveAttribute('href', '/jobs/7');
  });

  it('omits the salary line when there is none', () => {
    render(<JobCard job={{ ...job, salaryRange: null }} />);
    expect(screen.queryByText('12-18 LPA')).not.toBeInTheDocument();
  });
});
