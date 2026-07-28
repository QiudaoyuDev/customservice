import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import { Button, Input, Textarea, Tag, Modal, StatusFlow, EmptyState } from '../components/ui';
import { useTranslation, LANGS } from '../i18n';

export default function DocumentsPage() {
  const { t } = useTranslation();
  const [rows, setRows] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showUpload, setShowUpload] = useState(false);
  const [form, setForm] = useState({ title: '', language: 'en-US', region: 'EU', productModelId: '' });
  const [file, setFile] = useState<File | null>(null);
  const [previewId, setPreviewId] = useState<string | null>(null);
  const [preview, setPreview] = useState<any>(null);

  const load = async () => {
    setLoading(true);
    try {
      const list = await api('/knowledge');
      setRows(list ?? []);
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
    data.append('meta', JSON.stringify(form));
    const item = await api('/knowledge', { method: 'POST', body: data });
    setShowUpload(false);
    setFile(null);
    setForm({ title: '', language: 'en-US', region: 'EU', productModelId: '' });
    setPreviewId(item.id);
    setPreview(item);
    load();
  };

  const openPreview = async (id: string) => {
    const item = await api(`/knowledge/${id}`);
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
                  <td className="px-4 py-2">{d.language}</td>
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
              value={form.language}
              onChange={(e) => setForm({ ...form, language: e.target.value })}
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
            <Input value={form.productModelId} onChange={(e) => setForm({ ...form, productModelId: e.target.value })} placeholder={t('documents.productPlaceholder')} />
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
            {preview?.parsing && <span className="rounded bg-amber-100 px-2 py-0.5 text-amber-700">{t('documents.parsing')}</span>}
            {preview?.chunks != null && <span>{t('documents.chunks', { count: preview.chunks })}</span>}
          </div>
          <div className="max-h-72 overflow-y-auto whitespace-pre-wrap rounded border border-line p-3 text-xs text-ink">
            {preview?.extractedText || t('documents.noContent')}
          </div>
        </div>
      </Modal>
    </div>
  );
}
