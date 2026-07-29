import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import { Button, Input, Textarea, Tag, Modal, StatusFlow, EmptyState } from '../components/ui';
import { useTranslation, LANGS, langNames, regionLabel } from '../i18n';

interface FlowNode {
  nodeKey: string;
  nodeType: string;
  prompt: string;
  expectedInput?: string;
  risk?: string;
  branchYes?: string;
  branchNo?: string;
  branchUnknown?: string;
  branchNext?: string;
  sourceRefs?: string;
}

export default function FlowsPage() {
  const { t } = useTranslation();
  const [flows, setFlows] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [selId, setSelId] = useState<string | null>(null);
  const [detail, setDetail] = useState<any>(null);
  const [editing, setEditing] = useState<FlowNode | null>(null);
  const [showNewFlow, setShowNewFlow] = useState(false);
  const [showSim, setShowSim] = useState(false);
  const [newFlow, setNewFlow] = useState({
    title: '',
    triggerIntent: 'TROUBLESHOOTING',
    productModelId: '',
    productVariantId: '',
    hardwareRevision: '',
    firmwareMin: '',
    firmwareMax: '',
    triggerPhrase: '',
    priority: 0,
    region: 'EU',
    locale: 'en',
  });
  const [simLog, setSimLog] = useState<any[]>([]);
  const [simBusy, setSimBusy] = useState(false);

  const typeLabel = (ty: string) =>
    ty === 'AI' ? t('flows.typeAI') : ty === 'HUMAN' ? t('flows.typeHuman') : t('flows.typeEnd');

  const load = async () => {
    setLoading(true);
    try {
      const list = await api('/flows');
      setFlows(list ?? []);
      if (!selId && (list ?? []).length) setSelId(list[0].id);
    } finally {
      setLoading(false);
    }
  };
  const loadDetail = async (id: string) => {
    const d = await api(`/flows/${id}`);
    setDetail({ ...d.flow, nodes: d.nodes });
    setSelId(id);
    if (d?.nodes?.length) setEditing(toEditable(d.nodes[0]));
    else setEditing(null);
  };
  useEffect(() => {
    load();
  }, []);
  useEffect(() => {
    if (selId) loadDetail(selId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selId]);

  const saveNode = async () => {
    if (!detail || !editing) return;
    const payload = {
      ...editing,
      sourceRefs: editing.sourceRefs
        ? editing.sourceRefs
            .split(',')
            .map((v) => v.trim())
            .filter(Boolean)
        : [],
    };
    if (!payload.nodeKey) payload.nodeKey = 'n' + Math.random().toString(36).slice(2, 8);
    const exists = detail.nodes?.some(
      (node: { nodeKey: string }) => node.nodeKey === payload.nodeKey,
    );
    await api(
      exists ? `/flows/${detail.id}/nodes/${payload.nodeKey}` : `/flows/${detail.id}/nodes`,
      { method: exists ? 'PUT' : 'POST', body: JSON.stringify(payload) },
    );
    loadDetail(detail.id);
  };
  const deleteNode = async (key: string) => {
    if (!detail) return;
    await api(`/flows/${detail.id}/nodes/${key}`, { method: 'DELETE' });
    loadDetail(detail.id);
  };

  const simulate = async () => {
    if (!detail) return;
    setSimBusy(true);
    setSimLog([]);
    try {
      const r = await api(`/flows/${detail.id}/simulate`, { method: 'POST' });
      setSimLog(r?.transcript ?? []);
    } finally {
      setSimBusy(false);
    }
    setShowSim(true);
  };

  const updateFlowStatus = async (status: string) => {
    if (!detail) return;
    await api(`/flows/${detail.id}/${status.toLowerCase()}`, { method: 'POST' });
    loadDetail(detail.id);
  };

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-ink">{t('flows.title')}</h1>
          <p className="text-sm text-ink2">{t('flows.subtitle')}</p>
        </div>
        <div className="flex gap-2">
          <Button variant="ai" onClick={simulate} disabled={!detail}>
            {t('flows.simulate')}
          </Button>
          <Button variant="ai" onClick={() => setShowNewFlow(true)}>
            {t('flows.new')}
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="text-sm text-ink2">{t('common.loading')}</div>
      ) : flows.length === 0 ? (
        <EmptyState title={t('flows.emptyTitle')} hint={t('flows.emptyHint')} />
      ) : (
        <div className="flex gap-4">
          <div className="w-72 shrink-0">
            <div className="space-y-2">
              {flows.map((f) => (
                <button
                  key={f.id}
                  onClick={() => setSelId(f.id)}
                  className={`block w-full rounded-xl border p-3 text-left ${selId === f.id ? 'border-ai bg-ai-soft' : 'border-line bg-white'}`}
                >
                  <div className="text-sm font-medium text-ink">{f.title}</div>
                  <div className="mt-1 text-xs text-ink2">
                    {t('flows.stepCount', { count: f.nodes?.length ?? 0 })}
                  </div>
                  <div className="mt-1">
                    <StatusFlow status={f.status} />
                  </div>
                </button>
              ))}
            </div>
          </div>

          <div className="flex-1">
            {!detail ? (
              <div className="text-sm text-ink2">{t('flows.selectFlow')}</div>
            ) : (
              <div className="space-y-4">
                <div className="flex items-center justify-between rounded-xl border border-line bg-white p-4">
                  <div>
                    <div className="font-medium text-ink">{detail.title}</div>
                    <div className="text-xs text-ink2">
                      {t('flows.triggerIntent')}：{detail.triggerIntent} ·{' '}
                      {t('flows.productModelId')} {detail.productModelId} · {detail.region} ·{' '}
                      {detail.language}
                    </div>
                  </div>
                  <div className="flex gap-2">
                    {detail.status === 'DRAFT' && (
                      <Button size="sm" variant="ai" onClick={() => updateFlowStatus('REVIEW')}>
                        {t('flows.submit')}
                      </Button>
                    )}
                    {detail.status === 'REVIEW' && (
                      <Button size="sm" variant="ai" onClick={() => updateFlowStatus('APPROVED')}>
                        {t('flows.approve')}
                      </Button>
                    )}
                    {detail.status === 'APPROVED' && (
                      <Button size="sm" variant="ai" onClick={() => updateFlowStatus('PUBLISHED')}>
                        {t('flows.publish')}
                      </Button>
                    )}
                    {detail.status === 'PUBLISHED' && (
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => updateFlowStatus('DEPRECATED')}
                      >
                        {t('flows.deprecate')}
                      </Button>
                    )}
                    {(detail.status === 'DEPRECATED' || detail.status === 'APPROVED') && (
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => updateFlowStatus('PUBLISHED')}
                      >
                        {t('flows.restore')}
                      </Button>
                    )}
                  </div>
                </div>

                <div className="flex gap-4">
                  <div className="w-1/2 space-y-2">
                    <div className="text-sm font-medium text-ink">{t('flows.node')}</div>
                    {detail.nodes?.map((n: any) => (
                      <div
                        key={n.nodeKey}
                        onClick={() => setEditing(toEditable(n))}
                        className={`cursor-pointer rounded-lg border p-3 ${editing?.nodeKey === n.nodeKey ? 'border-ai bg-ai-soft' : 'border-line bg-white'}`}
                      >
                        <div className="flex items-center justify-between">
                          <span className="text-sm text-ink">{n.nodeKey}</span>
                          <Tag tone={n.risk === 'HIGH' ? 'bad' : 'mute'}>
                            {n.risk === 'HIGH' ? t('flows.highRisk') : typeLabel(n.nodeType)}
                          </Tag>
                        </div>
                        <div className="mt-1 line-clamp-2 text-xs text-ink2">
                          {n.prompt || t('flows.noPrompt')}
                        </div>
                        {n.nodeType === 'HUMAN_ESCALATION' && (
                          <div className="mt-1 text-[10px] text-ink2">{t('flows.escalated')}</div>
                        )}
                        {n.nodeType === 'END' && (
                          <div className="mt-1 text-[10px] text-ink2">{t('flows.ended')}</div>
                        )}
                      </div>
                    ))}
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() =>
                        setEditing({
                          nodeKey: '',
                          nodeType: 'QUESTION',
                          prompt: '',
                          expectedInput: 'yes_no_unknown',
                          risk: 'LOW',
                          branchYes: '',
                          branchNo: '',
                          branchUnknown: '',
                          branchNext: '',
                          sourceRefs: '',
                        })
                      }
                    >
                      {t('flows.newNode')}
                    </Button>
                  </div>

                  <div className="w-1/2 space-y-3 rounded-xl border border-line bg-white p-4">
                    {!editing ? (
                      <div className="text-sm text-ink2">{t('flows.clickNode')}</div>
                    ) : (
                      <>
                        <div className="text-sm font-bold text-ink">
                          {t('flows.inspectorTitle', {
                            key: editing.nodeKey || t('flows.newTitleField'),
                          })}
                        </div>
                        <div>
                          <label className="mb-1 block text-xs text-ink2">{t('flows.type')}</label>
                          <select
                            className="w-full rounded border border-line px-3 py-2 text-sm"
                            value={editing.nodeType}
                            onChange={(e) => setEditing({ ...editing, nodeType: e.target.value })}
                          >
                            <option value="QUESTION">{t('flows.typeAI')}</option>
                            <option value="OBSERVE">{t('flows.typeObserve')}</option>
                            <option value="OPERATION">{t('flows.typeOperation')}</option>
                            <option value="VERIFY">{t('flows.typeVerify')}</option>
                            <option value="DECISION">{t('flows.typeDecision')}</option>
                            <option value="HUMAN_ESCALATION">{t('flows.typeHuman')}</option>
                            <option value="END">{t('flows.typeEnd')}</option>
                          </select>
                        </div>
                        <div>
                          <label className="mb-1 block text-xs text-ink2">
                            {t('flows.prompt')}
                          </label>
                          <Textarea
                            value={editing.prompt}
                            onChange={(e) => setEditing({ ...editing, prompt: e.target.value })}
                          />
                        </div>
                        <div>
                          <label className="mb-1 block text-xs text-ink2">{t('flows.risk')}</label>
                          <select
                            className="w-full rounded border border-line px-3 py-2 text-sm"
                            value={editing.risk}
                            onChange={(e) => setEditing({ ...editing, risk: e.target.value })}
                          >
                            <option value="LOW">{t('flows.riskLow')}</option>
                            <option value="HIGH">{t('flows.riskHigh')}</option>
                          </select>
                        </div>
                        {editing.nodeType !== 'END' && (
                          <div className="space-y-2">
                            <div>
                              <label className="mb-1 block text-xs text-ink2">
                                {t('flows.branchYes')}
                              </label>
                              <Input
                                value={editing.branchYes ?? ''}
                                onChange={(e) =>
                                  setEditing({ ...editing, branchYes: e.target.value })
                                }
                                placeholder={t('flows.nextNodeKey')}
                              />
                            </div>
                            <div>
                              <label className="mb-1 block text-xs text-ink2">
                                {t('flows.branchNo')}
                              </label>
                              <Input
                                value={editing.branchNo ?? ''}
                                onChange={(e) =>
                                  setEditing({ ...editing, branchNo: e.target.value })
                                }
                                placeholder={t('flows.nextNodeKey')}
                              />
                            </div>
                            <div>
                              <label className="mb-1 block text-xs text-ink2">
                                {t('flows.branchUnknown')}
                              </label>
                              <Input
                                value={editing.branchUnknown ?? ''}
                                onChange={(e) =>
                                  setEditing({ ...editing, branchUnknown: e.target.value })
                                }
                                placeholder={t('flows.nextNodeKey')}
                              />
                            </div>
                            <div>
                              <label className="mb-1 block text-xs text-ink2">
                                {t('flows.branchNext')}
                              </label>
                              <Input
                                value={editing.branchNext ?? ''}
                                onChange={(e) =>
                                  setEditing({ ...editing, branchNext: e.target.value })
                                }
                                placeholder={t('flows.nextNodeKey')}
                              />
                            </div>
                          </div>
                        )}
                        <div>
                          <label className="mb-1 block text-xs text-ink2">
                            {t('flows.sourceRefs')}
                          </label>
                          <Input
                            value={editing.sourceRefs}
                            onChange={(e) => setEditing({ ...editing, sourceRefs: e.target.value })}
                            placeholder={t('flows.sourceRefsPlaceholder')}
                          />
                        </div>
                        <div className="flex gap-2">
                          <Button size="sm" variant="ai" onClick={saveNode}>
                            {t('flows.saveNode')}
                          </Button>
                          {editing.nodeKey && (
                            <Button
                              size="sm"
                              variant="danger"
                              onClick={() => deleteNode(editing.nodeKey)}
                            >
                              {t('flows.delete')}
                            </Button>
                          )}
                        </div>
                      </>
                    )}
                  </div>
                </div>

                <CoverageNote detail={detail} t={t} />
              </div>
            )}
          </div>
        </div>
      )}

      <Modal
        open={showNewFlow}
        title={t('flows.newTitle')}
        onClose={() => setShowNewFlow(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setShowNewFlow(false)}>
              {t('common.cancel')}
            </Button>
            <Button
              variant="ai"
              onClick={async () => {
                const payload = {
                  ...newFlow,
                  productVariantId: newFlow.productVariantId || null,
                  hardwareRevision: newFlow.hardwareRevision || null,
                  firmwareMin: newFlow.firmwareMin || null,
                  firmwareMax: newFlow.firmwareMax || null,
                  triggerPhrase: newFlow.triggerPhrase || null,
                };
                await api('/flows', { method: 'POST', body: JSON.stringify(payload) });
                setShowNewFlow(false);
                setNewFlow({
                  title: '',
                  triggerIntent: 'TROUBLESHOOTING',
                  productModelId: '',
                  productVariantId: '',
                  hardwareRevision: '',
                  firmwareMin: '',
                  firmwareMax: '',
                  triggerPhrase: '',
                  priority: 0,
                  region: 'EU',
                  locale: 'en',
                });
                load();
              }}
            >
              {t('flows.create')}
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('flows.newTitleField')}</label>
            <Input
              value={newFlow.title}
              onChange={(e) => setNewFlow({ ...newFlow, title: e.target.value })}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('flows.triggerIntent')}</label>
            <Input
              value={newFlow.triggerIntent}
              onChange={(e) => setNewFlow({ ...newFlow, triggerIntent: e.target.value })}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('flows.productModelId')}</label>
            <Input
              value={newFlow.productModelId}
              onChange={(e) => setNewFlow({ ...newFlow, productModelId: e.target.value })}
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs text-ink2">
                {t('flows.productVariant')}
              </label>
              <Input
                value={newFlow.productVariantId}
                onChange={(e) => setNewFlow({ ...newFlow, productVariantId: e.target.value })}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs text-ink2">{t('flows.hardwareRevision')}</label>
              <Input
                value={newFlow.hardwareRevision}
                onChange={(e) => setNewFlow({ ...newFlow, hardwareRevision: e.target.value })}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs text-ink2">{t('flows.firmwareMin')}</label>
              <Input
                value={newFlow.firmwareMin}
                onChange={(e) => setNewFlow({ ...newFlow, firmwareMin: e.target.value })}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs text-ink2">{t('flows.firmwareMax')}</label>
              <Input
                value={newFlow.firmwareMax}
                onChange={(e) => setNewFlow({ ...newFlow, firmwareMax: e.target.value })}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs text-ink2">{t('flows.triggerPhrase')}</label>
              <Input
                value={newFlow.triggerPhrase}
                onChange={(e) => setNewFlow({ ...newFlow, triggerPhrase: e.target.value })}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs text-ink2">{t('flows.priority')}</label>
              <Input
                type="number"
                value={newFlow.priority}
                onChange={(e) => setNewFlow({ ...newFlow, priority: Number(e.target.value) })}
              />
            </div>
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('flows.region')}</label>
            <select
              className="w-full rounded border border-line px-3 py-2 text-sm"
              value={newFlow.region}
              onChange={(e) => setNewFlow({ ...newFlow, region: e.target.value })}
            >
              {['EU', 'NA', 'APAC', 'LATAM', 'MEA'].map((r) => (
                <option key={r} value={r}>
                  {regionLabel(t, r)}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs text-ink2">{t('flows.language')}</label>
            <select
              className="w-full rounded border border-line px-3 py-2 text-sm"
              value={newFlow.locale}
              onChange={(e) => setNewFlow({ ...newFlow, locale: e.target.value.split('-')[0] })}
            >
              {LANGS.map((l) => (
                <option key={l} value={l}>
                  {langNames[l] ?? l}
                </option>
              ))}
            </select>
          </div>
        </div>
      </Modal>

      <Modal open={showSim} title={t('flows.simulateTitle')} onClose={() => setShowSim(false)}>
        <div className="space-y-2 text-sm">
          {simBusy ? (
            <div className="text-ink2">{t('flows.simulating')}</div>
          ) : simLog.length === 0 ? (
            <div className="text-ink2">{t('flows.simStart')}</div>
          ) : (
            simLog.map((s, i) => (
              <div key={i} className="rounded border border-line p-2">
                <div className="text-xs text-ink2">
                  {t('flows.node')} {s.nodeKey} · {s.intent}
                </div>
                <div className="text-ink">{s.prompt || s.content}</div>
              </div>
            ))
          )}
        </div>
      </Modal>
    </div>
  );
}

function toEditable(node: any): FlowNode {
  return {
    ...node,
    sourceRefs: Array.isArray(node.sourceRefs)
      ? node.sourceRefs.join(', ')
      : (node.sourceRefs ?? ''),
  };
}

function CoverageNote({ detail, t }: { detail: any; t: (k: string, p?: any) => string }) {
  if (!detail?.graph) return null;
  const { reachable, unreachable } = detail.graph;
  const total = detail.nodes?.length ?? 0;
  return (
    <div className="rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-700">
      {t('flows.coverageReached', { visited: reachable?.length ?? 0, nodes: total })}
      {unreachable?.length ? t('flows.coverageUnreached', { list: unreachable.join(', ') }) : ''}
    </div>
  );
}
