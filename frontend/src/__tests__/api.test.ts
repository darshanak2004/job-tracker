import { ApiError, API_URL, applyToJob, login, setToken, withdrawApplication } from '@/lib/api';

const mockFetch = jest.fn();
global.fetch = mockFetch as unknown as typeof fetch;

function response(status: number, body?: unknown) {
  return {
    ok: status >= 200 && status < 300,
    status,
    text: async () => (body === undefined ? '' : typeof body === 'string' ? body : JSON.stringify(body)),
  };
}

describe('api client', () => {
  beforeEach(() => {
    mockFetch.mockReset();
    window.localStorage.clear();
  });

  it('POSTs JSON to the backend', async () => {
    mockFetch.mockResolvedValue(response(200, { token: 't', user: { id: 1 } }));

    const res = await login('a@b.com', 'pw');

    expect(res.token).toBe('t');
    expect(mockFetch).toHaveBeenCalledWith(
      `${API_URL}/api/auth/login`,
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ email: 'a@b.com', password: 'pw' }),
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
      }),
    );
  });

  it('attaches the stored JWT as a Bearer token', async () => {
    setToken('my.jwt.token');
    mockFetch.mockResolvedValue(response(201, { id: 1 }));

    await applyToJob(3, 'hi');

    const [, init] = mockFetch.mock.calls[0];
    expect(init.headers.Authorization).toBe('Bearer my.jwt.token');
  });

  it('does not send an Authorization header when logged out', async () => {
    mockFetch.mockResolvedValue(response(200, { token: 't', user: {} }));

    await login('a@b.com', 'pw');

    const [, init] = mockFetch.mock.calls[0];
    expect(init.headers.Authorization).toBeUndefined();
  });

  it('turns error responses into ApiError with message and field errors', async () => {
    mockFetch.mockResolvedValue(
      response(400, { message: 'Validation failed', fieldErrors: { email: 'Email must be valid' } }),
    );

    await expect(login('bad', 'pw')).rejects.toMatchObject({
      name: 'ApiError',
      status: 400,
      message: 'Validation failed',
      fieldErrors: { email: 'Email must be valid' },
    });
  });

  it('falls back to a generic message when the error body is not JSON', async () => {
    mockFetch.mockResolvedValue(response(502, '<html>Bad gateway</html>'));

    await expect(login('a@b.com', 'pw')).rejects.toThrow('Request failed (502)');
  });

  it('resolves to undefined for 204 No Content', async () => {
    mockFetch.mockResolvedValue(response(204));

    await expect(withdrawApplication(1)).resolves.toBeUndefined();
  });

  it('reports an unreachable backend as ApiError(0)', async () => {
    mockFetch.mockRejectedValue(new TypeError('Failed to fetch'));

    const err = await login('a@b.com', 'pw').catch((e) => e);

    expect(err).toBeInstanceOf(ApiError);
    expect(err.status).toBe(0);
    expect(err.message).toMatch(/Cannot reach the server/);
  });
});
