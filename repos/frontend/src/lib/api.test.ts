import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api, clearToken, getToken, setToken, streamAnswer } from './api';

describe('management API client', () => {
  beforeEach(() => {
    const values = new Map<string, string>();
    vi.stubGlobal('localStorage', {
      getItem: (key: string) => values.get(key) ?? null,
      setItem: (key: string, value: string) => values.set(key, value),
      removeItem: (key: string) => values.delete(key),
    });
  });

  afterEach(() => {
    clearToken();
    vi.unstubAllGlobals();
  });

  it('sends login credentials only to the real authentication endpoint and persists no password', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          accessToken: 'signed-token',
          email: 'operator@example.test',
          role: 'ADMIN',
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    const response = await api('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: 'operator@example.test', password: 'not-persisted' }),
    });

    expect(response.email).toBe('operator@example.test');
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/login',
      expect.objectContaining({ method: 'POST' }),
    );
    expect(getToken()).toBeNull();
    setToken(response.accessToken);
    expect(getToken()).toBe('signed-token');
  });

  it('parses named SSE events and preserves incremental answer chunks', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue(
          new Response(
            [
              'event: meta\ndata: {"answerId":"a1","intent":"TROUBLESHOOT"}\n\n',
              'event: delta\ndata: {"answerId":"a1","content":"first "}\n\n',
              'event: delta\ndata: {"answerId":"a1","content":"second"}\n\n',
              'event: done\ndata: {"answerId":"a1"}\n\n',
            ].join(''),
            { status: 200, headers: { 'Content-Type': 'text/event-stream' } },
          ),
        ),
    );
    const frames: Array<{ event: string; data: any }> = [];

    await streamAnswer('conversation-1', (frame) => frames.push(frame), 'anonymous-token');

    expect(frames).toEqual([
      { event: 'meta', data: { answerId: 'a1', intent: 'TROUBLESHOOT' } },
      { event: 'delta', data: { answerId: 'a1', content: 'first ' } },
      { event: 'delta', data: { answerId: 'a1', content: 'second' } },
      { event: 'done', data: { answerId: 'a1' } },
    ]);
  });
});
