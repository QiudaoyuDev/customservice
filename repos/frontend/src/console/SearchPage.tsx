import { useState } from 'react';
import { api } from '../lib/api';
import { Button, Input, Tag } from '../components/ui';
import { useTranslation, LANGS } from '../i18n';

export default function SearchPage() {
  const { t } = useTranslation();
  const [productModelId, setProductModelId] = useState('');
  const [region, setRegion] = useState('EU');
  const [language, setLanguage] = useState('en-US');
  const [query, setQuery] = useState('');
  const [limit, setLimit] = useState(8);
  const [result, setResult] = useState<any>(null);
  const [busy, setBusy] = useState(false);

  const run = async () => {
    if (!query.trim()) return;
    setBusy(true);
    try {
      const r = await api('/search/run', {
        method: 'POST',
        body: JSON.stringify({ productModelId: productModelId || null, region, language, query, limit }),
      });
      setResult(r);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <div className="mb-4">
        <h1 className="text-xl font-bold text-ink">{t('search.title')}</h1>
        <p className="text-sm text-ink2">{t('search.subtitle')}</p>
      </div>

      <div className="space-y-3 rounded-xl border border-line bg-white p-4">
        <div>
          <label className="mb-1 block text-xs text-ink2">{t('search.query')}</label>
          <Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder={t('search.placeholderQuery')} />
        </div>
        <div className="grid grid-cols-3 gap-3">
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('search.productId')}</label>
            <Input value={productModelId} onChange={(e) => setProductModelId(e.target.value)} placeholder={t('search.productPlaceholder')} />
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('search.region')}</label>
            <select className="w-full rounded border border-line px-3 py-2 text-sm" value={region} onChange={(e) => setRegion(e.target.value)}>
              {['EU', 'NA', 'APAC', 'LATAM', 'MEA'].map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('search.language')}</label>
            <select className="w-full rounded border border-line px-3 py-2 text-sm" value={language} onChange={(e) => setLanguage(e.target.value)}>
              {LANGS.map((l) => (
                <option key={l} value={l}>
                  {l}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <label className="text-xs text-ink2">{t('search.limit')}</label>
          <input
            type="number"
            min={1}
            max={50}
            value={limit}
            onChange={(e) => setLimit(Number(e.target.value))}
            className="w-20 rounded border border-line px-2 py-1 text-sm"
          />
          <Button variant="ai" onClick={run} disabled={busy || !query.trim()}>
            {busy ? t('search.searching') : t('search.run')}
          </Button>
        </div>
      </div>

      <div className="mt-4">
        {!result ? (
          <div className="rounded-xl border border-dashed border-line bg-white/60 p-8 text-center text-sm text-ink2">{t('search.empty')}</div>
        ) : (
          <div className="space-y-2">
            {result.results?.length === 0 ? (
              <div className="rounded-xl border border-dashed border-line bg-white/60 p-8 text-center text-sm text-ink2">{t('search.noMatch')}</div>
            ) : (
              result.results?.map((c: any, i: number) => (
                <div key={i} className="rounded-xl border border-line bg-white p-3">
                  <div className="mb-1 flex items-center gap-2 text-xs">
                    <Tag tone={c.mode === 'VECTOR' ? 'ai' : 'warn'}>{c.mode === 'VECTOR' ? t('search.vector') : t('search.keyword')}</Tag>
                    <span className="text-ink2">
                      {t('search.score')} {c.score?.toFixed?.(3)}
                    </span>
                    <span className="text-ink2">· {c.docTitle}</span>
                  </div>
                  <div className="whitespace-pre-wrap text-sm text-ink">{c.text}</div>
                  <div className="mt-1 text-xs text-ink2">
                    {t('search.sources')}
                    {c.sourceRef || t('search.noSource')}
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </div>
    </div>
  );
}
