'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import JobForm from '@/components/JobForm';
import RequireAuth from '@/components/RequireAuth';
import { getJob, updateJob } from '@/lib/api';
import type { Job } from '@/lib/types';

function EditJob() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
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
  return (
    <>
      <h1 className="mb-4 text-2xl font-bold">Edit posting</h1>
      <JobForm
        initial={job}
        submitLabel="Save changes"
        onSubmit={async (input) => {
          await updateJob(job.id, input);
          router.push('/recruiter/jobs');
        }}
      />
    </>
  );
}

export default function EditJobPage() {
  return (
    <RequireAuth role="RECRUITER">
      <EditJob />
    </RequireAuth>
  );
}
