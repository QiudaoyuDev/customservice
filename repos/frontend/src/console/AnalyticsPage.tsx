import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Card, PageHeader, StatCard, EmptyState } from '../components/ui';
import { loadAnalytics } from '../lib/api';

export default function AnalyticsPage() {
  const { t } = useTranslation();
  const [analytics, setAnalytics] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        setAnalytics(await loadAnalytics());
      } catch {
        setAnalytics(null);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (loading) return <div className="p-6 text-sm text-ink2">{t('common.loading')}</div>;

  if (!analytics) {
    return (
      <div className="enter">
        <PageHeader title={t('analytics.title')} subtitle={t('analytics.subtitle')} icon="◷" />
        <Card>
          <EmptyState title={t('analytics.unavailable')} hint={t('analytics.retryHint')} />
        </Card>
      </div>
    );
  }

  const events: { type: string; count: number }[] = analytics.events ?? [];
  const totalEvents = events.reduce((s, e) => s + e.count, 0);
  const sum = (re: RegExp) =>
    events.filter((e) => re.test(e.type)).reduce((s, e) => s + e.count, 0);
  const answerCount = sum(/answer/i);
  const handoffCount = sum(/handoff/i);
  const max = Math.max(1, ...events.map((e) => e.count));
  const cat = (type: string) =>
    /handoff|human/i.test(type) ? 'human' : /safety|risk/i.test(type) ? 'safety' : 'ai';

  const labelFor = (type: string) => {
    const ty = type.toLowerCase();
    if (/answer/.test(ty)) return t('analytics.answers');
    if (/handoff/.test(ty)) return t('analytics.handoffs');
    if (/evidence/.test(ty)) return t('analytics.noEvidence');
    if (/conflict/.test(ty)) return t('analytics.conflicts');
    return type;
  };

  return (
    <div className="enter">
      <PageHeader title={t('analytics.title')} subtitle={t('analytics.subtitle')} icon="◷" />

      <div className="mb-5 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatCard label={t('analytics.events')} value={totalEvents} tone="brand" />
        <StatCard label={t('analytics.answers')} value={answerCount} tone="ai" />
        <StatCard label={t('analytics.handoffs')} value={handoffCount} tone="human" />
        <StatCard
          label={t('analytics.avgLatency')}
          value={analytics.avgLatencyMs != null ? `${analytics.avgLatencyMs} ms` : '—'}
          tone="ok"
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="p-4 lg:col-span-2">
          <div className="mb-3 text-xs font-bold uppercase tracking-wider text-ink2">
            {t('analytics.eventCounts')}
          </div>
          {events.length === 0 ? (
            <EmptyState title={t('analytics.retryHint')} />
          ) : (
            <div className="space-y-3">
              {events.map((e) => {
                const c = cat(e.type);
                const bar =
                  c === 'human'
                    ? 'bg-human-500'
                    : c === 'safety'
                      ? 'bg-safety'
                      : 'bg-ai-500';
                return (
                  <div key={e.type}>
                    <div className="mb-1 flex items-center justify-between text-sm">
                      <span className="font-medium text-ink">{labelFor(e.type)}</span>
                      <span className="font-mono text-xs text-ink2">{e.count}</span>
                    </div>
                    <div className="h-2 overflow-hidden rounded-full bg-slate-100">
                      <div
                        className={`h-full rounded-full ${bar} enter-pop`}
                        style={{ width: `${(e.count / max) * 100}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </Card>

        <Card className="p-4">
          <div className="mb-3 text-xs font-bold uppercase tracking-wider text-ink2">
            {t('analytics.title')}
          </div>
          <div className="space-y-2 text-sm">
            <Row label={t('analytics.noEvidence')} value={analytics.noEvidence ?? '—'} tone="warn" />
            <Row label={t('analytics.conflicts')} value={analytics.conflicts ?? '—'} tone="danger" />
            <div className="rounded-xl bg-brand-soft px-3 py-2 text-xs text-ink2">
              {t('analytics.avgLatency')}:{' '}
              <span className="font-mono font-semibold text-ink">
                {analytics.avgLatencyMs != null ? `${analytics.avgLatencyMs} ms` : '—'}
              </span>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}

function Row({
  label,
  value,
  tone,
}: {
  label: string;
  value: React.ReactNode;
  tone: 'warn' | 'danger';
}) {
  const cls = tone === 'warn' ? 'text-warn' : 'text-danger';
  return (
    <div className="flex items-center justify-between rounded-xl bg-slate-50 px-3 py-2">
      <span className="text-ink2">{label}</span>
      <span className={`font-mono font-semibold ${cls}`}>{value}</span>
    </div>
  );
}
