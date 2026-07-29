import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {api, clearToken, getToken, setToken} from './api';

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
        const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
            accessToken: 'signed-token', email: 'operator@example.test', role: 'ADMIN',
        }), {status: 200, headers: {'Content-Type': 'application/json'}}));
        vi.stubGlobal('fetch', fetchMock);

        const response = await api('/auth/login', {
            method: 'POST', body: JSON.stringify({email: 'operator@example.test', password: 'not-persisted'}),
        });

        expect(response.email).toBe('operator@example.test');
        expect(fetchMock).toHaveBeenCalledWith('/api/auth/login', expect.objectContaining({method: 'POST'}));
        expect(getToken()).toBeNull();
        setToken(response.accessToken);
        expect(getToken()).toBe('signed-token');
    });
});
