import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import clsx from 'clsx';
import { useAuth } from '../lib/auth';
import { postAnswer, pub, pubUpload, streamAnswer } from '../lib/api';
import { Button, Input, Modal, Textarea, Tag, Banner, DiagnosisRail } from '../components/ui';
import { langNames, LANGS, regionLabel, useTranslation } from '../i18n';

function StepCard({
  m,
  onReply,
  t,
}: {
  m: any;
  onReply: (v: string) => void;
  t: (k: string, p?: any) => string;
}) {
  const detail = m.flowControl ?? {};
  const sources: string[] = m.citations ?? [];
  const expected = m.expectedInput;
  const isLast = detail.end;
  const risk = m.risk?.toLowerCase();
  const yesNo = [
    { label: t('common.yes'), value: 'yes', tone: 'ok' as const },
    { label: t('common.no'), value: 'no', tone: 'bad' as const },
    { label: t('common.unknown'), value: 'unknown', tone: 'mute' as const },
  ];
  const resolved = [
    { label: t('common.resolved'), value: 'resolved', tone: 'ok' as const },
    { label: t('common.unresolved'), value: 'unresolved', tone: 'warn' as const },
  ];
  const toneClass = (tone: string) =>
    tone === 'ok'
      ? 'border-ok/40 bg-ok-bg text-ok hover:bg-ok/20'
      : tone === 'bad'
        ? 'border-danger/40 bg-danger-bg text-danger hover:bg-danger/20'
        : tone === 'warn'
          ? 'border-warn/40 bg-warn-bg text-warn hover:bg-warn/20'
          : 'border-line bg-slate-50 text-ink2 hover:bg-brand-soft';
  return (
    <div
      className={clsx(
        'rounded-2xl border p-4',
        risk === 'high' ? 'border-safety bg-safety-bg/60' : 'border-ai-100 bg-gradient-to-b from-ai-soft to-white',
      )}
    >
      {risk === 'high' && (
        <div className="mb-2 inline-flex items-center gap-1 rounded-md bg-safety px-2 py-0.5 text-xs font-bold text-white">
          ⚠ {t('support.highRiskStep')}
        </div>
      )}
      <div className="mb-1 flex items-center gap-1.5 text-sm font-bold text-ink">
        <span className="text-ai-600">✦</span>
        {t('support.stepLabel')}
      </div>
      <div className="whitespace-pre-wrap text-[15px] leading-relaxed text-ink">{m.content}</div>
      {sources.length > 0 && (
        <div className="mt-2 text-xs text-ink2">
          {t('support.sources')}
          {sources.map((s, i) => (
            <span key={i} className="ml-2 inline-flex rounded bg-ai-soft px-2 py-0.5 font-mono text-ai-600">
              {s}
            </span>
          ))}
        </div>
      )}
      {expected === 'yes_no' && (
        <div className="mt-3 flex flex-wrap gap-2">
          {yesNo.slice(0, 2).map((r) => (
            <button
              key={r.value}
              onClick={() => onReply(r.value)}
              className={clsx('rounded-xl border px-3 py-1.5 text-sm font-semibold', toneClass(r.tone))}
            >
              {r.label}
            </button>
          ))}
        </div>
      )}
      {expected === 'resolved_unresolved' && (
        <div className="mt-3 flex flex-wrap gap-2">
          {resolved.map((r) => (
            <button
              key={r.value}
              onClick={() => onReply(r.value)}
              className={clsx('rounded-xl border px-3 py-1.5 text-sm font-semibold', toneClass(r.tone))}
            >
              {r.label}
            </button>
          ))}
        </div>
      )}
      {expected === 'yes_no_unknown' && (
        <div className="mt-3 flex flex-wrap gap-2">
          {yesNo.map((r) => (
            <button
              key={r.value}
              onClick={() => onReply(r.value)}
              className={clsx('rounded-xl border px-3 py-1.5 text-sm font-semibold', toneClass(r.tone))}
            >
              {r.label}
            </button>
          ))}
        </div>
      )}
      {isLast && <div className="mt-2 text-xs font-medium text-ink2">{t('support.lastStep')}</div>}
    </div>
  );
}

