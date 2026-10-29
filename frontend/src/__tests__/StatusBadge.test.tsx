import { render, screen } from '@testing-library/react';
import StatusBadge from '@/components/StatusBadge';

describe('StatusBadge', () => {
  it.each([
    ['APPLIED', 'Applied'],
    ['UNDER_REVIEW', 'Under review'],
    ['INTERVIEW', 'Interview'],
    ['OFFERED', 'Offered'],
    ['REJECTED', 'Rejected'],
    ['OPEN', 'Open'],
    ['CLOSED', 'Closed'],
  ] as const)('renders a human label for %s', (status, label) => {
    render(<StatusBadge status={status} />);
    expect(screen.getByText(label)).toBeInTheDocument();
  });

  it('colours rejected differently from offered', () => {
    const { rerender } = render(<StatusBadge status="REJECTED" />);
    const rejectedClass = screen.getByText('Rejected').className;
    rerender(<StatusBadge status="OFFERED" />);
    expect(screen.getByText('Offered').className).not.toEqual(rejectedClass);
  });
});
