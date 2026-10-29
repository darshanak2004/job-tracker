'use client';

import { FormEvent, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';

export default function LoginForm() {
  const { login } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const next: { email?: string; password?: string } = {};
    if (!email.trim()) {
      next.email = 'Email is required';
    }
    if (!password) {
      next.password = 'Password is required';
    }
    setErrors(next);
    setFormError('');
    if (Object.keys(next).length > 0) {
      return;
    }

    setSubmitting(true);
    try {
      await login(email, password);
      router.push('/');
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate className="card mx-auto max-w-md space-y-4">
      <h1 className="text-2xl font-bold">Welcome back</h1>
      {formError && (
        <div role="alert" className="alert-error">
          {formError}
        </div>
      )}
      <div>
        <label htmlFor="email" className="label">
          Email
        </label>
        <input id="email" type="email" className="input" value={email} onChange={(e) => setEmail(e.target.value)} />
        {errors.email && <p className="field-error">{errors.email}</p>}
      </div>
      <div>
        <label htmlFor="password" className="label">
          Password
        </label>
        <input
          id="password"
          type="password"
          className="input"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {errors.password && <p className="field-error">{errors.password}</p>}
      </div>
      <button type="submit" disabled={submitting} className="btn-primary w-full">
        {submitting ? 'Logging in...' : 'Log in'}
      </button>
      <p className="text-center text-sm text-slate-500">
        No account?{' '}
        <Link href="/register" className="text-brand-600 hover:underline">
          Sign up
        </Link>
      </p>
    </form>
  );
}
