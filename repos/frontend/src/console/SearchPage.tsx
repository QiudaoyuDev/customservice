import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Input, Card, PageHeader, Tag, EmptyState } from '../components/ui';
import { listProducts, searchV2 } from '../lib/api';
import type { Product } from '../lib/types';

const MODE_LABEL: Record<string, string> = { vector: 'vector', keyword: 'keyword', hybrid: 'hybrid' };

export default function SearchPage() {
  const { t } = useTranslation();
  const [query, setQuery] = useState('');
  const [productId, setProductId] = useState('');
  const [region, setRegion] = useState('EU');
  const [language, setLanguage] = useState('en');
  const [limit, setLimit] = useState(5);
  const [results, setResults] = useState<any[]>([]);
  const [searched, setSearched] = useState(false);
  const [searching, setSearching] = useState(false);
  const [products, setProducts] = useState<Product[]>([]);

  useEffect(() => {
    void listProducts().then(setProducts).catch(() => {});
  }, []);

  const handleSearch = async () => {
    setSearching(true);
    try {
      const r = await searchV2({ query, productModelId: productId || undefined, region, language, limit });
      setResults(r ?? []);
      setSearched(true);
    } finally {
      setSearching(false);
    }
  };

  return (
    <div className="enter">
      <PageHeader title={t('search.title')} subtitle={t('search.subtitle')} icon="⌕" />

      <Card className="mb-4 p-4">
        <div className="grid gap-3 md:grid-cols-2">
          <div className="md:col-span-2">
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('search.query')}</label>
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={t('search.placeholderQuery')}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('search.productId')}</label>
            <select
              className="w-full rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-soft"
              value={productId}
              onChange={(e) => setProductId(e.target.value)}
            >
              <option value="">{t('search.productPlaceholder')}</option>
              {products.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.displayName}
                </option>
              ))}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold text-ink2">{t('search.region')}</label>
              <select
                className="w-full rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-soft"
                value={region}
                onChange={(e) => setRegion(e.target.value)}
              >
                {['EU', 'NA', 'APAC', 'LATAM', 'MEA'].map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-ink2">{t('search.language')}</label>
              <select
                className="w-full rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-soft"
                value={language}
                onChange={(e) => setLanguage(e.target.value)}
              >
                <option value="zh-CN">简体中文</option>
                <option value="en">English</option>
                <option value="de-DE">Deutsch</option>
                <option value="fr-FR">Français</option>
                <option value="es-ES">Español</option>
              </select>
            </div>
          </div>
          <div className="flex items-end justify-between gap-3">
            <div className="w-32">
              <label className="mb-1 block text-xs font-semibold text-ink2">{t('search.limit')}</label>
              <Input
                type="number"
                min={1}
                max={20}
                value={limit}
                onChange={(e) => setLimit(Math.max(1, Number(e.target.value) || 1))}
              />
            </div>
            <Button variant="primary" onClick={handleSearch} disabled={searching || !query.trim()}>
              {searching ? t('search.searching') : t('search.run')}
            </Button>
          </div>
        </div>
      </Card>

      {!searched ? (
        <Card>
          <EmptyState title={t('search.empty')} />
        </Card>
      ) : results.length === 0 ? (
        <Card>
          <EmptyState title={t('search.noMatch')} />
        </Card>
      ) : (
        <div className="space-y-3">
          {results.map((r, i) => (
            <Card key={i} className="p-4">
              <div className="mb-2 flex items-center gap-2">
                <Tag tone="ai">{MODE_LABEL[r.mode] ?? r.mode}</Tag>
                <Tag tone="mute">{t('search.score')}: {(r.score ?? 0).toFixed(2)}</Tag>
                <span className="text-xs text-ink3">
                  {t('search.sources')} {r.sourceRefs?.length ? r.sourceRefs.join(', ') : t('search.noSource')}
                </span>
              </div>
              <div className="whitespace-pre-wrap text-sm text-ink">{r.text}</div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
