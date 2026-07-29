/** 统一的 API 访问层：管理后台走 /api（需 Token），终端用户走 /public。
 *  所有请求都会打印调试日志，并回传后端注入的 X-Request-Id，便于前后端联调排查。 */

const TOKEN_KEY = 'accessToken';

export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setToken = (t: string) => localStorage.setItem(TOKEN_KEY, t);
export const clearToken = () => localStorage.removeItem(TOKEN_KEY);

type Level = 'log' | 'warn' | 'error';

function logApi(level: Level, msg: string, ...args: unknown[]) {
  if (!import.meta.env.DEV) return;
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
    logApi(
      'log',
      `REQ ${method} ${url} -> ${res.status} in ${Math.round(performance.now() - t0)}ms`,
    );
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
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...init.headers,
      },
    }),
  );
}

/** 管理后台文件上传（multipart，不手动设置 Content-Type）。 */
export async function apiUpload(path: string, form: FormData): Promise<any> {
  const token = getToken();
  return parse(
    await tracked('POST', `/api${path}`, {
      method: 'POST',
      headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
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
      headers: { 'Content-Type': 'application/json', ...init.headers },
    }),
  );
}

/** 匿名端 multipart 请求，浏览器负责补充 boundary。 */
export async function pubUpload(
  path: string,
  form: FormData,
  conversationToken?: string,
): Promise<any> {
  return parse(
    await tracked('POST', `/public${path}`, {
      method: 'POST',
      headers: conversationToken ? { 'X-Conversation-Token': conversationToken } : undefined,
      body: form,
    }),
  );
}

/** 终端用户流式回答（SSE）。解析后端 answer 事件，逐条回调。 */
export type AnswerStreamEvent = { event: string; data: any };

export async function streamAnswer(
  conversationId: string,
  onData: (frame: AnswerStreamEvent) => void,
  conversationToken?: string,
): Promise<void> {
  const t0 = performance.now();
  const res = await tracked('GET', `/public/conversations/${conversationId}/answers/stream`, {
    headers: conversationToken ? { 'X-Conversation-Token': conversationToken } : undefined,
  });
  if (!res.ok) {
    const requestId = res.headers.get('X-Request-Id');
    throw new Error(`流式回答失败 (${res.status})${requestId ? ` requestId=${requestId}` : ''}`);
  }
  const reader = res.body!.getReader();
  const decoder = new TextDecoder();
  let buf = '';
  let count = 0;
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buf += decoder.decode(value, { stream: true });
    const blocks = buf.split('\n\n');
    buf = blocks.pop() ?? '';
    for (const block of blocks) {
      const line = block.split('\n').find((l) => l.startsWith('data:'));
      const event =
        block
          .split('\n')
          .find((l) => l.startsWith('event:'))
          ?.slice(6)
          .trim() || 'message';
      if (!line) continue;
      const json = line.slice(5).trim();
      if (json) {
        let data: any;
        try {
          data = JSON.parse(json);
        } catch {
          logApi('warn', 'stream: malformed event ignored', json);
          continue;
        }
        onData({ event, data });
        count++;
      }
    }
  }
  logApi(
    'log',
    `stream done conversation=${conversationId} events=${count} in ${Math.round(performance.now() - t0)}ms`,
  );
}

/** 终端用户非流式回答（流式失败时的回退）。 */
export async function postAnswer(conversationId: string, conversationToken?: string): Promise<any> {
  return pub(`/conversations/${conversationId}/answers`, {
    method: 'POST',
    headers: conversationToken ? { 'X-Conversation-Token': conversationToken } : undefined,
  });
}

/* ---------- 诊断流程（管理后台） ---------- */
export const listFlows = () => api('/flows');
export const getFlow = (id: string) => api(`/flows/${id}`);
export const createFlow = (body: any) =>
  api('/flows', { method: 'POST', body: JSON.stringify(body) });
