import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Button,
  Input,
  Modal,
  Textarea,
  Tag,
  StatusFlow,
  EmptyState,
  PageHeader,
  Table,
  Card,
  StatCard,
} from '../components/ui';
import {
  listProducts,
  createProduct,
  listProductModels,
  createProductModel,
  listProductVariants,
  createProductVariant,
  listFirmware,
  createFirmware,
} from '../lib/api';
import type { Product, ProductModel, ProductVariant, Firmware } from '../lib/types';

export default function ProductsPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [products, setProducts] = useState<Product[]>([]);
  const [models, setModels] = useState<ProductModel[]>([]);
  const [variants, setVariants] = useState<ProductVariant[]>([]);
  const [firmware, setFirmware] = useState<Firmware[]>([]);
  const [loading, setLoading] = useState(true);
  const [showProduct, setShowProduct] = useState(false);
  const [newName, setNewName] = useState('');
  const [newModel, setNewModel] = useState('');
  const [newModelName, setNewModelName] = useState('');
  const [showVariant, setShowVariant] = useState(false);
  const [variantProductId, setVariantProductId] = useState('');
  const [variantModelId, setVariantModelId] = useState('');
  const [variantName, setVariantName] = useState('');
  const [variantSku, setVariantSku] = useState('');
  const [showFirmware, setShowFirmware] = useState(false);
  const [fwProductModelId, setFwProductModelId] = useState('');
  const [fwVersion, setFwVersion] = useState('');
  const [fwNotes, setFwNotes] = useState('');

  const load = async () => {
    setLoading(true);
    try {
      const [p, m, v, f] = await Promise.all([
        listProducts(),
        listProductModels(),
        listProductVariants(),
        listFirmware(),
      ]);
      setProducts(p);
      setModels(m);
      setVariants(v);
      setFirmware(f);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, []);

  const createP = async () => {
    if (!newName.trim()) return;
    await createProduct({
      family: newName.trim(),
      model: newModel.trim(),
      displayName: newModelName.trim(),
      region: 'EU',
      status: 'draft',
    });
    setNewName('');
    setShowProduct(false);
    void load();
  };
  const createM = async () => {
    if (!newModel.trim() || !newModelName.trim()) return;
    await createProductModel({ model: newModel.trim(), name: newModelName.trim() });
    setNewModel('');
    setNewModelName('');
    void load();
  };
  const createV = async () => {
    if (!variantProductId || !variantModelId || !variantName.trim() || !variantSku.trim()) return;
    await createProductVariant({
      productId: variantProductId,
      productModelId: variantModelId,
      name: variantName.trim(),
      sku: variantSku.trim(),
    });
    setShowVariant(false);
    setVariantName('');
    setVariantSku('');
    void load();
  };
  const createF = async () => {
    if (!fwProductModelId || !fwVersion.trim()) return;
    await createFirmware({ productModelId: fwProductModelId, version: fwVersion.trim(), notes: fwNotes });
    setShowFirmware(false);
    setFwVersion('');
    setFwNotes('');
    void load();
  };

  if (loading) {
    return <div className="p-6 text-sm text-ink2">{t('common.loading')}</div>;
  }

  return (
    <div className="enter">
      <PageHeader
        title={t('products.title')}
        subtitle={t('products.subtitle')}
        icon="▦"
        actions={
          <>
            <Button variant="ai" onClick={() => setShowVariant(true)}>
              {t('products.addVariant')}
            </Button>
            <Button variant="ai" onClick={() => setShowFirmware(true)}>
              {t('products.addFirmware')}
            </Button>
            <Button variant="primary" onClick={() => setShowProduct(true)}>
              {t('products.add')}
            </Button>
          </>
        }
      />

      <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard label={t('products.title')} value={products.length} tone="brand" />
        <StatCard label={t('products.models')} value={models.length} tone="ai" />
        <StatCard label={t('products.variants')} value={variants.length} tone="human" />
        <StatCard label={t('products.firmware')} value={firmware.length} tone="ok" />
      </div>

      <Card className="overflow-hidden">
        <div className="px-4 pt-4">
          <h2 className="font-display text-base font-bold text-ink">{t('products.title')}</h2>
        </div>
        {products.length === 0 ? (
          <EmptyState title={t('products.empty')} hint={t('products.emptyHint')} />
        ) : (
          <Table
            header={[
              t('products.name'),
              t('products.model'),
              t('products.region'),
              t('products.firmware'),
              t('products.status'),
              t('products.actions'),
            ]}
          >
            {products.map((p) => (
              <tr key={p.id} className="border-t border-line transition hover:bg-brand-soft/40">
                <td className="px-4 py-3 font-medium text-ink">{p.displayName}</td>
                <td className="px-4 py-3 text-ink2">{p.model ?? '—'}</td>
                <td className="px-4 py-3 text-ink2">{p.region ?? '—'}</td>
                <td className="px-4 py-3 text-ink2">{p.firmwareMax ?? '—'}</td>
                <td className="px-4 py-3">
                  <StatusFlow status={p.status ?? 'draft'} />
                </td>
                <td className="px-4 py-3">
                  <Button variant="ghost" size="sm" onClick={() => navigate(`/console/documents?product=${p.id}`)}>
                    {t('products.actions')}
                  </Button>
                </td>
              </tr>
            ))}
          </Table>
        )}
      </Card>

      <Modal
        open={showProduct}
        title={t('products.add')}
        onClose={() => setShowProduct(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setShowProduct(false)}>
              {t('common.cancel')}
            </Button>
            <Button variant="primary" onClick={createP}>
              {t('common.create')}
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('products.name')}</label>
            <Input value={newName} onChange={(e) => setNewName(e.target.value)} placeholder={t('products.name')} />
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('products.model')}</label>
            <Input value={newModel} onChange={(e) => setNewModel(e.target.value)} placeholder="X100" />
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('products.modelName')}</label>
            <Input value={newModelName} onChange={(e) => setNewModelName(e.target.value)} placeholder={t('products.modelName')} />
          </div>
        </div>
      </Modal>

      <Modal
        open={showVariant}
        title={t('products.addVariant')}
        onClose={() => setShowVariant(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setShowVariant(false)}>
              {t('common.cancel')}
            </Button>
            <Button variant="ai" onClick={createV}>
              {t('common.create')}
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('products.product')}</label>
            <select
              className="w-full rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-soft"
              value={variantProductId}
              onChange={(e) => setVariantProductId(e.target.value)}
            >
              <option value="">{t('common.select')}</option>
              {products.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.displayName}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('products.model')}</label>
            <select
              className="w-full rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-soft"
              value={variantModelId}
              onChange={(e) => setVariantModelId(e.target.value)}
            >
              <option value="">{t('common.select')}</option>
              {models.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.model} · {m.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('products.variantName')}</label>
            <Input value={variantName} onChange={(e) => setVariantName(e.target.value)} />
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">SKU</label>
            <Input value={variantSku} onChange={(e) => setVariantSku(e.target.value)} />
          </div>
        </div>
      </Modal>

      <Modal
        open={showFirmware}
        title={t('products.addFirmware')}
        onClose={() => setShowFirmware(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setShowFirmware(false)}>
              {t('common.cancel')}
            </Button>
            <Button variant="ai" onClick={createF}>
              {t('common.create')}
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('products.model')}</label>
            <select
              className="w-full rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-soft"
              value={fwProductModelId}
              onChange={(e) => setFwProductModelId(e.target.value)}
            >
              <option value="">{t('common.select')}</option>
              {models.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.model} · {m.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('products.firmwareVersion')}</label>
            <Input value={fwVersion} onChange={(e) => setFwVersion(e.target.value)} placeholder="2.1.0" />
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('products.notes')}</label>
            <Textarea value={fwNotes} onChange={(e) => setFwNotes(e.target.value)} />
          </div>
        </div>
      </Modal>
    </div>
  );
}
