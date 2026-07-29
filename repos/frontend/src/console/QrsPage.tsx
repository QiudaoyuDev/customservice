import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Input, Modal, Card, PageHeader, StatCard, Tag, EmptyState } from '../components/ui';
import { listProducts } from '../lib/api';
import { listQrs, createQr, revokeQr } from '../lib/api';
import { regionLabel } from '../i18n';
import type { Product, Qr } from '../lib/types';

export default function QrsPage() {
  const { t } = useTranslation();
  const [qrs, setQrs] = useState<Qr[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [show, setShow] = useState(false);
  const [form, setForm] = useState({
    productId: '',
    region: 'EU',
    label: '',
    count: 1,
    expiresInDays: 365,
  });

  const load = async () => {
    setLoading(true);
    try {
      const [q, p] = await Promise.all([listQrs(), listProducts()]);
      setQrs(q);
      setProducts(p);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, []);

  const create = async () => {
    if (!form.productId) return;
    await createQr({
      productId: form.productId,
      region: form.region,
      label: form.label || undefined,
      count: form.count,
      expiresInDays: form.expiresInDays,
    });
    setShow(false);
    void load();
  };
  const revoke = async (id: string) => {
    await revokeQr(id);
    void load();
  };

  if (loading) return <div className="p-6 text-sm text-ink2">{t('common.loading')}</div>;

  const active = qrs.filter((q) => q.status === 'ACTIVE').length;
  const revoked = qrs.filter((q) => q.status === 'REVOKED').length;

  return (
    <div className="enter">
      <PageHeader
        title={t('qrs.title')}
        subtitle={t('qrs.subtitle')}
        icon="▣"
        actions={<Button variant="primary" onClick={() => setShow(true)}>{t('qrs.generate')}</Button>}
      />

      <div className="mb-5 grid grid-cols-3 gap-3">
        <StatCard label={t('qrs.title')} value={qrs.length} tone="brand" />
        <StatCard label={t('qrs.active')} value={active} tone="ok" />
        <StatCard label={t('qrs.revoked')} value={revoked} tone="safety" />
      </div>

      {qrs.length === 0 ? (
        <Card>
          <EmptyState title={t('qrs.empty')} hint={t('qrs.emptyHint')} action={<Button variant="primary" onClick={() => setShow(true)}>{t('qrs.generate')}</Button>} />
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {qrs.map((q) => (
            <Card key={q.id} interactive className="flex flex-col p-4">
              <div className="flex items-start justify-between">
                <div className="grid h-16 w-16 place-items-center rounded-xl bg-white text-brand-700 shadow-inner">
                  <svg width="42" height="42" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M3 3h8v8H3V3zm2 2v4h4V5H5zm8-2h8v8h-8V3zm2 2v4h4V5h-4zM3 13h8v8H3v-8zm2 2v4h4v-4H5zm8 0h2v2h-2v-2zm4 0h2v2h-2v-2zm-4 4h2v2h-2v-2zm4 0h2v2h-2v-2zm2-4h2v2h-2v-2zm0 4h2v2h-2v-2z" />
                  </svg>
                </div>
                <Tag tone={q.status === 'ACTIVE' ? 'ok' : q.status === 'REVOKED' ? 'safety' : 'mute'}>
                  {t(`qrStatus.${q.status}`)}
                </Tag>
              </div>
              <div className="mt-3 font-mono text-xs text-ink2">{q.code}</div>
              <div className="mt-1 text-sm font-semibold text-ink">{q.label || q.productName || '—'}</div>
              <div className="mt-1 text-xs text-ink2">
                {regionLabel(t, q.region)} · {q.link}
              </div>
              <div className="mt-1 text-xs text-ink3">
                {t('qrs.expires')}: {q.expiresAt?.slice(0, 10) ?? '—'}
              </div>
              {q.status === 'ACTIVE' && (
                <Button variant="ghost" size="sm" className="mt-3 self-start text-danger" onClick={() => revoke(q.id)}>
                  {t('qrs.revoke')}
                </Button>
              )}
            </Card>
          ))}
        </div>
      )}

      <Modal
        open={show}
        title={t('qrs.generate')}
        onClose={() => setShow(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setShow(false)}>
              {t('common.cancel')}
            </Button>
            <Button variant="primary" onClick={create}>
              {t('qrs.create')}
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('qrs.product')}</label>
            <select
              className="w-full rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-soft"
              value={form.productId}
              onChange={(e) => setForm({ ...form, productId: e.target.value })}
            >
              <option value="">{t('common.select')}</option>
              {products.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.displayName}
                </option>
              ))}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold text-ink2">{t('common.region')}</label>
              <select
                className="w-full rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-soft"
                value={form.region}
                onChange={(e) => setForm({ ...form, region: e.target.value })}
              >
                {['EU', 'NA', 'APAC', 'LATAM', 'MEA'].map((r) => (
                  <option key={r} value={r}>
                    {regionLabel(t, r)}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-ink2">{t('qrs.count')}</label>
              <Input
                type="number"
                min={1}
                value={form.count}
                onChange={(e) => setForm({ ...form, count: Math.max(1, Number(e.target.value) || 1) })}
              />
            </div>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('qrs.label')}</label>
            <Input value={form.label} onChange={(e) => setForm({ ...form, label: e.target.value })} placeholder={t('qrs.labelPh')} />
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('qrs.expiresInDays')}</label>
            <Input
              type="number"
              value={form.expiresInDays}
              onChange={(e) => setForm({ ...form, expiresInDays: Number(e.target.value) || 0 })}
            />
          </div>
        </div>
      </Modal>
    </div>
  );
}
