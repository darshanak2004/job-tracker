export type Role = 'CANDIDATE' | 'RECRUITER';
export type JobStatus = 'OPEN' | 'CLOSED';
export type ApplicationStatus = 'APPLIED' | 'UNDER_REVIEW' | 'INTERVIEW' | 'OFFERED' | 'REJECTED';

export const APPLICATION_STATUSES: ApplicationStatus[] = [
  'APPLIED',
  'UNDER_REVIEW',
  'INTERVIEW',
  'OFFERED',
  'REJECTED',
];

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Job {
  id: number;
  title: string;
  company: string;
  location: string;
  description: string;
  salaryRange: string | null;
  status: JobStatus;
  recruiterId: number;
  recruiterName: string;
  createdAt: string;
}

export interface JobInput {
  title: string;
  company: string;
  location: string;
  description: string;
  salaryRange?: string;
  status?: JobStatus;
}

export interface Application {
  id: number;
  jobId: number;
  jobTitle: string;
  company: string;
  candidateId: number;
  candidateName: string;
  candidateEmail: string;
  coverLetter: string | null;
  status: ApplicationStatus;
  appliedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