export const updateFlow = (id: string, body: any) =>
  api(`/flows/${id}`, { method: 'PUT', body: JSON.stringify(body) });
export const addNode = (id: string, body: any) =>
  api(`/flows/${id}/nodes`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
export const updateNode = (id: string, key: string, body: any) =>
  api(`/flows/${id}/nodes/${key}`, { method: 'PUT', body: JSON.stringify(body) });
export const deleteNode = (id: string, key: string) =>
  api(`/flows/${id}/nodes/${key}`, { method: 'DELETE' });
export const flowAction = (id: string, action: string) =>
  api(`/flows/${id}/${action}`, { method: 'POST' });
export const simulateFlow = (id: string) => api(`/flows/${id}/simulate`, { method: 'POST' });

/* ---------- 产品 / 型号 / 变体 / 固件 ---------- */
export const listProducts = () => api('/products');
export const createProduct = (body: any) =>
  api('/products', { method: 'POST', body: JSON.stringify(body) });
export const listProductModels = (productId?: string) =>
  api(productId ? `/products/${productId}/models` : '/product-models');
export const createProductModel = (body: any) =>
  api('/product-models', { method: 'POST', body: JSON.stringify(body) });
export const listProductVariants = () => api('/product-variants');
export const createProductVariant = (body: any) =>
  api('/product-variants', { method: 'POST', body: JSON.stringify(body) });
export const listFirmware = () => api('/firmware');
export const createFirmware = (body: any) =>
  api('/firmware', { method: 'POST', body: JSON.stringify(body) });

/* ---------- 二维码 ---------- */
export const listQrs = () => api('/qrs');
export const createQr = (body: any) =>
  api('/qrs', { method: 'POST', body: JSON.stringify(body) });
export const revokeQr = (id: string) => api(`/qrs/${id}/revoke`, { method: 'POST' });

/* ---------- 知识文档 ---------- */
export const listDocuments = () => api('/documents');
export const uploadDocument = (input: {
  file: File;
  title?: string;
  language?: string;
  region?: string;
  productModelId?: string;
}) => {
  const form = new FormData();
  form.append('file', input.file);
  if (input.title) form.append('title', input.title);
  if (input.language) form.append('language', input.language);
  if (input.region) form.append('region', input.region);
  if (input.productModelId) form.append('productModelId', input.productModelId);
  return apiUpload('/documents', form);
};
export const getDocumentContent = (id: string) => api(`/documents/${id}/content`);
export const previewDocumentUrl = (id: string) => `/api/documents/${id}/preview`;

/* ---------- 人工转接 ---------- */
export const listHandoffs = () => api('/handoffs');
export const claimHandoff = (id: string) =>
  api(`/handoffs/${id}/claim`, { method: 'POST' });
export const closeHandoff = (id: string, resolution: string, note?: string) =>
  api(`/handoffs/${id}/close`, {
    method: 'POST',
    body: JSON.stringify({ resolution, note }),
  });
export const addHandoffNote = (id: string, body: string) =>
  api(`/handoffs/${id}/notes`, { method: 'POST', body: JSON.stringify({ body }) });

/* ---------- 模型供应商 ---------- */
export const listModelProviders = () => api('/model-providers');
export const createModelProvider = (body: any) =>
  api('/model-providers', { method: 'POST', body: JSON.stringify(body) });
export const updateModelProvider = (id: string, body: any) =>
  api(`/model-providers/${id}`, { method: 'PUT', body: JSON.stringify(body) });
export const testModelProvider = (body: any) =>
  api('/model-providers/test', { method: 'POST', body: JSON.stringify(body) });
export const defaultModelProvider = (id: string) =>
  api(`/model-providers/${id}/default`, { method: 'POST' });

/* ---------- 检索 / 分析 ---------- */
export const searchV2 = (body: any) =>
  api('/search', { method: 'POST', body: JSON.stringify(body) });
export const loadAnalytics = () => api('/analytics');
