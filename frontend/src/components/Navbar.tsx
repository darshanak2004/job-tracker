'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';

export default function Navbar() {
  const { user, logout } = useAuth();
  const router = useRouter();

  const handleLogout = () => {
    logout();
    router.push('/login');
  };

  return (
    <header className="border-b border-slate-200 bg-white">
      <nav className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
        <Link href="/" className="text-lg font-bold text-brand-600">
          JobTracker
        </Link>
        <div className="flex items-center gap-4 text-sm">
          {user ? (
            <>
              <Link href="/jobs" className="hover:text-brand-600">
                Browse jobs
              </Link>
              {user.role === 'CANDIDATE' && (
                <Link href="/applications" className="hover:text-brand-600">
                  My applications
                </Link>
              )}
              {user.role === 'RECRUITER' && (
                <Link href="/recruiter/jobs" className="hover:text-brand-600">
                  My postings
                </Link>
              )}
              <span className="hidden text-slate-500 sm:inline">
                {user.name} ({user.role.toLowerCase()})
              </span>
              <button type="button" onClick={handleLogout} className="btn-secondary">
                Log out
              </button>
            </>
          ) : (
            <>
              <Link href="/login" className="hover:text-brand-600">
                Log in
              </Link>
              <Link href="/register" className="btn-primary">
                Sign up
              </Link>
            </>
          )}
        </div>
      </nav>
    </header>
  );
}
