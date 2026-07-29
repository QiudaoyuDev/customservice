import { useEffect, useState } from 'react';
import { api, apiUpload } from '../lib/api';
import { Product } from '../lib/types';
import { Button, Input, Textarea, Tag, Modal, StatusFlow, EmptyState } from '../components/ui';
import { useTranslation, LANGS } from '../i18n';

export default function DocumentsPage() {
  const { t } = useTranslation();
  const [rows, setRows] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showUpload, setShowUpload] = useState(false);
  const [form, setForm] = useState({ title: '', locale: 'en', region: 'EU', productModelId: '' });
  const [products, setProducts] = useState<Product[]>([]);
  const [file, setFile] = useState<File | null>(null);
  const [previewId, setPreviewId] = useState<string | null>(null);
  const [preview, setPreview] = useState<any>(null);

  const load = async () => {
    setLoading(true);
    try {
      const [list, productList] = await Promise.all([api('/documents'), api('/products')]);
      setRows(list ?? []);
      setProducts(productList ?? []);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    load();
  }, []);

  const upload = async () => {
    if (!file) return;
    const data = new FormData();
    data.append('file', file);
    data.append('title', form.title);
    data.append('locale', form.locale);
    data.append('region', form.region);
    data.append('productModelId', form.productModelId);
    const item = await apiUpload('/documents', data);
    setShowUpload(false);
    setFile(null);
    setForm({ title: '', locale: 'en', region: 'EU', productModelId: '' });
    setPreviewId(item.id);
    setPreview(item);
    load();
  };

  const openPreview = async (id: string) => {
    const item = await api(`/documents/${id}/preview`);
    setPreviewId(id);
    setPreview(item);
  };

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-ink">{t('documents.title')}</h1>
          <p className="text-sm text-ink2">{t('documents.subtitle')}</p>
        </div>
        <Button variant="ai" onClick={() => setShowUpload(true)}>
          {t('documents.new')}
        </Button>
      </div>

      {loading ? (
        <div className="text-sm text-ink2">{t('common.loading')}</div>
      ) : rows.length === 0 ? (
        <EmptyState title={t('documents.emptyTitle')} hint={t('documents.emptyHint')} />
      ) : (
        <div className="overflow-hidden rounded-xl border border-line bg-white">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-ink2">
              <tr>
                <th className="px-4 py-2 text-left">{t('documents.thTitle')}</th>
                <th className="px-4 py-2 text-left">{t('documents.thLanguage')}</th>
                <th className="px-4 py-2 text-left">{t('documents.thRegion')}</th>
                <th className="px-4 py-2 text-left">{t('documents.thProduct')}</th>
                <th className="px-4 py-2 text-left">{t('documents.thStatus')}</th>
                <th className="px-4 py-2 text-left">{t('documents.thActions')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((d) => (
                <tr key={d.id} className="border-t border-line">
                  <td className="px-4 py-2">{d.title}</td>
                  <td className="px-4 py-2">{d.locale}</td>
                  <td className="px-4 py-2">{d.region}</td>
                  <td className="px-4 py-2 font-mono text-xs">{d.productModelId || '—'}</td>
                  <td className="px-4 py-2">
                    <StatusFlow status={d.status} />
                  </td>
                  <td className="px-4 py-2">
                    <button className="text-ai hover:underline" onClick={() => openPreview(d.id)}>
                      {t('documents.preview')}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal
        open={showUpload}
        title={t('documents.uploadTitle')}
        onClose={() => setShowUpload(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setShowUpload(false)}>
              {t('common.cancel')}
            </Button>
            <Button variant="ai" onClick={upload} disabled={!file}>
              {t('documents.upload')}
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('documents.titleField')}</label>
            <Input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('documents.language')}</label>
            <select
              className="w-full rounded border border-line px-3 py-2 text-sm"
              value={form.locale}
              onChange={(e) => setForm({ ...form, locale: e.target.value.split('-')[0] })}
            >
              {LANGS.map((l) => (
                <option key={l} value={l}>
                  {l}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('documents.region')}</label>
            <select
              className="w-full rounded border border-line px-3 py-2 text-sm"
              value={form.region}
              onChange={(e) => setForm({ ...form, region: e.target.value })}
            >
              {['EU', 'NA', 'APAC', 'LATAM', 'MEA'].map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('documents.productModelId')}</label>
            <select className="w-full rounded border border-line px-3 py-2 text-sm" value={form.productModelId} onChange={(e) => setForm({ ...form, productModelId: e.target.value })}>
              <option value="">{t('documents.productPlaceholder')}</option>
              {products.map((p) => <option key={p.id} value={p.id}>{p.displayName} · {p.model}</option>)}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('documents.file')}</label>
            <input type="file" accept=".pdf,.docx,.png,.jpeg,.jpg" onChange={(e) => setFile(e.target.files?.[0] ?? null)} className="block w-full text-sm" />
          </div>
        </div>
      </Modal>

      <Modal open={!!previewId} title={t('documents.preview')} onClose={() => setPreviewId(null)}>
        <div className="space-y-3 text-sm">
          <div className="flex items-center gap-2 text-xs text-ink2">
            <StatusFlow status={preview?.status} />
            {preview?.chunks != null && <span>{t('documents.chunks', { count: preview.chunks.length })}</span>}
          </div>
          <div className="max-h-72 overflow-y-auto whitespace-pre-wrap rounded border border-line p-3 text-xs text-ink">
            {preview?.text || t('documents.noContent')}
          </div>
        </div>
      </Modal>
    </div>
  );
}
