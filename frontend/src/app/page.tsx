'use client';

import { useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';

export default function HomePage() {
  const { user, loading } = useAuth();
  const router = useRouter();

  // Logged-in users go straight to their home screen
  useEffect(() => {
    if (!loading && user) {
      router.replace(user.role === 'RECRUITER' ? '/recruiter/jobs' : '/jobs');
    }
  }, [loading, user, router]);

  if (loading || user) {
    return <p className="py-10 text-center text-slate-500">Loading...</p>;
  }

  return (
    <section className="py-16 text-center">
      <h1 className="text-4xl font-extrabold tracking-tight text-slate-900">Hire faster. Get hired sooner.</h1>
      <p className="mx-auto mt-4 max-w-xl text-lg text-slate-600">
        Recruiters post openings and move applicants through the pipeline. Candidates apply once and watch the
        status of every application in one place.
      </p>
      <div className="mt-8 flex justify-center gap-3">
        <Link href="/register" className="btn-primary">
          Get started
        </Link>
        <Link href="/login" className="btn-secondary">
          Log in
        </Link>
      </div>
    </section>
  );
}
