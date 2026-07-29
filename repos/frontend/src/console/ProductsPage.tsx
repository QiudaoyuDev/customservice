import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import { Button, Input, Textarea, Tag, Modal, StatusFlow, EmptyState } from '../components/ui';
import { useTranslation } from '../i18n';

export default function ProductsPage() {
  const { t } = useTranslation();
  const [rows, setRows] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showNew, setShowNew] = useState(false);
  const [form, setForm] = useState({ family: '', model: '', displayName: '', region: 'EU', hardwareVersion: '', firmwareMin: '', firmwareMax: '' });

  const load = async () => {
    setLoading(true);
    try {
      const list = await api('/products');
      setRows(list ?? []);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    load();
  }, []);

  const create = async () => {
    await api('/products', { method: 'POST', body: JSON.stringify(form) });
    setShowNew(false);
    setForm({ family: '', model: '', displayName: '', region: 'EU', hardwareVersion: '', firmwareMin: '', firmwareMax: '' });
    load();
  };

  const archive = async (id: string) => {
    await api(`/products/${id}/archive`, {method: 'POST'});
    load();
  };

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-ink">{t('products.title')}</h1>
          <p className="text-sm text-ink2">{t('products.subtitle')}</p>
        </div>
        <Button variant="ai" onClick={() => setShowNew(true)}>
          {t('products.new')}
        </Button>
      </div>

      {loading ? (
        <div className="text-sm text-ink2">{t('common.loading')}</div>
      ) : rows.length === 0 ? (
        <EmptyState title={t('products.emptyTitle')} hint={t('products.emptyHint')} />
      ) : (
        <div className="overflow-hidden rounded-xl border border-line bg-white">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-ink2">
              <tr>
                <th className="px-4 py-2 text-left">{t('products.family')}</th>
                <th className="px-4 py-2 text-left">{t('products.model')}</th>
                <th className="px-4 py-2 text-left">{t('products.displayName')}</th>
                <th className="px-4 py-2 text-left">{t('products.region')}</th>
                <th className="px-4 py-2 text-left">{t('products.hardwareVersion')}</th>
                <th className="px-4 py-2 text-left">{t('products.firmwareRange')}</th>
                <th className="px-4 py-2 text-left">{t('products.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((p) => (
                <tr key={p.id} className="border-t border-line">
                  <td className="px-4 py-2">{p.family}</td>
                  <td className="px-4 py-2">{p.model}</td>
                  <td className="px-4 py-2">{p.displayName}</td>
                  <td className="px-4 py-2">{p.region}</td>
                  <td className="px-4 py-2">{p.hardwareVersion || t('products.allHardware')}</td>
                  <td className="px-4 py-2">
                    {(p.firmwareMin || t('products.anyFirmware'))} ~ {p.firmwareMax || t('products.anyFirmware')}
                  </td>
                  <td className="px-4 py-2">
                    {p.status === 'ACTIVE' && <button className="text-xs text-red-600 hover:underline" onClick={() => archive(p.id)}>{t('products.archive')}</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal
        open={showNew}
        title={t('products.newTitle')}
        onClose={() => setShowNew(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setShowNew(false)}>
              {t('common.cancel')}
            </Button>
            <Button variant="ai" onClick={create}>
              {t('products.create')}
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('products.family')}</label>
            <Input value={form.family} onChange={(e) => setForm({ ...form, family: e.target.value })} />
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('products.model')}</label>
            <Input value={form.model} onChange={(e) => setForm({ ...form, model: e.target.value })} />
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('products.displayName')}</label>
            <Input value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} />
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('products.region')}</label>
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
            <label className="mb-1 block text-xs text-ink2">{t('products.hardwareVersion')}</label>
            <Input value={form.hardwareVersion} onChange={(e) => setForm({ ...form, hardwareVersion: e.target.value })} />
          </div>
          <div className="flex gap-2">
            <div className="flex-1">
              <label className="mb-1 block text-xs text-ink2">{t('products.firmwareMin')}</label>
              <Input value={form.firmwareMin} onChange={(e) => setForm({ ...form, firmwareMin: e.target.value })} />
            </div>
            <div className="flex-1">
              <label className="mb-1 block text-xs text-ink2">{t('products.firmwareMax')}</label>
              <Input value={form.firmwareMax} onChange={(e) => setForm({ ...form, firmwareMax: e.target.value })} />
            </div>
          </div>
        </div>
      </Modal>
    </div>
  );
}
