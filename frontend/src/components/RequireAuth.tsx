'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import type { Role } from '@/lib/types';

/** Wrap a page with this to require login (and optionally a specific role). */
export default function RequireAuth({ role, children }: { role?: Role; children: React.ReactNode }) {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user) {
      router.replace('/login');
    }
  }, [loading, user, router]);

  if (loading || !user) {
    return <p className="py-10 text-center text-slate-500">Loading...</p>;
  }
  if (role && user.role !== role) {
    return (
      <div role="alert" className="alert-error">
        You do not have access to this page.
      </div>
    );
  }
  return <>{children}</>;
}
