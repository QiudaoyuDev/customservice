import { useState } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';

/** The management console intentionally keeps API access in one place so auth handling stays consistent. */
const api = async (path: string, init: RequestInit = {}) => {
  const token = localStorage.getItem('accessToken');
  const response = await fetch(`/api${path}`, {
    ...init,
    headers: { ...init.headers, ...(token ? { Authorization: `Bearer ${token}` } : {}) },
  });
  if (!response.ok)
    throw new Error((await response.json().catch(() => null))?.message ?? '请求失败');
  return response.status === 204 ? null : response.json();
};

type Product = {
  id: string;
  family: string;
  model: string;
  displayName: string;
  region: string;
  status: string;
  hardwareVersion?: string;
  firmwareMin?: string;
  firmwareMax?: string;
};
type QrBinding = { id: string; productModelId: string; batch?: string; serialNumber?: string; status: string; expiresAt?: string };
type Document = { id: string; revisionId: string; title: string; locale: string; status: string };
type Preview = { title: string; status: string; text: string; chunks: { chunkNo: number; source: string; text: string }[] };

function App() {
  const [tab, setTab] = useState<'products' | 'qrs' | 'documents' | 'search'>('products');
  const [products, setProducts] = useState<Product[]>([]);
  const [documents, setDocuments] = useState<Document[]>([]);
  const [preview, setPreview] = useState<Preview | null>(null);
  const [qrs, setQrs] = useState<QrBinding[]>([]);
  const [searchResult, setSearchResult] = useState<any>(null);
  const [message, setMessage] = useState('连接 API 后可管理首批知识资产');
  const loadProducts = async () => {
    try {
      setProducts(await api('/products'));
      setMessage('产品列表已刷新');
    } catch (e) {
      setMessage((e as Error).message);
    }
  };
  const loadQrs = async () => { try { setQrs(await api('/qr-bindings')); setMessage('二维码列表已刷新'); } catch (e) { setMessage((e as Error).message); } };
  const createQr = async () => {
    const productModelId = prompt('产品 ID'); if (!productModelId) return;
    const batch = prompt('批次（可选）') ?? ''; const serialNumber = prompt('序列号（可选）') ?? '';
    try { const result = await api('/qr-bindings', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ productModelId, batch, serialNumber }) }); await loadQrs(); setMessage(`二维码已创建，请立即保存令牌：${result.token}`); } catch (e) { setMessage((e as Error).message); }
  };
  const revokeQr = async (id: string) => { const reason = prompt('撤销原因'); if (!reason) return; try { await api(`/qr-bindings/${id}/revoke`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ reason }) }); await loadQrs(); } catch (e) { setMessage((e as Error).message); } };
  const loadDocuments = async () => {
    try { setDocuments(await api('/documents')); setMessage('文档列表已刷新'); } catch (e) { setMessage((e as Error).message); }
  };
  const openPreview = async (id: string) => {
    try { setPreview(await api(`/documents/${id}/preview`)); } catch (e) { setMessage((e as Error).message); }
  };
  const transition = async (id: string, action: 'submit' | 'approve' | 'publish' | 'deprecate' | 'archive' | 'restore') => {
    try { await api(`/knowledge-revisions/${id}/${action}`, { method: 'POST' }); await loadDocuments(); setMessage(`已执行 ${action}`); } catch (e) { setMessage((e as Error).message); }
  };
  const uploadDocument = async () => {
    const title = prompt('文档标题'); const locale = prompt('语言，例如 en-US');
    const productModelId = prompt('适用产品 ID'); const region = prompt('地区，例如 US');
    if (!title || !locale || !productModelId || !region) return;
    const input = document.createElement('input'); input.type = 'file'; input.accept = '.pdf,.docx,.png,.jpg,.jpeg';
    input.onchange = async () => {
      const file = input.files?.[0]; if (!file) return;
      const body = new FormData(); body.append('title', title); body.append('locale', locale); body.append('productModelId', productModelId); body.append('region', region); body.append('file', file);
      try { await api('/documents', { method: 'POST', body }); await loadDocuments(); setMessage('文件已上传，等待解析'); } catch (e) { setMessage((e as Error).message); }
    };
    input.click();
  };
  const runSearch = async () => {
    const query = prompt('检索问题'); const productModelId = prompt('产品 ID'); const region = prompt('地区，例如 US'); const locale = prompt('语言，例如 en-US');
    if (!query || !productModelId || !region || !locale) return;
    try { setSearchResult(await api('/search', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ query, productModelId, region, locale, limit: 10 }) })); setMessage('检索完成；请核验来源和适用范围'); } catch (e) { setMessage((e as Error).message); }
  };
  const createProduct = async () => {
    const family = prompt('产品系列');
    const model = prompt('型号');
    const displayName = prompt('显示名称');
    const region = prompt('地区，例如 US');
    if (!family || !model || !displayName || !region) return;
    try {
      await api('/products', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ family, model, displayName, region, hardwareVersion: prompt('硬件版本（可选）'), firmwareMin: prompt('最低固件（可选）'), firmwareMax: prompt('最高固件（可选）') }),
      });
      await loadProducts();
    } catch (e) {
      setMessage((e as Error).message);
    }
  };
  return (
    <main>
      <aside>
        <div className="brand">
          <small>HARDWARE AI</small>
          <strong>
            Service
            <br />
            Console
          </strong>
        </div>
        <nav>
          {(['products', 'qrs', 'documents', 'search'] as const).map((item) => (
            <button
              className={tab === item ? 'active' : ''}
              onClick={() => setTab(item)}
              key={item}
            >
              {item === 'products' ? '产品上下文' : item === 'qrs' ? '二维码' : item === 'documents' ? '知识文档' : '检索验证'}
            </button>
          ))}
        </nav>
        <p className="status">● {message}</p>
      </aside>
      <section>
        <header>
          <p>知识运营 / 初版 v0.1</p>
          <h1>{tab === 'products' ? '产品上下文' : '知识工作区'}</h1>
        </header>
        {tab === 'products' && (
          <>
            <div className="actions">
              <button onClick={loadProducts}>刷新列表</button>
              <button className="primary" onClick={createProduct}>
                新建产品
              </button>
            </div>
            <div className="grid">
              {products.map((p) => (
                <article key={p.id}>
                  <small>
                    {p.region} · {p.status}
                  </small>
                  <h2>{p.displayName}</h2>
                  <p>
                    {p.family} / {p.model}
                  </p>
                  <p>{p.hardwareVersion || '全部硬件'} · {p.firmwareMin || '任意'} ~ {p.firmwareMax || '任意'}</p>
                </article>
              ))}
              {products.length === 0 && (
                <article className="empty">
                  尚无产品。创建首个型号后，即可绑定二维码与限定知识适用范围。
                </article>
              )}
            </div>
          </>
        )}
        {tab === 'qrs' && <><div className="actions"><button onClick={loadQrs}>刷新列表</button><button className="primary" onClick={createQr}>创建二维码</button></div><div className="grid">{qrs.map(q => <article key={q.id}><small>{q.status}</small><h2>{q.batch || '未分批'}</h2><p>{q.serialNumber || '无序列号'}</p>{q.status === 'ACTIVE' && <button onClick={() => revokeQr(q.id)}>撤销</button>}</article>)}{qrs.length === 0 && <article className="empty">暂无二维码。创建后令牌只显示一次，请交付至安全的二维码生成流程。</article>}</div></>}
        {tab === 'documents' && (
          <>
            <div className="actions"><button onClick={loadDocuments}>刷新文档</button><button className="primary" onClick={uploadDocument}>上传文档</button></div>
            <div className="grid">
              {documents.map((d) => <article key={d.id}><small>{d.locale} · {d.status}</small><h2>{d.title}</h2><div className="actions"><button onClick={() => openPreview(d.id)}>预览</button>{d.revisionId && d.status === 'DRAFT' && <button onClick={() => transition(d.revisionId, 'submit')}>送审</button>}{d.revisionId && d.status === 'REVIEW' && <button onClick={() => transition(d.revisionId, 'approve')}>批准</button>}{d.revisionId && d.status === 'APPROVED' && <button className="primary" onClick={() => transition(d.revisionId, 'publish')}>发布</button>}{d.revisionId && d.status === 'PUBLISHED' && <button onClick={() => transition(d.revisionId, 'deprecate')}>下架</button>}{d.revisionId && d.status === 'DEPRECATED' && <button className="primary" onClick={() => transition(d.revisionId, 'restore')}>回滚发布</button>}</div></article>)}
              {documents.length === 0 && <article className="empty">暂无文档。可通过 API 上传 PDF、DOCX、PNG 或 JPEG，再在此查看解析和审核状态。</article>}
            </div>
            {preview && <article className="empty"><h2>{preview.title}</h2><small>{preview.status}</small><pre>{preview.text}</pre><p>检索块：{preview.chunks.length}</p></article>}
          </>
        )}
        {tab === 'search' && (
          <><div className="actions"><button className="primary" onClick={runSearch}>执行限定检索</button></div><article className="empty"><h2>检索验证</h2><p>请求必须填写产品、地区和语言；服务端会额外绑定当前登录租户和已发布状态。</p>{searchResult && <pre>{JSON.stringify(searchResult, null, 2)}</pre>}</article></>
        )}
      </section>
    </main>
  );
}
createRoot(document.getElementById('root')!).render(<App />);
