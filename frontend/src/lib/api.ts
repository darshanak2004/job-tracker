import type {
  Application,
  ApplicationStatus,
  AuthResponse,
  Job,
  JobInput,
  PageResponse,
  Role,
  User,
} from './types';

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
const TOKEN_KEY = 'jt_token';

// ---- token storage (localStorage; wrapped because it can throw / not exist on the server) ----
export function getToken(): string | null {
  try {
    return typeof window === 'undefined' ? null : window.localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token: string): void {
  try {
    window.localStorage.setItem(TOKEN_KEY, token);
  } catch {
    /* ignore */
  }
}

export function clearToken(): void {
  try {
    window.localStorage.removeItem(TOKEN_KEY);
  } catch {
    /* ignore */
  }
}

// ---- errors ----
export class ApiError extends Error {
  status: number;
  fieldErrors?: Record<string, string>;

  constructor(status: number, message: string, fieldErrors?: Record<string, string>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

interface RequestOptions {
  method?: string;
  body?: unknown;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`; // the JWT travels in this header on every call
  }

  let res: Response;
  try {
    res = await fetch(`${API_URL}${path}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Is the backend running?');
  }

  if (res.status === 204) {
    return undefined as T;
  }

  const text = await res.text();
  let data: unknown = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }

  if (!res.ok) {
    const err = (data ?? {}) as { message?: string; fieldErrors?: Record<string, string> };
    throw new ApiError(res.status, err.message ?? `Request failed (${res.status})`, err.fieldErrors);
  }
  return data as T;
}

// ---- auth ----
export const register = (name: string, email: string, password: string, role: Role) =>
  request<AuthResponse>('/api/auth/register', { method: 'POST', body: { name, email, password, role } });

export const login = (email: string, password: string) =>
  request<AuthResponse>('/api/auth/login', { method: 'POST', body: { email, password } });

export const me = () => request<User>('/api/auth/me');

// ---- jobs ----
export const searchJobs = (keyword: string, page: number, size = 9) => {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (keyword.trim()) {
    params.set('keyword', keyword.trim());
  }
  return request<PageResponse<Job>>(`/api/jobs?${params.toString()}`);
};

export const getJob = (id: number) => request<Job>(`/api/jobs/${id}`);
export const myJobs = () => request<Job[]>('/api/jobs/mine');
export const createJob = (input: JobInput) => request<Job>('/api/jobs', { method: 'POST', body: input });
export const updateJob = (id: number, input: JobInput) =>
  request<Job>(`/api/jobs/${id}`, { method: 'PUT', body: input });
export const deleteJob = (id: number) => request<void>(`/api/jobs/${id}`, { method: 'DELETE' });

// ---- applications ----
export const applyToJob = (jobId: number, coverLetter: string) =>
  request<Application>(`/api/jobs/${jobId}/applications`, { method: 'POST', body: { coverLetter } });

export const myApplications = () => request<Application[]>('/api/applications/mine');
export const jobApplications = (jobId: number) => request<Application[]>(`/api/jobs/${jobId}/applications`);
export const updateApplicationStatus = (id: number, status: ApplicationStatus) =>
  request<Application>(`/api/applications/${id}/status`, { method: 'PATCH', body: { status } });
export const withdrawApplication = (id: number) =>
  request<void>(`/api/applications/${id}`, { method: 'DELETE' });
