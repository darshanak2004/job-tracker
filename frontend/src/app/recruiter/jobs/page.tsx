'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import RequireAuth from '@/components/RequireAuth';
import StatusBadge from '@/components/StatusBadge';
import { deleteJob, myJobs } from '@/lib/api';
import type { Job } from '@/lib/types';

function MyPostings() {
  const [jobs, setJobs] = useState<Job[] | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    myJobs()
      .then(setJobs)
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load postings'));
  }, []);

  const remove = async (id: number) => {
    if (!window.confirm('Delete this posting and all its applications?')) {
      return;
    }
    try {
      await deleteJob(id);
      setJobs((list) => (list ?? []).filter((j) => j.id !== id));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not delete');
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">My job postings</h1>
        <Link href="/recruiter/jobs/new" className="btn-primary">
          New posting
        </Link>
      </div>
      {error && (
        <div role="alert" className="alert-error">
          {error}
        </div>
      )}
      {jobs === null && !error && <p className="text-slate-500">Loading...</p>}
      {jobs?.length === 0 && <p className="text-slate-500">You have not posted any jobs yet.</p>}
      {jobs?.map((job) => (
        <article key={job.id} className="card flex flex-wrap items-center justify-between gap-3">
          <div>
            <Link href={`/jobs/${job.id}`} className="font-semibold hover:text-brand-600">
              {job.title}
            </Link>
            <p className="text-sm text-slate-500">
              {job.company} · {job.location}
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge status={job.status} />
            <Link href={`/recruiter/jobs/${job.id}/applications`} className="btn-secondary">
              Applicants
            </Link>
            <Link href={`/recruiter/jobs/${job.id}/edit`} className="btn-secondary">
              Edit
            </Link>
            <button className="btn-danger" onClick={() => remove(job.id)}>
              Delete
            </button>
          </div>
        </article>
      ))}
    </div>
  );
}

export default function RecruiterJobsPage() {
  return (
    <RequireAuth role="RECRUITER">
      <MyPostings />
    </RequireAuth>
  );
}