export default function SupportPage() {
  const { qrToken } = useParams();
  const navigate = useNavigate();
  const { logout, isAuthenticated } = useAuth();
  const { t, i18n } = useTranslation();
  const [language, setLanguage] = useState(i18n.language);
  const [convId, setConvId] = useState('');
  const [conversationToken, setConversationToken] = useState('');
  const [region, setRegion] = useState('');
  const [messages, setMessages] = useState<any[]>([]);
  const [input, setInput] = useState('');
  const [notices, setNotices] = useState<string[]>([]);
  const [notice, setNotice] = useState(t('support.identifying'));
  const [busy, setBusy] = useState(false);
  const [showInfo, setShowInfo] = useState(false);
  const [showHandoff, setShowHandoff] = useState(false);
  const [handoffReason, setHandoffReason] = useState('');
  const [handoffContact, setHandoffContact] = useState('');
  const [handoffAgree, setHandoffAgree] = useState(false);
  const [handoffId, setHandoffId] = useState('');
  const [showChange, setShowChange] = useState(false);
  const [changeProductId, setChangeProductId] = useState('');
  const [productOptions, setProductOptions] = useState<
    Array<{ id: string; displayName: string; model: string; hardwareVersion?: string }>
  >([]);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [replays, setReplays] = useState<any[]>([]);
  const scrollRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setConvId('');
      setConversationToken('');
      setMessages([]);
      setNotice(t('support.identifying'));
      try {
        const item = await pub('/conversations', {
          method: 'POST',
          body: JSON.stringify({ qrToken, language }),
        });
        if (cancelled) return;
        setConvId(item.id);
        setConversationToken(item.conversationAccessToken);
        setRegion(item.region ?? '');
        setNotice(t('support.identified'));
      } catch (e) {
        if (!cancelled) setNotice((e as Error).message);
      }
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [qrToken, language]);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, notices, notice]);

  const send = async () => {
    const text = input.trim();
    if (!text || !convId || busy) return;
    setInput('');
    setBusy(true);
    setMessages((m) => [
      ...m,
      { role: 'user', content: text, createdAt: new Date().toISOString() },
    ]);
    const loadingId = Date.now();
    setMessages((m) => [
      ...m,
      {
        id: loadingId,
        role: 'assistant',
        loading: true,
        createdAt: new Date().toISOString(),
      },
    ]);
    try {
      await pub(`/conversations/${convId}/messages`, {
        method: 'POST',
        headers: { 'X-Conversation-Token': conversationToken },
        body: JSON.stringify({ content: text }),
      });
      await requestAnswer(loadingId);
    } catch (e) {
      setMessages((m) => m.filter((x) => x.id !== loadingId));
      setNotice(t('support.sendFailed', { msg: (e as Error).message }));
    } finally {
      setBusy(false);
    }
  };

  const requestAnswer = async (loadingId: number) => {
    try {
      const partial: any = {
        id: loadingId,
        role: 'assistant',
        loading: false,
        content: '',
        citations: [],
      };
      await streamAnswer(
        convId,
        ({ event, data }) => {
          if (event === 'meta') partial.intent = data.intent;
          if (event === 'delta') partial.content += data.content ?? '';
          if (event === 'citations') partial.citations = data ?? [];
          if (event === 'done') partial.intent = data.intent ?? partial.intent;
          if (event === 'error') throw new Error(data.code ?? 'ANSWER_FAILED');
          if (event === 'meta' || event === 'delta' || event === 'citations' || event === 'done')
            setMessages((messages) =>
              messages.map((message) => (message.id === loadingId ? { ...partial } : message)),
            );
        },
        conversationToken,
      );
    } catch {
      const answer = await postAnswer(convId, conversationToken);
      setMessages((m) =>
        m.filter((x) => x.id !== loadingId).concat({ ...answer, role: 'assistant' }),
      );
    }
  };

  const sendImage = async (file: File) => {
    if (!convId || busy) return;
    setBusy(true);
    const loadingId = Date.now();
    setMessages((m) => [
      ...m,
      {
        id: loadingId,
        role: 'assistant',
        loading: true,
        createdAt: new Date().toISOString(),
      },
    ]);
    try {
      const data = new FormData();
      data.append('file', file);
      data.append('content', input.trim() || t('support.image'));
      await pubUpload(`/conversations/${convId}/attachments`, data, conversationToken);
      await requestAnswer(loadingId);
    } catch (e) {
      setMessages((m) => m.filter((x) => x.id !== loadingId));
      setNotice(t('support.imageFailed', { msg: (e as Error).message }));
    } finally {
      setBusy(false);
    }
  };

  const reply = async (v: string) => {
    if (!convId || busy) return;
    setBusy(true);
    setMessages((m) => [...m, { role: 'user', content: v, createdAt: new Date().toISOString() }]);
    const loadingId = Date.now();
    setMessages((m) => [
      ...m,
      {
        id: loadingId,
        role: 'assistant',
        loading: true,
        createdAt: new Date().toISOString(),
      },
    ]);
    try {
      const controlledReply =
        v === 'yes' || v === 'resolved'
          ? 'YES'
          : v === 'no' || v === 'unresolved'
            ? 'NO'
            : v === 'unknown'
              ? 'UNKNOWN'
              : undefined;
      await pub(`/conversations/${convId}/messages`, {
        method: 'POST',
        headers: { 'X-Conversation-Token': conversationToken },
        body: JSON.stringify({ content: v, controlledReply }),
      });
      await requestAnswer(loadingId);
    } catch (e) {
      setMessages((m) => m.filter((x) => x.id !== loadingId));
      setNotice(t('support.sendFailed', { msg: (e as Error).message }));
    } finally {
      setBusy(false);
    }
  };

  const submitHandoff = async () => {
    if (!convId || !handoffAgree) return;
    try {
      const item = await pub('/handoffs', {
        method: 'POST',
        headers: { 'X-Conversation-Token': conversationToken },
        body: JSON.stringify({
          conversationId: convId,
          idempotencyKey: crypto.randomUUID(),
          reason: handoffReason || 'user-request',
          summary: handoffReason || 'User requested human support',
          contact: handoffContact || null,
          contactAuthorized: handoffAgree,
        }),
      });
      setHandoffId(item.id ?? '');
      setNotice(t('support.handoffCreated', { id: item.id ?? '' }));
    } catch (e) {
      setNotice(t('support.submitFailed', { msg: (e as Error).message }));
    }
  };

  const changeProduct = async () => {
    if (!convId || !changeProductId.trim()) return;
    try {
      await pub(`/conversations/${convId}/product-context`, {
        method: 'POST',
        headers: { 'X-Conversation-Token': conversationToken },
        body: JSON.stringify({ productModelId: changeProductId }),
      });
      setShowChange(false);
      setChangeProductId('');
      setMessages([]);
      setNotice(t('support.identified'));
    } catch (e) {
      setNotice(t('support.changeFailed', { msg: (e as Error).message }));
    }
  };

  const openProductSelection = async () => {
    if (!convId) return;
    try {
      const options = await pub(`/conversations/${convId}/product-options`, {
        headers: { 'X-Conversation-Token': conversationToken },
      });
      setProductOptions(options ?? []);
      setChangeProductId('');
      setShowChange(true);
    } catch (e) {
      setNotice((e as Error).message);
    }
  };

  const feedback = (positive: boolean) => {
    if (!convId) return;
    pub(`/conversations/${convId}/feedback`, {
      method: 'POST',
      headers: { 'X-Conversation-Token': conversationToken },
      body: JSON.stringify({ resolved: positive, comment: '' }),
    }).catch(() => {});
    setNotice(positive ? t('support.feedbackThanks') : t('support.feedbackRecorded'));
  };

  const loadReplays = async () => {
    if (!convId) return;
    try {
      const list = await pub(`/conversations/${convId}/messages`, {
        headers: { 'X-Conversation-Token': conversationToken },
      });
      setReplays(list ?? []);
      setHistoryOpen(true);
    } catch {
      /* ignore */
    }
  };

  const renderMsg = (m: any) => {
    if (m.loading)
      return (
        <div className="flex items-center gap-2 rounded-2xl border border-line bg-white p-4 text-sm text-ink2">
          <span className="h-4 w-4 animate-spin rounded-full border-2 border-ai-100 border-t-ai-500" />
          {t('support.analyzing')}
        </div>
      );
    const intent = m.intent;
    const c = m.content ?? '';
    if (intent === 'SAFETY_STOP')
      return (
        <div className="flex gap-3 rounded-2xl border border-safety bg-safety-bg p-4">
          <div className="grid h-8 w-8 flex-none place-items-center rounded-xl bg-safety text-white shadow-[0_0_0_4px] shadow-safety-bg">
            ⚠
          </div>
          <div>
            <div className="font-bold text-safety">{t('support.safetyTitle')}</div>
            <div className="mt-1 whitespace-pre-wrap text-[15px] text-ink">{c}</div>
          </div>
        </div>
      );
    if (intent === 'HUMAN_REQUEST')
      return (
        <div className="rounded-2xl border border-human-500 bg-human-soft p-4">
          <div className="flex items-center gap-1.5 font-bold text-human-600">
            <span>🧑</span>
            {t('support.humanTitle')}
          </div>
          <div className="mt-1 whitespace-pre-wrap text-[15px] text-ink">{c}</div>
        </div>
      );
    if (intent === 'TROUBLESHOOTING') {
      if (m.flowControl) return <StepCard m={m} onReply={reply} t={t} />;
      return (
        <div className="rounded-2xl border border-line bg-white p-4">
          <div className="mb-1 flex items-center gap-1.5 text-sm font-bold text-ink">
            <span className="text-ai-600">✦</span>
            {t('support.stepLabel')}
          </div>
          <div className="whitespace-pre-wrap text-[15px] text-ink">{c}</div>
        </div>
      );
    }
    if (intent === 'INFO')
      return (
        <div className="rounded-2xl border border-line bg-white p-4 text-[15px] text-ink">
          {t('support.demoSession')}
          {c}
        </div>
      );
    return (
      <div className="rounded-2xl border border-line bg-white p-4">
        <div className="mb-1 flex items-center gap-1.5 text-xs font-bold text-ai-600">
          <span>✦</span> AI
        </div>
        <div className="whitespace-pre-wrap text-[15px] text-ink">{c}</div>
      </div>
    );
  };

  const flowMsgs = messages.filter((m) => m.intent === 'TROUBLESHOOTING' && m.flowControl);
  const railSteps = flowMsgs.map((m, k) => {
    const risk = m.risk?.toLowerCase() === 'high';
    const state: 'done' | 'current' | 'risk' =
      k < flowMsgs.length - 1 ? 'done' : risk ? 'risk' : 'current';
    return {
      key: m.flowControl?.nodeKey ?? `STEP ${k + 1}`,
      label: (m.content ?? '').slice(0, 32),
      state,
    };
  });
  const flowStep = flowMsgs.length;
  const totalStep =
    [...messages].reverse().find((m: any) => m.flowControl)?.flowControl?.totalSteps ?? null;

  const onLangChange = (l: string) => {
    setLanguage(l);
    i18n.changeLanguage(l);
  };

  return (
    <div className="flex h-screen flex-col">
      <header className="glass sticky top-0 z-20 flex flex-wrap items-center gap-3 border-b border-line bg-white px-4 py-2.5">
        <div className="flex items-center gap-2">
          <div className="grid h-7 w-7 place-items-center rounded-lg bg-gradient-to-br from-brand-700 to-ai-500 font-display text-xs font-extrabold text-white">
            H
          </div>
          <span className="font-display text-sm font-bold text-ink">HARDWARE AI</span>
          <Tag tone="ai">{t('support.chatTitle', { id: convId.slice(0, 8) })}</Tag>
          <Tag tone="mute">
            {regionLabel(t, region)} · {langNames[language] ?? language}
          </Tag>
        </div>
        <div className="ml-auto flex flex-wrap items-center gap-2">
          <select
            className="h-8 rounded-lg border border-line bg-white px-2 text-sm text-ink"
            value={region}
            onChange={(e) => setRegion(e.target.value)}
            aria-label={t('support.region')}
          >
            {['EU', 'NA', 'APAC', 'LATAM', 'MEA'].map((r) => (
              <option key={r} value={r}>
                {regionLabel(t, r)}
              </option>
            ))}
          </select>
          <select
            className="h-8 rounded-lg border border-line bg-white px-2 text-sm text-ink"
            value={language}
            onChange={(e) => onLangChange(e.target.value)}
            aria-label={t('support.language')}
          >
            {LANGS.map((l) => (
              <option key={l} value={l}>
                {langNames[l] ?? l}
              </option>
            ))}
          </select>
          <button
            className="h-8 rounded-lg border border-line px-2.5 text-xs font-semibold text-ink2 transition hover:bg-brand-soft hover:text-brand-700"
            onClick={() => void openProductSelection()}
          >
            {t('support.changeProduct')}
          </button>
          {isAuthenticated ? (
            <>
              <button
                className="h-8 rounded-lg border border-line px-2.5 text-xs font-semibold text-ink2 transition hover:bg-brand-soft hover:text-brand-700"
                onClick={() => navigate('/console')}
              >
                {t('support.console')}
              </button>
              <button
                className="h-8 rounded-lg border border-line px-2.5 text-xs font-semibold text-ink2 transition hover:bg-brand-soft hover:text-brand-700"
                onClick={() => {
                  logout();
                  navigate('/');
                }}
              >
                {t('console.logout')}
              </button>
            </>
          ) : (
            <button
              className="h-8 rounded-lg border border-line px-2.5 text-xs font-semibold text-ink2 transition hover:bg-brand-soft hover:text-brand-700"
              onClick={() => navigate('/login')}
            >
              {t('login.submit')}
            </button>
          )}
          <button
            className="grid h-8 w-8 place-items-center rounded-lg border border-line text-ink2 transition hover:bg-brand-soft hover:text-brand-700"
            onClick={() => setShowInfo(true)}
            aria-label={t('support.infoTitle')}
          >
            ⓘ
          </button>
        </div>
      </header>

      <div className="flex min-h-0 flex-1">
        <div ref={scrollRef} className="flex-1 overflow-y-auto px-4 py-4">
          <div className="mx-auto flex max-w-2xl flex-col gap-3">
            {notice && <Banner tone="info">{notice}</Banner>}
            {notices.map((n, i) => (
              <Banner key={i} tone="info">
                {n}
              </Banner>
            ))}

            {messages.map((m, i) => (
              <div
                key={m.id ?? i}
                className={m.role === 'user' ? 'flex justify-end' : 'flex justify-start'}
              >
                <div className={m.role === 'user' ? 'max-w-[80%]' : 'w-full'}>
                  {m.role === 'user' ? (
                    <div className="rounded-2xl rounded-br-md bg-brand-700 px-4 py-2.5 text-[15px] text-white">
                      {m.content}
                    </div>
                  ) : (
                    renderMsg(m)
                  )}
                </div>
              </div>
            ))}

            {flowStep > 0 && (
              <div className="rounded-xl bg-warn-bg px-3 py-2 text-sm font-medium text-warn lg:hidden">
                {t('support.progressTitle')}{' '}
                {totalStep
                  ? t('support.progressStep', { cur: flowStep, total: totalStep })
                  : t('support.progressStepNoTotal', { cur: flowStep })}
              </div>
            )}

            <div className="rounded-xl border border-line bg-white px-3 py-2 text-xs text-ink2">
              {t('support.noticeFlow')}
            </div>
          </div>
        </div>

        {railSteps.length > 0 && (
          <aside className="hidden w-72 flex-none overflow-y-auto border-l border-line bg-white/60 p-4 lg:block">
            <div className="sticky top-0">
              <div className="mb-3 rounded-xl bg-brand-soft p-3">
                <div className="flex items-center gap-2 font-display text-sm font-bold text-brand-700">
                  <span className="h-2 w-2 animate-pulse-ring rounded-full bg-ai-500" />
                  {t('support.progressTitle')}
                </div>
              </div>
              <DiagnosisRail steps={railSteps} note={t('support.noticeFlow')} />
            </div>
          </aside>
        )}
      </div>

      <footer className="border-t border-line bg-white px-4 py-3">
        <div className="mx-auto flex max-w-2xl flex-col gap-2">
          <div className="flex flex-wrap items-center gap-2 text-xs">
            <span className="font-semibold text-ink2">{t('support.quick')}</span>
            <button
              className="rounded-full border border-line px-3 py-1 font-medium text-ink2 transition hover:border-brand-500 hover:text-brand-700"
              onClick={() => setInput(t('support.errorCode') + ' E102')}
            >
              ⌨ {t('support.errorCode')}
            </button>
            <button
              className="rounded-full border border-line px-3 py-1 font-medium text-ink2 transition hover:border-brand-500 hover:text-brand-700"
              onClick={() => fileInputRef.current?.click()}
            >
              📷 {t('support.image')}
            </button>
            <button
              className="ml-auto rounded-full border border-human-500 bg-human-soft px-3 py-1 font-semibold text-human-600 transition hover:bg-human-100"
              onClick={() => setShowHandoff(true)}
            >
              🧑 {t('support.toHuman')}
            </button>
          </div>
          <div className="flex items-end gap-2">
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (f) sendImage(f);
                e.target.value = '';
              }}
            />
            <Textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder={t('support.placeholder')}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  send();
                }
              }}
              className="flex-1"
            />
            <Button variant="ai" onClick={send} disabled={busy || !convId}>
              {busy ? t('support.sending') : t('support.send')}
            </Button>
          </div>
          <div className="text-[11px] text-ink2">{t('support.privacy')}</div>
        </div>
      </footer>

      <Modal open={showInfo} title={t('support.infoTitle')} onClose={() => setShowInfo(false)}>
        <div className="space-y-2 text-sm">
          <div>
            <span className="text-ink2">{t('support.region')}：</span>
            {region}
          </div>
          <div>
            <span className="text-ink2">{t('support.language')}：</span>
            {language}
          </div>
          <div>
            <span className="text-ink2">{t('support.sessionNo')}：</span>
            {convId}
          </div>
          <div className="pt-2">
            <Button variant="ghost" onClick={loadReplays}>
              {t('support.viewReplays')}
            </Button>
            {historyOpen && (
              <div className="mt-2 max-h-60 overflow-y-auto rounded border border-line p-2 text-xs">
                {replays.length === 0 ? (
                  <div className="text-ink2">{t('support.noReplays')}</div>
                ) : (
                  replays.map((r, i) => (
                    <div key={i} className="border-b border-line py-1 last:border-0">
                      <div className="font-medium text-ink">
                        #{i + 1} · {r.title}
                      </div>
                      <div className="text-ink2">
                        {r.steps?.length ?? 0} {t('support.stepsUnit')} · {r.verdict ?? '—'}
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        </div>
      </Modal>

      <Modal
        open={showHandoff}
        title={t('support.handoffTitle')}
        onClose={() => setShowHandoff(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setShowHandoff(false)}>
              {t('support.handoffCancel')}
            </Button>
            <Button variant="ai" disabled={!handoffAgree || !convId} onClick={submitHandoff}>
              {handoffId ? t('support.handoffSubmitted') : t('support.handoffSubmit')}
            </Button>
          </>
        }
      >
        <div className="space-y-3 text-sm">
          {handoffId ? (
            <div className="rounded-lg bg-ok-bg px-3 py-2 text-ok">{t('support.handoffCreated', { id: handoffId })}</div>
          ) : (
            <>
              <p className="text-ink2">{t('support.handoffDesc')}</p>
              <div>
                <label className="mb-1 block text-xs text-ink2">{t('support.handoffReason')}</label>
                <Textarea
                  value={handoffReason}
                  onChange={(e) => setHandoffReason(e.target.value)}
                  placeholder={t('support.handoffReasonPh')}
                />
              </div>
              <div>
                <label className="mb-1 block text-xs text-ink2">{t('support.handoffContact')}</label>
                <Input
                  value={handoffContact}
                  onChange={(e) => setHandoffContact(e.target.value)}
                  placeholder="name@example.com / +49..."
                />
              </div>
              <label className="flex items-start gap-2 text-xs text-ink2">
                <input
                  type="checkbox"
                  checked={handoffAgree}
                  onChange={(e) => setHandoffAgree(e.target.checked)}
                />
                <span>{t('support.handoffAgree')}</span>
              </label>
              <div className="rounded bg-slate-50 px-2 py-1 text-[11px] text-ink2/80">
                {t('support.handoffChannel')}
              </div>
            </>
          )}
        </div>
      </Modal>

      <Modal
        open={showChange}
        title={t('support.changeProductTitle')}
        onClose={() => setShowChange(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setShowChange(false)}>
              {t('support.handoffCancel')}
            </Button>
            <Button
              variant="ai"
              disabled={!changeProductId.trim() || !convId}
              onClick={changeProduct}
            >
              {t('support.changeProductUpdate')}
            </Button>
          </>
        }
      >
        <div className="space-y-3 text-sm">
          <p className="text-ink2">{t('support.changeProductDesc')}</p>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('support.changeProductPlaceholder')}</label>
            <select
              className="w-full rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-soft"
              value={changeProductId}
              onChange={(e) => setChangeProductId(e.target.value)}
            >
              <option value="">{t('common.select')}</option>
              {productOptions.map((product) => (
                <option key={product.id} value={product.id}>
                  {product.displayName} · {product.model}
                  {product.hardwareVersion ? ` · ${product.hardwareVersion}` : ''}
                </option>
              ))}
            </select>
          </div>
        </div>
      </Modal>
    </div>
  );
}
