'use client';

import { FormEvent, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ApiError } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import type { Role } from '@/lib/types';

export default function RegisterForm() {
  const { register } = useAuth();
  const router = useRouter();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<Role>('CANDIDATE');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const next: Record<string, string> = {};
    if (!name.trim()) {
      next.name = 'Name is required';
    }
    if (!/^\S+@\S+\.\S+$/.test(email)) {
      next.email = 'Enter a valid email';
    }
    if (password.length < 8) {
      next.password = 'Password must be at least 8 characters';
    }
    setErrors(next);
    setFormError('');
    if (Object.keys(next).length > 0) {
      return;
    }

    setSubmitting(true);
    try {
      await register(name, email, password, role);
      router.push('/');
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors) {
        setErrors(err.fieldErrors);
      }
      setFormError(err instanceof Error ? err.message : 'Registration failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate className="card mx-auto max-w-md space-y-4">
      <h1 className="text-2xl font-bold">Create your account</h1>
      {formError && (
        <div role="alert" className="alert-error">
          {formError}
        </div>
      )}
      <div>
        <label htmlFor="name" className="label">
          Full name
        </label>
        <input id="name" className="input" value={name} onChange={(e) => setName(e.target.value)} />
        {errors.name && <p className="field-error">{errors.name}</p>}
      </div>
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
      <div>
        <label htmlFor="role" className="label">
          I am a
        </label>
        <select id="role" className="input" value={role} onChange={(e) => setRole(e.target.value as Role)}>
          <option value="CANDIDATE">Candidate (looking for jobs)</option>
          <option value="RECRUITER">Recruiter (posting jobs)</option>
        </select>
      </div>
      <button type="submit" disabled={submitting} className="btn-primary w-full">
        {submitting ? 'Creating account...' : 'Sign up'}
      </button>
      <p className="text-center text-sm text-slate-500">
        Already registered?{' '}
        <Link href="/login" className="text-brand-600 hover:underline">
          Log in
        </Link>
      </p>
    </form>
  );
}
