import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../lib/auth';
import { postAnswer, pub, pubUpload, streamAnswer } from '../lib/api';
import { Button, Input, Modal, Textarea } from '../components/ui';
import { langNames, LANGS, useTranslation } from '../i18n';

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
      ? 'border-green-400 bg-green-50 text-green-700'
      : tone === 'bad'
        ? 'border-red-300 bg-red-50 text-red-600'
        : tone === 'warn'
          ? 'border-amber-300 bg-amber-50 text-amber-700'
          : 'border-slate-300 bg-slate-50 text-slate-600';
  return (
    <div
      className={`rounded-2xl border p-4 ${risk === 'high' ? 'border-red-300 bg-red-50/60' : 'border-line bg-white'}`}
    >
      {risk === 'high' && (
        <div className="mb-2 inline-block rounded-md bg-red-100 px-2 py-0.5 text-xs font-bold text-red-600">
          {t('support.highRiskStep')}
        </div>
      )}
      <div className="mb-1 text-sm font-bold text-ink">{t('support.stepLabel')}</div>
      <div className="whitespace-pre-wrap text-[15px] leading-relaxed text-ink">{m.content}</div>
      {sources.length > 0 && (
        <div className="mt-2 text-xs text-ink2">
          {t('support.sources')}
          {sources.map((s, i) => (
            <span key={i} className="mr-2">
              <span className="text-ai">{s}</span>
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
              className={`rounded-lg border px-3 py-1.5 text-sm font-medium ${toneClass(r.tone)}`}
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
              className={`rounded-lg border px-3 py-1.5 text-sm font-medium ${toneClass(r.tone)}`}
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
              className={`rounded-lg border px-3 py-1.5 text-sm font-medium ${toneClass(r.tone)}`}
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
        <div className="rounded-2xl border border-line bg-white p-4 text-sm text-ink2">
          {t('support.analyzing')}
        </div>
      );
    const intent = m.intent;
    const c = m.content ?? '';
    if (intent === 'SAFETY_STOP')
      return (
        <div className="rounded-2xl border border-red-300 bg-red-50 p-4">
          <div className="font-bold text-red-600">{t('support.safetyTitle')}</div>
          <div className="mt-1 whitespace-pre-wrap text-[15px] text-ink">{c}</div>
        </div>
      );
    if (intent === 'HUMAN_REQUEST')
      return (
        <div className="rounded-2xl border border-ai/40 bg-ai-soft p-4">
          <div className="font-bold text-ai">{t('support.humanTitle')}</div>
          <div className="mt-1 whitespace-pre-wrap text-[15px] text-ink">{c}</div>
        </div>
      );
    if (intent === 'TROUBLESHOOTING') {
      if (m.flowControl) return <StepCard m={m} onReply={reply} t={t} />;
      return (
        <div className="rounded-2xl border border-line bg-white p-4">
          <div className="mb-1 text-sm font-bold text-ink">{t('support.stepLabel')}</div>
          <div className="whitespace-pre-wrap text-[15px] text-ink">{c}</div>
        </div>
      );
    }
    if (intent === 'INFO' && c.startsWith('认证'))
      return (
        <div className="rounded-2xl border border-line bg-white p-4 text-[15px] text-ink">
          {t('support.demoSession')}
          {c}
        </div>
      );
    return (
      <div className="rounded-2xl border border-line bg-white p-4 whitespace-pre-wrap text-[15px] text-ink">
        {c}
      </div>
    );
  };

  const flowStep = messages.filter((m) => m.intent === 'TROUBLESHOOTING' && m.flowControl).length;
  const totalStep =
    [...messages].reverse().find((m: any) => m.flowControl)?.flowControl?.totalSteps ?? null;

  const onLangChange = (l: string) => {
    setLanguage(l);
    i18n.changeLanguage(l);
  };

  return (
    <div className="flex h-screen flex-col bg-slate-50">
      <header className="flex items-center justify-between border-b border-line bg-white px-4 py-2">
        <div className="flex items-center gap-2">
          <span className="text-sm font-bold text-ink">HARDWARE AI</span>
          <span className="rounded bg-slate-100 px-2 py-0.5 text-xs text-ink2">
            {t('support.chatTitle', { id: convId.slice(0, 8) })}
          </span>
          <span className="rounded bg-slate-100 px-2 py-0.5 text-xs text-ink2">
            {region} · {language}
          </span>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs text-ink2">{t('support.region')}</span>
          <select
            className="rounded border border-line px-2 py-1 text-sm"
            value={region}
            onChange={(e) => setRegion(e.target.value)}
          >
            {['EU', 'NA', 'APAC', 'LATAM', 'MEA'].map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
          <span className="text-xs text-ink2">{t('support.language')}</span>
          <select
            className="rounded border border-line px-2 py-1 text-sm"
            value={language}
            onChange={(e) => onLangChange(e.target.value)}
          >
            {LANGS.map((l) => (
              <option key={l} value={l}>
                {langNames[l] ?? l}
              </option>
            ))}
          </select>
          <button
            className="rounded border border-line px-2 py-1 text-xs text-ink2 hover:bg-slate-50"
            onClick={() => void openProductSelection()}
          >
            {t('support.changeProduct')}
          </button>
          {isAuthenticated ? (
            <>
              <button
                className="rounded border border-line px-2 py-1 text-xs text-ink2 hover:bg-slate-50"
                onClick={() => navigate('/console')}
              >
                {t('support.console')}
              </button>
              <button
                className="rounded border border-line px-2 py-1 text-xs text-ink2 hover:bg-slate-50"
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
              className="rounded border border-line px-2 py-1 text-xs text-ink2 hover:bg-slate-50"
              onClick={() => navigate('/login')}
            >
              {t('login.submit')}
            </button>
          )}
          <button
            className="rounded border border-line px-2 py-1 text-xs text-ink2 hover:bg-slate-50"
            onClick={() => setShowInfo(true)}
          >
            {t('support.infoTitle')}
          </button>
        </div>
      </header>

      <div ref={scrollRef} className="flex-1 overflow-y-auto px-4 py-4">
        <div className="mx-auto flex max-w-2xl flex-col gap-3">
          {notice && (
            <div className="rounded-lg bg-blue-50 px-3 py-2 text-sm text-blue-700">{notice}</div>
          )}
          {notices.map((n, i) => (
            <div key={i} className="rounded-lg bg-blue-50 px-3 py-2 text-sm text-blue-700">
              {n}
            </div>
          ))}

          {messages.map((m, i) => (
            <div
              key={m.id ?? i}
              className={m.role === 'user' ? 'flex justify-end' : 'flex justify-start'}
            >
              <div className={m.role === 'user' ? 'max-w-[80%]' : 'w-full max-w-full'}>
                {m.role === 'user' ? (
                  <div className="rounded-2xl bg-ai px-4 py-2 text-[15px] text-white">
                    {m.content}
                  </div>
                ) : (
                  renderMsg(m)
                )}
              </div>
            </div>
          ))}

          {flowStep > 0 && (
            <div className="rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-700">
              {t('support.progressTitle')}{' '}
              {totalStep
                ? t('support.progressStep', {
                    cur: flowStep,
                    total: totalStep,
                  })
                : t('support.progressStepNoTotal', { cur: flowStep })}
            </div>
          )}

          <div className="rounded-xl border border-line bg-white p-3 text-xs text-ink2">
            {t('support.noticeFlow')}
          </div>
        </div>
      </div>

      <footer className="border-t border-line bg-white px-4 py-3">
        <div className="mx-auto flex max-w-2xl flex-col gap-2">
          <div className="flex flex-wrap items-center gap-2 text-xs text-ink2">
            <span>{t('support.quick')}</span>
            <button
              className="rounded-full border border-line px-3 py-1 hover:bg-slate-50"
              onClick={() => setInput(t('support.errorCode') + ' E102')}
            >
              ⌨ {t('support.errorCode')}
            </button>
            <button
              className="rounded-full border border-line px-3 py-1 hover:bg-slate-50"
              onClick={() => fileInputRef.current?.click()}
            >
              📷 {t('support.image')}
            </button>
            <button
              className="rounded-full border border-ai/40 px-3 py-1 text-ai hover:bg-ai-soft"
              onClick={() => setShowHandoff(true)}
            >
              {t('support.toHuman')}
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
          <div className="text-[11px] text-ink2/70">{t('support.privacy')}</div>
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
            <div className="rounded-lg bg-green-50 px-3 py-2 text-green-700">
              {t('support.handoffCreated', { id: handoffId })}
            </div>
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
                <label className="mb-1 block text-xs text-ink2">
                  {t('support.handoffContact')}
                </label>
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
            <label className="mb-1 block text-xs text-ink2">
              {t('support.changeProductPlaceholder')}
            </label>
            <select
              className="w-full rounded border border-line px-3 py-2 text-sm"
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
