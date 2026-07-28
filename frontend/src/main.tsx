import { useState } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';

/** The management console intentionally keeps API access in one place so auth handling stays consistent. */
const api = async (path: string, init: RequestInit = {}) => {
  const token = localStorage.getItem('accessToken');
  const response = await fetch(`/api${path}`, { ...init, headers: { ...init.headers, ...(token ? { Authorization: `Bearer ${token}` } : {}) } });
  if (!response.ok) throw new Error((await response.json().catch(() => null))?.message ?? '请求失败');
  return response.status === 204 ? null : response.json();
};

type Product = { id: string; family: string; model: string; displayName: string; region: string; status: string };

function App() {
  const [tab, setTab] = useState<'products' | 'documents' | 'search'>('products');
  const [products, setProducts] = useState<Product[]>([]); const [message, setMessage] = useState('连接 API 后可管理首批知识资产');
  const loadProducts = async () => { try { setProducts(await api('/products')); setMessage('产品列表已刷新'); } catch (e) { setMessage((e as Error).message); } };
  const createProduct = async () => { const family=prompt('产品系列'); const model=prompt('型号'); const displayName=prompt('显示名称'); const region=prompt('地区，例如 US'); if (!family || !model || !displayName || !region) return; try { await api('/products',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({family,model,displayName,region})}); await loadProducts(); } catch(e) {setMessage((e as Error).message);} };
  return <main><aside><div className="brand"><small>HARDWARE AI</small><strong>Service<br/>Console</strong></div><nav>{(['products','documents','search'] as const).map(item => <button className={tab===item?'active':''} onClick={()=>setTab(item)} key={item}>{item==='products'?'产品上下文':item==='documents'?'知识文档':'检索验证'}</button>)}</nav><p className="status">● {message}</p></aside><section><header><p>知识运营 / 初版 v0.1</p><h1>{tab==='products'?'产品上下文':'知识工作区'}</h1></header>{tab==='products' && <><div className="actions"><button onClick={loadProducts}>刷新列表</button><button className="primary" onClick={createProduct}>新建产品</button></div><div className="grid">{products.map(p=><article key={p.id}><small>{p.region} · {p.status}</small><h2>{p.displayName}</h2><p>{p.family} / {p.model}</p></article>)}{products.length===0&&<article className="empty">尚无产品。创建首个型号后，即可绑定二维码与限定知识适用范围。</article>}</div></>}{tab==='documents'&&<article className="empty"><h2>知识文档</h2><p>后端已提供 PDF、DOCX、PNG、JPEG 上传接口。下一步将接入解析任务状态和审核发布操作。</p></article>}{tab==='search'&&<article className="empty"><h2>检索验证</h2><p>发布索引与混合检索接口将在文档解析 Worker 完成后接入此页面。</p></article>}</section></main>;
}
createRoot(document.getElementById('root')!).render(<App />);
