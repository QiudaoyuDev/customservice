import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Card, PageHeader, Tag, EmptyState, Drawer, Avatar } from '../components/ui';
import { listHandoffs, claimHandoff, closeHandoff, addHandoffNote } from '../lib/api';
import type { Handoff } from '../lib/types';

const STATUS_TONE: Record<string, 'warn' | 'info' | 'ok'> = {
  new: 'warn',
  in_progress: 'info',
  closed: 'ok',
};
const COLUMNS = ['new', 'in_progress', 'closed'] as const;

export default function HandoffsPage() {
  const { t } = useTranslation();
  const [items, setItems] = useState<Handoff[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Handoff | null>(null);
  const [resolution, setResolution] = useState('');
  const [noteInput, setNoteInput] = useState('');

  const load = async () => {
    setLoading(true);
    try {
      setItems(await listHandoffs());
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, []);

  const claim = async (h: Handoff) => {
    await claimHandoff(h.id);
    setSelected({ ...h, status: 'in_progress', assigneeId: 'me' });
    void load();
  };
  const submit = async () => {
    if (!selected || !resolution) return;
    await closeHandoff(selected.id, resolution, noteInput || undefined);
    setSelected(null);
    setResolution('');
    setNoteInput('');
    void load();
  };
  const addNote = async () => {
    if (!selected || !noteInput.trim()) return;
    await addHandoffNote(selected.id, noteInput.trim());
    setNoteInput('');
    void load();
  };

  if (loading) return <div className="p-6 text-sm text-ink2">{t('common.loading')}</div>;

  return (
    <div className="enter">
      <PageHeader title={t('handoffs.title')} subtitle={t('handoffs.subtitle')} icon="🧑" />

      <div className="grid gap-4 md:grid-cols-3">
        {COLUMNS.map((col) => {
          const list = items.filter((h) => (h.status ?? 'new') === col);
          return (
            <div key={col} className="flex flex-col">
              <div className="mb-2 flex items-center gap-2 px-1">
                <Tag tone={STATUS_TONE[col]}>{t(`handoffs.status.${col}`)}</Tag>
                <span className="text-sm font-semibold text-ink2">{list.length}</span>
              </div>
              <div className="flex flex-col gap-3">
                {list.length === 0 ? (
                  <Card className="p-4">
                    <EmptyState title={t('handoffs.emptyTitle')} />
                  </Card>
                ) : (
                  list.map((h) => (
                    <Card
                      key={h.id}
                      interactive
                      className="cursor-pointer p-3"
                      onClick={() => setSelected(h)}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-mono text-xs text-ink2">
                          {t('handoffs.session', { id: (h.id ?? '').slice(0, 8) })}
                        </span>
                        {h.assigneeId && <Avatar name={h.assigneeId} size={22} />}
                      </div>
                      <div className="mt-2 line-clamp-2 text-sm font-medium text-ink">
                        {h.trigger ?? h.summary ?? '—'}
                      </div>
                      <div className="mt-1.5 flex flex-wrap items-center gap-1.5 text-xs text-ink2">
                        <Tag tone="mute">{h.region}</Tag>
                        {h.resolution && <Tag tone="ok">{t(`handoffs.resolution.${h.resolution}`)}</Tag>}
                      </div>
                      {h.status !== 'closed' && (
                        <Button
                          variant={h.status === 'new' ? 'ai' : 'ghost'}
                          size="sm"
                          className="mt-3 w-full"
                          onClick={(e) => {
                            e.stopPropagation();
                            void claim(h);
                          }}
                        >
                          {h.status === 'new' ? t('handoffs.claim') : t('handoffs.close')}
                        </Button>
                      )}
                    </Card>
                  ))
                )}
              </div>
            </div>
          );
        })}
      </div>

      <Drawer
        open={!!selected}
        title={selected ? t('handoffs.handleTitle') : ''}
        onClose={() => setSelected(null)}
      >
        {selected && (
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <Tag tone={STATUS_TONE[selected.status ?? 'new']}>{t(`handoffs.status.${selected.status ?? 'new'}`)}</Tag>
              <Tag tone="mute">{selected.region}</Tag>
            </div>

            <div>
              <div className="mb-1 text-xs font-semibold text-ink2">{t('handoffs.trigger')}</div>
              <div className="rounded-xl bg-slate-50 p-3 text-sm text-ink">{selected.trigger ?? selected.summary}</div>
            </div>

            <div>
              <div className="mb-1 text-xs font-semibold text-ink2">{t('handoffs.conversation')}</div>
              <div className="max-h-56 space-y-2 overflow-auto">
                {(selected.conversation ?? []).map((m: any, i: number) => (
                  <div
                    key={i}
                    className={
                      'rounded-xl border p-2 text-sm ' +
                      (m.role === 'user' ? 'border-line bg-white' : 'border-ai-100 bg-ai-soft')
                    }
                  >
                    <span className="mr-1 font-mono text-[10px] text-ink3">{m.role}</span>
                    {m.content}
                  </div>
                ))}
                {(!selected.conversation || selected.conversation.length === 0) && (
                  <div className="text-xs text-ink2">{t('handoffs.noNotesYet')}</div>
                )}
              </div>
            </div>

            <div>
              <div className="mb-1 text-xs font-semibold text-ink2">{t('handoffs.internalNotes')}</div>
              <div className="space-y-2">
                {(selected.notes ?? []).map((n: any, i: number) => (
                  <div key={i} className="rounded-lg bg-brand-soft px-3 py-2 text-sm text-ink">
                    {n.body}
                  </div>
                ))}
                <div className="flex gap-2">
                  <input
                    className="flex-1 rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-soft"
                    value={noteInput}
                    onChange={(e) => setNoteInput(e.target.value)}
                    placeholder={t('handoffs.addNotePlaceholder')}
                  />
                  <Button variant="ghost" size="sm" onClick={addNote}>
                    {t('handoffs.addNote')}
                  </Button>
                </div>
              </div>
            </div>

            {selected.status !== 'closed' && (
              <div>
                <div className="mb-1 text-xs font-semibold text-ink2">{t('handoffs.resolutionLabel')}</div>
                <select
                  className="w-full rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-soft"
                  value={resolution}
                  onChange={(e) => setResolution(e.target.value)}
                >
                  <option value="">{t('handoffs.selectResult')}</option>
                  {['resolved', 'waiting_parts', 'warranty', 'abandoned', 'duplicate', 'product_defect'].map((r) => (
                    <option key={r} value={r}>
                      {t(`handoffs.resolution.${r}`)}
                    </option>
                  ))}
                </select>
                <Button variant="primary" className="mt-3 w-full" disabled={!resolution} onClick={submit}>
                  {t('handoffs.submitResult')}
                </Button>
              </div>
            )}
          </div>
        )}
      </Drawer>
    </div>
  );
}
