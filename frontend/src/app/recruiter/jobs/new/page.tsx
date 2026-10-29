'use client';

import { useRouter } from 'next/navigation';
import JobForm from '@/components/JobForm';
import RequireAuth from '@/components/RequireAuth';
import { createJob } from '@/lib/api';

export default function NewJobPage() {
  const router = useRouter();
  return (
    <RequireAuth role="RECRUITER">
      <h1 className="mb-4 text-2xl font-bold">New job posting</h1>
      <JobForm
        submitLabel="Publish job"
        onSubmit={async (input) => {
          await createJob(input);
          router.push('/recruiter/jobs');
        }}
      />
    </RequireAuth>
  );
}
