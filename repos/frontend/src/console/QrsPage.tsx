import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import { Product, QrBinding } from '../lib/types';
import { Button, Input, Modal, Tag, EmptyState } from '../components/ui';
import { useTranslation } from '../i18n';

export default function QrsPage() {
  const { t } = useTranslation();
  const [rows, setRows] = useState<QrBinding[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [variants, setVariants] = useState<
    Array<{ id: string; hardwareRevision?: string; sku?: string; region: string }>
  >([]);
  const [loading, setLoading] = useState(true);
  const [showNew, setShowNew] = useState(false);
  const [showToken, setShowToken] = useState<any>(null);
  const [form, setForm] = useState({
    productModelId: '',
    productVariantId: '',
    initialFirmwareVersion: '',
    batch: '',
    serialNumber: '',
  });
  const [copied, setCopied] = useState<string | null>(null);
  const [revokeId, setRevokeId] = useState<string | null>(null);
  const [revokeReason, setRevokeReason] = useState('');

  const load = async () => {
    setLoading(true);
    try {
      const [list, productList] = await Promise.all([api('/qr-bindings'), api('/products')]);
      setRows(list ?? []);
      setProducts(productList ?? []);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    load();
  }, []);

  const create = async () => {
    const item = await api('/qr-bindings', { method: 'POST', body: JSON.stringify(form) });
    setShowNew(false);
    setForm({
      productModelId: '',
      productVariantId: '',
      initialFirmwareVersion: '',
      batch: '',
      serialNumber: '',
    });
    setShowToken(item);
    load();
  };

  const doRevoke = async () => {
    if (!revokeId) return;
    await api(`/qr-bindings/${revokeId}/revoke`, {
      method: 'POST',
      body: JSON.stringify({ reason: revokeReason }),
    });
    setRevokeId(null);
    setRevokeReason('');
    load();
  };

  const selectProduct = async (productModelId: string) => {
    setForm({ ...form, productModelId, productVariantId: '', initialFirmwareVersion: '' });
    setVariants(productModelId ? await api(`/products/${productModelId}/variants`) : []);
  };

  const copy = (token: string, id: string) => {
    navigator.clipboard?.writeText(token);
    setCopied(id);
    setTimeout(() => setCopied((c) => (c === id ? null : c)), 1500);
  };

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-ink">{t('qrs.title')}</h1>
          <p className="text-sm text-ink2">{t('qrs.subtitle')}</p>
        </div>
        <Button variant="ai" onClick={() => setShowNew(true)}>
          {t('qrs.new')}
        </Button>
      </div>

      {loading ? (
        <div className="text-sm text-ink2">{t('common.loading')}</div>
      ) : rows.length === 0 ? (
        <EmptyState title={t('qrs.emptyTitle')} hint={t('qrs.emptyHint')} />
      ) : (
        <div className="overflow-hidden rounded-xl border border-line bg-white">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-ink2">
              <tr>
                <th className="px-4 py-2 text-left">{t('qrs.thModelId')}</th>
                <th className="px-4 py-2 text-left">{t('qrs.thBatch')}</th>
                <th className="px-4 py-2 text-left">{t('qrs.thSerial')}</th>
                <th className="px-4 py-2 text-left">{t('qrs.thStatus')}</th>
                <th className="px-4 py-2 text-left">{t('qrs.thCreated')}</th>
                <th className="px-4 py-2 text-left">{t('qrs.thActions')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((q) => (
                <tr key={q.id} className="border-t border-line">
                  <td className="px-4 py-2 font-mono text-xs">{q.productModelId}</td>
                  <td className="px-4 py-2">{q.batch || t('qrs.noBatch')}</td>
                  <td className="px-4 py-2">{q.serialNumber || t('qrs.noSerial')}</td>
                  <td className="px-4 py-2">
                    <Tag tone={q.status === 'ACTIVE' ? 'ok' : 'warn'}>
                      {q.status === 'ACTIVE' ? t('qrs.active') : t('qrs.revoked')}
                    </Tag>
                  </td>
                  <td className="px-4 py-2 text-xs text-ink2">
                    {q.expiresAt ? new Date(q.expiresAt).toLocaleString() : '—'}
                  </td>
                  <td className="px-4 py-2">
                    <button
                      className="text-ai hover:underline"
                      onClick={() => {
                        setRevokeId(q.id);
                        setRevokeReason('');
                      }}
                    >
                      {t('qrs.revoke')}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal
        open={showNew}
        title={t('qrs.newTitle')}
        onClose={() => setShowNew(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setShowNew(false)}>
              {t('common.cancel')}
            </Button>
            <Button variant="ai" onClick={create}>
              {t('qrs.create')}
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('qrs.productModelId')}</label>
            <select
              className="w-full rounded border border-line px-3 py-2 text-sm"
              value={form.productModelId}
              onChange={(e) => void selectProduct(e.target.value)}
            >
              <option value="">{t('common.select')}</option>
              {products.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.displayName} · {p.model}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">Hardware revision</label>
            <select
              className="w-full rounded border border-line px-3 py-2 text-sm"
              value={form.productVariantId}
              disabled={!form.productModelId}
              onChange={(e) => setForm({ ...form, productVariantId: e.target.value })}
            >
              <option value="">Default model revision</option>
              {variants.map((variant) => (
                <option key={variant.id} value={variant.id}>
                  {variant.hardwareRevision || 'Unspecified'} · {variant.region}
                  {variant.sku ? ` · ${variant.sku}` : ''}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">Initial firmware version</label>
            <Input
              value={form.initialFirmwareVersion}
              onChange={(e) => setForm({ ...form, initialFirmwareVersion: e.target.value })}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('qrs.batch')}</label>
            <Input
              value={form.batch}
              onChange={(e) => setForm({ ...form, batch: e.target.value })}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('qrs.serial')}</label>
            <Input
              value={form.serialNumber}
              onChange={(e) => setForm({ ...form, serialNumber: e.target.value })}
            />
          </div>
        </div>
      </Modal>

      <Modal open={!!showToken} title={t('qrs.tokenTitle')} onClose={() => setShowToken(null)}>
        <div className="space-y-3">
          <p className="text-sm text-ink2">{t('qrs.tokenHint')}</p>
          <div className="break-all rounded-lg border border-line bg-slate-50 p-3 font-mono text-xs">
            {showToken?.token}
          </div>
          <Button variant="ai" onClick={() => showToken && copy(showToken.token, showToken.id)}>
            {copied === showToken?.id ? t('qrs.copied') : t('qrs.copyToken')}
          </Button>
        </div>
      </Modal>

      <Modal
        open={!!revokeId}
        title={t('qrs.revokeReason')}
        onClose={() => setRevokeId(null)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setRevokeId(null)}>
              {t('common.cancel')}
            </Button>
            <Button variant="danger" onClick={doRevoke}>
              {t('qrs.revoke')}
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('qrs.revokeReason')}</label>
            <Input value={revokeReason} onChange={(e) => setRevokeReason(e.target.value)} />
          </div>
        </div>
      </Modal>
    </div>
  );
}
