'use client';

import { FormEvent, useCallback, useEffect, useState } from 'react';
import JobCard from '@/components/JobCard';
import RequireAuth from '@/components/RequireAuth';
import { searchJobs } from '@/lib/api';
import type { Job } from '@/lib/types';

function JobList() {
  const [keyword, setKeyword] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [jobs, setJobs] = useState<Job[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await searchJobs(query, page);
      setJobs(res.content);
      setTotalPages(res.totalPages);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load jobs');
    } finally {
      setLoading(false);
    }
  }, [query, page]);

  useEffect(() => {
    load();
  }, [load]);

  const onSearch = (e: FormEvent) => {
    e.preventDefault();
    setPage(0);
    setQuery(keyword);
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Open positions</h1>
      <form onSubmit={onSearch} className="flex gap-2">
        <input
          aria-label="Search jobs"
          className="input"
          placeholder="Search by title, company or location"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <button type="submit" className="btn-primary">
          Search
        </button>
      </form>

      {error && (
        <div role="alert" className="alert-error">
          {error}
        </div>
      )}
      {loading ? (
        <p className="text-slate-500">Loading jobs...</p>
      ) : jobs.length === 0 ? (
        <p className="text-slate-500">No open jobs found.</p>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {jobs.map((job) => (
            <JobCard key={job.id} job={job} />
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-4">
          <button className="btn-secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </button>
          <span className="text-sm text-slate-500">
            Page {page + 1} of {totalPages}
          </span>
          <button
            className="btn-secondary"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}

export default function JobsPage() {
  return (
    <RequireAuth>
      <JobList />
    </RequireAuth>
  );
}
