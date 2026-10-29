'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import RequireAuth from '@/components/RequireAuth';
import StatusBadge from '@/components/StatusBadge';
import { myApplications, withdrawApplication } from '@/lib/api';
import type { Application } from '@/lib/types';

function MyApplications() {
  const [apps, setApps] = useState<Application[] | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    myApplications()
      .then(setApps)
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load applications'));
  }, []);

  const withdraw = async (id: number) => {
    if (!window.confirm('Withdraw this application?')) {
      return;
    }
    try {
      await withdrawApplication(id);
      setApps((list) => (list ?? []).filter((a) => a.id !== id));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not withdraw');
    }
  };

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">My applications</h1>
      {error && (
        <div role="alert" className="alert-error">
          {error}
        </div>
      )}
      {apps === null && !error && <p className="text-slate-500">Loading...</p>}
      {apps?.length === 0 && (
        <p className="text-slate-500">
          You have not applied to anything yet.{' '}
          <Link href="/jobs" className="text-brand-600 hover:underline">
            Browse jobs
          </Link>
        </p>
      )}
      {apps?.map((a) => (
        <article key={a.id} className="card flex flex-wrap items-center justify-between gap-3">
          <div>
            <Link href={`/jobs/${a.jobId}`} className="font-semibold hover:text-brand-600">
              {a.jobTitle}
            </Link>
            <p className="text-sm text-slate-500">
              {a.company} · applied {new Date(a.appliedAt).toLocaleDateString()}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <StatusBadge status={a.status} />
            <button className="btn-danger" onClick={() => withdraw(a.id)}>
              Withdraw
            </button>
          </div>
        </article>
      ))}
    </div>
  );
}

export default function ApplicationsPage() {
  return (
    <RequireAuth role="CANDIDATE">
      <MyApplications />
    </RequireAuth>
  );
}
