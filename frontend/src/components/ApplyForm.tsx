'use client';

import { FormEvent, useState } from 'react';
import { applyToJob } from '@/lib/api';

export default function ApplyForm({ jobId }: { jobId: number }) {
  const [coverLetter, setCoverLetter] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await applyToJob(jobId, coverLetter);
      setDone(true);
    } catch (err) {
      // e.g. 409 "You have already applied to this job"
      setError(err instanceof Error ? err.message : 'Could not submit application');
    } finally {
      setSubmitting(false);
    }
  };

  if (done) {
    return <div className="alert-success">Application submitted! Track it under My applications.</div>;
  }

  return (
    <form onSubmit={handleSubmit} className="card space-y-3">
      <h2 className="text-lg font-semibold">Apply for this job</h2>
      {error && (
        <div role="alert" className="alert-error">
          {error}
        </div>
      )}
      <div>
        <label htmlFor="coverLetter" className="label">
          Cover letter (optional)
        </label>
        <textarea
          id="coverLetter"
          rows={5}
          className="input"
          value={coverLetter}
          onChange={(e) => setCoverLetter(e.target.value)}
        />
      </div>
      <button type="submit" disabled={submitting} className="btn-primary">
        {submitting ? 'Submitting...' : 'Submit application'}
      </button>
    </form>
  );
}
