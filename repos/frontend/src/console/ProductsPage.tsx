import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import { Button, Input, Textarea, Tag, Modal, StatusFlow, EmptyState } from '../components/ui';
import { useTranslation } from '../i18n';

export default function ProductsPage() {
  const { t } = useTranslation();
  const [rows, setRows] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showNew, setShowNew] = useState(false);
  const [showVariants, setShowVariants] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<any>(null);
  const [variants, setVariants] = useState<any[]>([]);
  const [selectedVariant, setSelectedVariant] = useState<any>(null);
  const [firmware, setFirmware] = useState<any[]>([]);
  const [variantForm, setVariantForm] = useState({region: 'EU', hardwareRevision: '', sku: ''});
  const [firmwareForm, setFirmwareForm] = useState({version: '', releaseDate: '', checksum: '', notes: ''});
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

  const openVariants = async (product: any) => {
    setSelectedProduct(product);
    setSelectedVariant(null);
    setFirmware([]);
    setVariants(await api(`/products/${product.id}/variants`));
    setShowVariants(true);
  };

  const createVariant = async () => {
    if (!selectedProduct) return;
    const item = await api(`/products/${selectedProduct.id}/variants`, {method: 'POST', body: JSON.stringify(variantForm)});
    setVariantForm({region: selectedProduct.region, hardwareRevision: '', sku: ''});
    setVariants((items) => [item, ...items]);
  };

  const chooseVariant = async (variant: any) => {
    if (!selectedProduct) return;
    setSelectedVariant(variant);
    setFirmware(await api(`/products/${selectedProduct.id}/variants/${variant.id}/firmware`));
  };

  const createFirmware = async () => {
    if (!selectedProduct || !selectedVariant) return;
    const item = await api(`/products/${selectedProduct.id}/variants/${selectedVariant.id}/firmware`, {method: 'POST', body: JSON.stringify({...firmwareForm, releaseDate: firmwareForm.releaseDate || null})});
    setFirmware((items) => [item, ...items]);
    setFirmwareForm({version: '', releaseDate: '', checksum: '', notes: ''});
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
                    <button className="mr-3 text-xs text-ai hover:underline" onClick={() => void openVariants(p)}>Variants</button>
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

      <Modal open={showVariants} title={`${selectedProduct?.displayName ?? ''} · variants`} onClose={() => setShowVariants(false)}>
        <div className="space-y-4">
          <div className="rounded border border-line p-3">
            <div className="mb-2 text-sm font-medium text-ink">Add hardware revision</div>
            <div className="grid grid-cols-3 gap-2">
              <Input placeholder="Region" value={variantForm.region} onChange={(e) => setVariantForm({...variantForm, region: e.target.value})}/>
              <Input placeholder="Hardware revision" value={variantForm.hardwareRevision} onChange={(e) => setVariantForm({...variantForm, hardwareRevision: e.target.value})}/>
              <Input placeholder="SKU" value={variantForm.sku} onChange={(e) => setVariantForm({...variantForm, sku: e.target.value})}/>
            </div>
            <Button className="mt-2" variant="ai" onClick={() => void createVariant()}>Add variant</Button>
          </div>
          <div className="space-y-2">
            {variants.map((variant) => <button key={variant.id} onClick={() => void chooseVariant(variant)} className={`block w-full rounded border p-2 text-left text-sm ${selectedVariant?.id === variant.id ? 'border-ai bg-ai-soft' : 'border-line'}`}>
              {variant.hardwareRevision || 'Unspecified revision'} · {variant.region}{variant.sku ? ` · ${variant.sku}` : ''}
            </button>)}
          </div>
          {selectedVariant && <div className="rounded border border-line p-3">
            <div className="mb-2 text-sm font-medium text-ink">Firmware for {selectedVariant.hardwareRevision || selectedVariant.id}</div>
            <div className="grid grid-cols-2 gap-2">
              <Input placeholder="Version" value={firmwareForm.version} onChange={(e) => setFirmwareForm({...firmwareForm, version: e.target.value})}/>
              <Input type="date" value={firmwareForm.releaseDate} onChange={(e) => setFirmwareForm({...firmwareForm, releaseDate: e.target.value})}/>
              <Input placeholder="Checksum" value={firmwareForm.checksum} onChange={(e) => setFirmwareForm({...firmwareForm, checksum: e.target.value})}/>
              <Input placeholder="Notes" value={firmwareForm.notes} onChange={(e) => setFirmwareForm({...firmwareForm, notes: e.target.value})}/>
            </div>
            <Button className="mt-2" variant="ai" onClick={() => void createFirmware()}>Add firmware</Button>
            <div className="mt-3 space-y-1 text-sm text-ink2">{firmware.map((item) => <div key={item.id}>{item.version} · {item.status}</div>)}</div>
          </div>}
        </div>
      </Modal>
    </div>
  );
}
