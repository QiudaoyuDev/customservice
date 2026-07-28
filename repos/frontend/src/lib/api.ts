/** 统一的 API 访问层：管理后台走 /api（需 Token），终端用户走 /public。
 *  所有请求都会打印调试日志，并回传后端注入的 X-Request-Id，便于前后端联调排查。 */

const TOKEN_KEY = 'accessToken';

export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setToken = (t: string) => localStorage.setItem(TOKEN_KEY, t);
export const clearToken = () => localStorage.removeItem(TOKEN_KEY);

type Level = 'log' | 'warn' | 'error';

function logApi(level: Level, msg: string, ...args: unknown[]) {
    // 开发期始终输出；生产环境如不需要可在此加环境判断。
    // eslint-disable-next-line no-console
    (console[level] ?? console.log).call(console, `[api] ${msg}`, ...args);
}

async function parse(res: Response) {
    const requestId = res.headers.get('X-Request-Id');
    const rid = requestId ? ` (requestId=${requestId})` : '';
    if (!res.ok) {
        const body = await res.json().catch(() => null);
        const message = body?.message ?? `请求失败 (${res.status})`;
        logApi('error', `RESP ${res.status}${rid}: ${message}`);
        throw new Error(message);
    }
    logApi('log', `RESP ${res.status}${rid}`);
    return res.status === 204 ? null : res.json();
}

/** 包装一次 fetch：记录请求方法/路径/耗时与响应状态。 */
async function tracked(method: string, url: string, init: RequestInit): Promise<Response> {
    const t0 = performance.now();
    try {
        const res = await fetch(url, init);
        logApi('log', `REQ ${method} ${url} -> ${res.status} in ${Math.round(performance.now() - t0)}ms`);
        return res;
    } catch (e) {
        logApi('error', `REQ ${method} ${url} FAILED in ${Math.round(performance.now() - t0)}ms`, e);
        throw e;
    }
}

/** 管理后台 JSON 请求（自动带 Bearer）。 */
export async function api(path: string, init: RequestInit = {}): Promise<any> {
    const token = getToken();
    const method = (init.method ?? 'GET').toUpperCase();
    return parse(
        await tracked(method, `/api${path}`, {
            ...init,
            headers: {'Content-Type': 'application/json', ...(token ? {Authorization: `Bearer ${token}`} : {}), ...init.headers},
        }),
    );
}

/** 管理后台文件上传（multipart，不手动设置 Content-Type）。 */
export async function apiUpload(path: string, form: FormData): Promise<any> {
    const token = getToken();
    return parse(
        await tracked('POST', `/api${path}`, {
            method: 'POST',
            headers: {...(token ? {Authorization: `Bearer ${token}`} : {})},
            body: form,
        }),
    );
}

/** 终端用户 JSON 请求（无需登录）。 */
export async function pub(path: string, init: RequestInit = {}): Promise<any> {
    const method = (init.method ?? 'GET').toUpperCase();
    return parse(
        await tracked(method, `/public${path}`, {
            ...init,
            headers: {'Content-Type': 'application/json', ...init.headers},
        }),
    );
}

/** 终端用户流式回答（SSE）。解析后端 answer 事件，逐条回调。 */
export async function streamAnswer(
    conversationId: string,
    onData: (a: any) => void,
): Promise<void> {
    const t0 = performance.now();
    const res = await tracked('GET', `/public/conversations/${conversationId}/answers/stream`, {});
    if (!res.ok) {
        const requestId = res.headers.get('X-Request-Id');
        throw new Error(`流式回答失败 (${res.status})${requestId ? ` requestId=${requestId}` : ''}`);
    }
    const reader = res.body!.getReader();
    const decoder = new TextDecoder();
    let buf = '';
    let count = 0;
    while (true) {
        const {done, value} = await reader.read();
        if (done) break;
        buf += decoder.decode(value, {stream: true});
        const blocks = buf.split('\n\n');
        buf = blocks.pop() ?? '';
        for (const block of blocks) {
            const line = block.split('\n').find((l) => l.startsWith('data:'));
            if (!line) continue;
            const json = line.slice(5).trim();
            if (json) {
                try {
                    onData(JSON.parse(json));
                    count++;
                } catch {
                    logApi('warn', 'stream: malformed event ignored', json);
                }
            }
        }
    }
    logApi('log', `stream done conversation=${conversationId} events=${count} in ${Math.round(performance.now() - t0)}ms`);
}

/** 终端用户非流式回答（流式失败时的回退）。 */
export async function postAnswer(conversationId: string): Promise<any> {
    return pub(`/conversations/${conversationId}/answers`, {method: 'POST'});
}

/* ---------- 诊断流程（管理后台） ---------- */
export const listFlows = () => api('/flows');
export const getFlow = (id: string) => api(`/flows/${id}`);
export const createFlow = (body: any) => api('/flows', {method: 'POST', body: JSON.stringify(body)});
export const updateFlow = (id: string, body: any) => api(`/flows/${id}`, {method: 'PUT', body: JSON.stringify(body)});
export const addNode = (id: string, body: any) => api(`/flows/${id}/nodes`, {
    method: 'POST',
    body: JSON.stringify(body)
});
export const updateNode = (id: string, key: string, body: any) =>
    api(`/flows/${id}/nodes/${key}`, {method: 'PUT', body: JSON.stringify(body)});
export const deleteNode = (id: string, key: string) => api(`/flows/${id}/nodes/${key}`, {method: 'DELETE'});
export const flowAction = (id: string, action: string) => api(`/flows/${id}/${action}`, {method: 'POST'});
export const simulateFlow = (id: string) => api(`/flows/${id}/simulate`, {method: 'POST'});
