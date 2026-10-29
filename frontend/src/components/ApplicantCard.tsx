'use client';

import { APPLICATION_STATUSES, type Application, type ApplicationStatus } from '@/lib/types';

interface Props {
  application: Application;
  onStatusChange: (id: number, status: ApplicationStatus) => void;
}

/** One applicant, as seen by the recruiter. The select moves them through the pipeline. */
export default function ApplicantCard({ application, onStatusChange }: Props) {
  const selectId = `status-${application.id}`;
  return (
    <article className="card space-y-2">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h3 className="font-semibold text-slate-900">{application.candidateName}</h3>
          <p className="text-sm text-slate-500">{application.candidateEmail}</p>
        </div>
        <div>
          <label htmlFor={selectId} className="sr-only">
            Status for {application.candidateName}
          </label>
          <select
            id={selectId}
            className="input"
            value={application.status}
            onChange={(e) => onStatusChange(application.id, e.target.value as ApplicationStatus)}
          >
            {APPLICATION_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s.replace('_', ' ').toLowerCase()}
              </option>
            ))}
          </select>
        </div>
      </div>
      {application.coverLetter && (
        <p className="whitespace-pre-line rounded-lg bg-slate-50 p-3 text-sm text-slate-600">
          {application.coverLetter}
        </p>
      )}
    </article>
  );
}
