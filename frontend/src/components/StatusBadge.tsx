import type { ApplicationStatus, JobStatus } from '@/lib/types';

const STYLES: Record<ApplicationStatus | JobStatus, { label: string; classes: string }> = {
  APPLIED: { label: 'Applied', classes: 'bg-slate-100 text-slate-700' },
  UNDER_REVIEW: { label: 'Under review', classes: 'bg-amber-100 text-amber-800' },
  INTERVIEW: { label: 'Interview', classes: 'bg-sky-100 text-sky-800' },
  OFFERED: { label: 'Offered', classes: 'bg-emerald-100 text-emerald-800' },
  REJECTED: { label: 'Rejected', classes: 'bg-red-100 text-red-700' },
  OPEN: { label: 'Open', classes: 'bg-emerald-100 text-emerald-800' },
  CLOSED: { label: 'Closed', classes: 'bg-slate-200 text-slate-600' },
};

export default function StatusBadge({ status }: { status: ApplicationStatus | JobStatus }) {
  const { label, classes } = STYLES[status];
  return (
    <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${classes}`}>{label}</span>
  );
}
