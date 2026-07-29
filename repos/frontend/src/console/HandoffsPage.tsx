import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import { Button, Tag, EmptyState, Modal, Textarea } from '../components/ui';
import { useTranslation } from '../i18n';

export default function HandoffsPage() {
  const { t } = useTranslation();
  const [rows, setRows] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [sel, setSel] = useState<any>(null);
  const [resolution, setResolution] = useState('');
  const [notes, setNotes] = useState<any[]>([]);
  const [note, setNote] = useState('');

  const statusLabel = (s: string) =>
    ({
      new: t('handoffs.status.new'),
      in_progress: t('handoffs.status.in_progress'),
      closed: t('handoffs.status.closed'),
    })[s] ?? s;
  const resolutionLabel = (k: string) => t('handoffs.resolution.' + k.toLowerCase());

  const load = async () => {
    setLoading(true);
    try {
      const list = await api('/handoffs');
      setRows(list ?? []);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    load();
  }, []);
  useEffect(() => {
    if (!sel) {
      setNotes([]);
      return;
    }
    api(`/handoffs/${sel.id}/notes`)
      .then((items) => setNotes(items ?? []))
      .catch(() => setNotes([]));
  }, [sel]);

  const claim = async (id: string) => {
    await api(`/handoffs/${id}/claim`, { method: 'POST' });
    load();
  };
  const closeCase = async () => {
    if (!sel) return;
    await api(`/handoffs/${sel.id}/close`, {
      method: 'POST',
      body: JSON.stringify({ resolution }),
    });
    setSel(null);
    setResolution('');
    load();
  };
  const addNote = async () => {
    if (!sel || !note.trim()) return;
    const created = await api(`/handoffs/${sel.id}/notes`, {
      method: 'POST',
      body: JSON.stringify({ content: note.trim() }),
    });
    setNotes((items) => [...items, created]);
    setNote('');
  };

  return (
    <div>
      <div className="mb-4">
        <h1 className="text-xl font-bold text-ink">{t('handoffs.title')}</h1>
        <p className="text-sm text-ink2">{t('handoffs.subtitle')}</p>
      </div>

      {loading ? (
        <div className="text-sm text-ink2">{t('common.loading')}</div>
      ) : rows.length === 0 ? (
        <EmptyState title={t('handoffs.emptyTitle')} hint={t('handoffs.emptyHint')} />
      ) : (
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
          {rows.map((h) => (
            <div key={h.id} className="rounded-xl border border-line bg-white p-4">
              <div className="mb-2 flex items-center justify-between">
                <span className="font-mono text-xs text-ink2">#{h.id.slice(0, 8)}</span>
                <Tag
                  tone={h.status === 'CLOSED' ? 'mute' : h.status === 'IN_PROGRESS' ? 'ai' : 'warn'}
                >
                  {statusLabel(h.status.toLowerCase())}
                </Tag>
              </div>
              <div className="text-sm text-ink">
                {h.summary || t('handoffs.session', { id: h.conversationId })}
              </div>
              <div className="mt-1 text-xs text-ink2">
                {t('handoffs.trigger')}：{h.reason}
              </div>
              <div className="mt-2 flex gap-2">
                {h.status !== 'CLOSED' && (
                  <Button
                    size="sm"
                    variant={h.status === 'NEW' ? 'ai' : 'ghost'}
                    disabled={h.status !== 'NEW'}
                    onClick={() => claim(h.id)}
                  >
                    {t('handoffs.claim')}
                  </Button>
                )}
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => {
                    setSel(h);
                    setResolution('');
                  }}
                >
                  {t('handoffs.close')}
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="mt-6 rounded-xl border border-line bg-white p-4">
        <h2 className="mb-2 text-sm font-bold text-ink">{t('handoffs.helpTitle')}</h2>
        <ul className="list-disc space-y-1 pl-5 text-xs text-ink2">
          <li>{t('handoffs.helpResolved')}</li>
          <li>{t('handoffs.helpWaitingParts')}</li>
          <li>{t('handoffs.helpWarranty')}</li>
          <li>{t('handoffs.helpAbandoned')}</li>
          <li>{t('handoffs.helpDuplicate')}</li>
          <li>{t('handoffs.helpDefect')}</li>
        </ul>
      </div>

      <Modal
        open={!!sel}
        title={t('handoffs.handleTitle')}
        onClose={() => setSel(null)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setSel(null)}>
              {t('handoffs.close')}
            </Button>
            <Button variant="ai" onClick={closeCase} disabled={!resolution}>
              {t('handoffs.submitResult')}
            </Button>
          </>
        }
      >
        <div className="space-y-3 text-sm">
          <div>
            <span className="text-ink2">{t('handoffs.conversation')}：</span>
            {sel?.conversationId}
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('handoffs.internalNotes')}</label>
            <div className="mb-2 max-h-28 space-y-1 overflow-y-auto rounded border border-line p-2 text-xs">
              {notes.length === 0 ? (
                <span className="text-ink2">{t('handoffs.noNotesYet')}</span>
              ) : (
                notes.map((item) => <div key={item.id}>{item.content}</div>)
              )}
            </div>
            <Textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder={t('handoffs.addNotePlaceholder')}
            />
            <Button size="sm" variant="ghost" onClick={addNote} disabled={!note.trim()}>
              {t('handoffs.addNote')}
            </Button>
          </div>
          <div>
            <span className="text-ink2">{t('handoffs.trigger')}：</span>
            {sel?.reason}
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('handoffs.resolutionLabel')}</label>
            <select
              className="w-full rounded border border-line px-3 py-2 text-sm"
              value={resolution}
              onChange={(e) => setResolution(e.target.value)}
            >
              <option value="">{t('handoffs.selectResult')}</option>
              {Object.keys(resolutionLabelMap).map((k) => (
                <option key={k} value={k}>
                  {resolutionLabel(k)}
                </option>
              ))}
            </select>
          </div>
        </div>
      </Modal>
    </div>
  );
}

const resolutionLabelMap: Record<string, string> = {
  RESOLVED: 'resolved',
  WAITING_PARTS: 'waiting_parts',
  WARRANTY: 'warranty',
  ABANDONED: 'abandoned',
  DUPLICATE: 'duplicate',
  PRODUCT_DEFECT: 'product_defect',
};
