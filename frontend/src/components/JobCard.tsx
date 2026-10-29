import Link from 'next/link';
import type { Job } from '@/lib/types';
import StatusBadge from './StatusBadge';

export default function JobCard({ job }: { job: Job }) {
  return (
    <article className="card flex flex-col gap-2 transition hover:shadow-md">
      <div className="flex items-start justify-between gap-2">
        <h3 className="text-lg font-semibold text-slate-900">
          <Link href={`/jobs/${job.id}`} className="hover:text-brand-600">
            {job.title}
          </Link>
        </h3>
        <StatusBadge status={job.status} />
      </div>
      <p className="text-sm text-slate-600">
        {job.company} · {job.location}
      </p>
      {job.salaryRange && <p className="text-sm font-medium text-slate-800">{job.salaryRange}</p>}
      <p className="line-clamp-2 text-sm text-slate-500">{job.description}</p>
    </article>
  );
}
