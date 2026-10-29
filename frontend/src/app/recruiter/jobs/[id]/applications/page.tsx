'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import ApplicantCard from '@/components/ApplicantCard';
import RequireAuth from '@/components/RequireAuth';
import { jobApplications, updateApplicationStatus } from '@/lib/api';
import type { Application, ApplicationStatus } from '@/lib/types';

function Applicants() {
  const params = useParams<{ id: string }>();
  const [apps, setApps] = useState<Application[] | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    jobApplications(Number(params.id))
      .then(setApps)
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load applicants'));
  }, [params.id]);

  const changeStatus = async (id: number, status: ApplicationStatus) => {
    setError('');
    try {
      const updated = await updateApplicationStatus(id, status);
      setApps((list) => (list ?? []).map((a) => (a.id === id ? updated : a)));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not update status');
    }
  };

  return (
    <div className="space-y-4">
      <Link href="/recruiter/jobs" className="text-sm text-brand-600 hover:underline">
        Back to my postings
      </Link>
      <h1 className="text-2xl font-bold">Applicants</h1>
      {error && (
        <div role="alert" className="alert-error">
          {error}
        </div>
      )}
      {apps === null && !error && <p className="text-slate-500">Loading...</p>}
      {apps?.length === 0 && <p className="text-slate-500">No applications yet.</p>}
      {apps?.map((a) => (
        <ApplicantCard key={a.id} application={a} onStatusChange={changeStatus} />
      ))}
    </div>
  );
}

export default function ApplicantsPage() {
  return (
    <RequireAuth role="RECRUITER">
      <Applicants />
    </RequireAuth>
  );
}
