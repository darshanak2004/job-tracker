'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import ApplyForm from '@/components/ApplyForm';
import RequireAuth from '@/components/RequireAuth';
import StatusBadge from '@/components/StatusBadge';
import { getJob } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import type { Job } from '@/lib/types';

function JobDetail() {
  const params = useParams<{ id: string }>();
  const { user } = useAuth();
  const [job, setJob] = useState<Job | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    getJob(Number(params.id))
      .then(setJob)
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load job'));
  }, [params.id]);

  if (error) {
    return (
      <div role="alert" className="alert-error">
        {error}
      </div>
    );
  }
  if (!job) {
    return <p className="text-slate-500">Loading...</p>;
  }

  const isOwner = user?.role === 'RECRUITER' && user.id === job.recruiterId;

  return (
    <div className="space-y-6">
      <div className="card space-y-3">
        <div className="flex items-start justify-between gap-2">
          <h1 className="text-2xl font-bold">{job.title}</h1>
          <StatusBadge status={job.status} />
        </div>
        <p className="text-slate-600">
          {job.company} · {job.location}
        </p>
        {job.salaryRange && <p className="font-medium">{job.salaryRange}</p>}
        <p className="whitespace-pre-line text-slate-700">{job.description}</p>
        <p className="text-xs text-slate-400">Posted by {job.recruiterName}</p>
      </div>

      {user?.role === 'CANDIDATE' &&
        (job.status === 'OPEN' ? (
          <ApplyForm jobId={job.id} />
        ) : (
          <div className="alert-error">This job is closed and no longer accepts applications.</div>
        ))}

      {isOwner && (
        <div className="flex gap-3">
          <Link href={`/recruiter/jobs/${job.id}/applications`} className="btn-primary">
            View applicants
          </Link>
          <Link href={`/recruiter/jobs/${job.id}/edit`} className="btn-secondary">
            Edit
          </Link>
        </div>
      )}
    </div>
  );
}

export default function JobDetailPage() {
  return (
    <RequireAuth>
      <JobDetail />
    </RequireAuth>
  );
}
