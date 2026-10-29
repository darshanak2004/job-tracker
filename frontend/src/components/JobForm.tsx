'use client';

import { FormEvent, useState } from 'react';
import { ApiError } from '@/lib/api';
import type { Job, JobInput, JobStatus } from '@/lib/types';

interface Props {
  initial?: Job;
  submitLabel: string;
  onSubmit: (input: JobInput) => Promise<void>;
}

const REQUIRED: { key: 'title' | 'company' | 'location' | 'description'; label: string }[] = [
  { key: 'title', label: 'Title' },
  { key: 'company', label: 'Company' },
  { key: 'location', label: 'Location' },
  { key: 'description', label: 'Description' },
];

interface FormValues {
  title: string;
  company: string;
  location: string;
  description: string;
  salaryRange: string;
  status: string;
}

export default function JobForm({ initial, submitLabel, onSubmit }: Props) {
  const [values, setValues] = useState<FormValues>({
    title: initial?.title ?? '',
    company: initial?.company ?? '',
    location: initial?.location ?? '',
    description: initial?.description ?? '',
    salaryRange: initial?.salaryRange ?? '',
    status: initial?.status ?? 'OPEN',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const set = (key: keyof FormValues, value: string) => setValues((v) => ({ ...v, [key]: value }));

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const next: Record<string, string> = {};
    REQUIRED.forEach(({ key, label }) => {
      if (!values[key].trim()) {
        next[key] = `${label} is required`;
      }
    });
    setErrors(next);
    setFormError('');
    if (Object.keys(next).length > 0) {
      return;
    }

    setSaving(true);
    try {
      await onSubmit({
        title: values.title,
        company: values.company,
        location: values.location,
        description: values.description,
        salaryRange: values.salaryRange || undefined,
        status: values.status as JobStatus,
      });
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors) {
        setErrors(err.fieldErrors);
      }
      setFormError(err instanceof Error ? err.message : 'Could not save the job');
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate className="card max-w-2xl space-y-4">
      {formError && (
        <div role="alert" className="alert-error">
          {formError}
        </div>
      )}
      {REQUIRED.filter((f) => f.key !== 'description').map(({ key, label }) => (
        <div key={key}>
          <label htmlFor={key} className="label">
            {label}
          </label>
          <input id={key} className="input" value={values[key]} onChange={(e) => set(key, e.target.value)} />
          {errors[key] && <p className="field-error">{errors[key]}</p>}
        </div>
      ))}
      <div>
        <label htmlFor="salaryRange" className="label">
          Salary range (optional)
        </label>
        <input
          id="salaryRange"
          className="input"
          value={values.salaryRange}
          onChange={(e) => set('salaryRange', e.target.value)}
        />
      </div>
      <div>
        <label htmlFor="description" className="label">
          Description
        </label>
        <textarea
          id="description"
          rows={6}
          className="input"
          value={values.description}
          onChange={(e) => set('description', e.target.value)}
        />
        {errors.description && <p className="field-error">{errors.description}</p>}
      </div>
      <div>
        <label htmlFor="status" className="label">
          Status
        </label>
        <select id="status" className="input" value={values.status} onChange={(e) => set('status', e.target.value)}>
          <option value="OPEN">Open (accepting applications)</option>
          <option value="CLOSED">Closed</option>
        </select>
      </div>
      <button type="submit" disabled={saving} className="btn-primary">
        {saving ? 'Saving...' : submitLabel}
      </button>
    </form>
  );
}
