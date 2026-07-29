import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import { EmptyState, Tag } from '../components/ui';

/** Minimal operational dashboard backed by the tenant-scoped analytics overview API. */
export default function AnalyticsPage() {
  const [data, setData] = useState<any>(null);
  useEffect(() => {
    api('/analytics/overview')
      .then(setData)
      .catch(() => setData({ failed: true }));
  }, []);
  if (!data) return <div className="text-sm text-ink2">Loading analytics…</div>;
  if (data.failed)
    return (
      <EmptyState
        title="Analytics unavailable"
        hint="Try again after operational events are recorded."
      />
    );
  const cards = [
    ['Events', data.totalEvents],
    ['Answers', data.answers],
    ['Handoffs', data.handoffs],
    ['No evidence', data.noEvidence],
    ['Conflicts', data.conflicts],
    ['Avg. answer latency', `${Math.round(data.averageAnswerLatencyMs)} ms`],
  ];
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-bold text-ink">Operational analytics</h1>
        <p className="text-sm text-ink2">Privacy-safe activity for the last {data.days} days.</p>
      </div>
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">
        {cards.map(([label, value]) => (
          <div key={String(label)} className="rounded-xl border border-line bg-white p-4">
            <div className="text-xs text-ink2">{label}</div>
            <div className="mt-1 text-2xl font-bold text-ink">{value}</div>
          </div>
        ))}
      </div>
      <div className="rounded-xl border border-line bg-white p-4">
        <h2 className="mb-3 text-sm font-bold text-ink">Event counts</h2>
        <div className="flex flex-wrap gap-2">
          {Object.entries(data.eventCounts ?? {}).map(([type, count]) => (
            <Tag key={type} tone="mute">
              {type}: {String(count)}
            </Tag>
          ))}
        </div>
      </div>
    </div>
  );
}
