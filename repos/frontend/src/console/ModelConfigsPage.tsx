import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Input, Modal, Card, PageHeader, StatCard, Tag, EmptyState } from '../components/ui';
import {
  listModelProviders,
  createModelProvider,
  updateModelProvider,
  testModelProvider,
  defaultModelProvider,
} from '../lib/api';
import type { ModelProvider } from '../lib/types';

export default function ModelConfigsPage() {
  const { t } = useTranslation();
  const [providers, setProviders] = useState<ModelProvider[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [testResult, setTestResult] = useState<string | null>(null);
  const [form, setForm] = useState({
    name: '',
    providerType: 'openai_compatible',
    baseUrl: '',
    modelName: '',
    apiKey: '',
    visionModel: '',
    temperature: 0.2,
    maxTokens: 1024,
    timeoutMs: 30000,
    isDefault: false,
  });

  const load = async () => {
    setLoading(true);
    try {
      setProviders(await listModelProviders());
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, []);

  const openCreate = () => {
    setEditingId(null);
    setForm({
      name: '',
      providerType: 'openai_compatible',
      baseUrl: '',
      modelName: '',
      apiKey: '',
      visionModel: '',
      temperature: 0.2,
      maxTokens: 1024,
      timeoutMs: 30000,
      isDefault: false,
    });
    setTestResult(null);
    setShowForm(true);
  };
  const openEdit = (p: ModelProvider) => {
    setEditingId(p.id);
    setForm({
      name: p.name,
      providerType: p.providerType,
      baseUrl: p.baseUrl,
      modelName: p.modelName,
      apiKey: '',
      visionModel: p.visionModel ?? '',
      temperature: p.temperature ?? 0.2,
      maxTokens: p.maxTokens ?? 1024,
      timeoutMs: p.timeoutMs ?? 30000,
      isDefault: !!p.isDefault,
    });
    setTestResult(null);
    setShowForm(true);
  };
  const save = async () => {
    setSaving(true);
    try {
      const payload = {
        name: form.name,
        providerType: form.providerType,
        baseUrl: form.baseUrl,
        modelName: form.modelName,
        apiKey: form.apiKey || undefined,
        visionModel: form.visionModel || undefined,
        temperature: form.temperature,
        maxTokens: form.maxTokens,
        timeoutMs: form.timeoutMs,
        isDefault: form.isDefault,
      };
      if (editingId) await updateModelProvider(editingId, payload);
      else await createModelProvider(payload);
      setShowForm(false);
      void load();
    } finally {
      setSaving(false);
    }
  };
  const test = async () => {
    setTestResult('…');
    try {
      await testModelProvider({
        providerType: form.providerType,
        baseUrl: form.baseUrl,
        modelName: form.modelName,
        apiKey: form.apiKey,
        visionModel: form.visionModel || undefined,
      });
      setTestResult(t('models.connectionOk'));
    } catch {
      setTestResult(t('models.connectionFailed'));
    }
  };
  const setDefault = async (id: string) => {
    await defaultModelProvider(id);
    void load();
  };

  if (loading) return <div className="p-6 text-sm text-ink2">{t('common.loading')}</div>;

  const enabled = providers.filter((p) => p.enabled).length;

  return (
    <div className="enter">
      <PageHeader
        title={t('models.title')}
        subtitle={t('models.subtitle')}
        icon="⚙"
        actions={<Button variant="primary" onClick={openCreate}>{t('models.addProvider')}</Button>}
      />

      <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3">
        <StatCard label={t('models.title')} value={providers.length} tone="brand" />
        <StatCard label={t('models.enabled')} value={enabled} tone="ok" />
        <StatCard label={t('models.default')} value={providers.filter((p) => p.isDefault).length} tone="ai" />
      </div>

      {providers.length === 0 ? (
        <Card>
          <EmptyState title={t('models.editTitle')} hint={t('models.subtitle')} action={<Button variant="primary" onClick={openCreate}>{t('models.addProvider')}</Button>} />
        </Card>
      ) : (
        <Card className="overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-brand-soft/50 text-ink2">
                <th className="border-b border-line px-4 py-2.5 text-left font-semibold">{t('models.name')}</th>
                <th className="border-b border-line px-4 py-2.5 text-left font-semibold">{t('models.model')}</th>
                <th className="border-b border-line px-4 py-2.5 text-left font-semibold">{t('models.state')}</th>
                <th className="border-b border-line px-4 py-2.5 text-left font-semibold">{t('models.action')}</th>
              </tr>
            </thead>
            <tbody>
              {providers.map((p) => (
                <tr key={p.id} className="border-t border-line transition hover:bg-brand-soft/40">
                  <td className="px-4 py-3">
                    <div className="font-medium text-ink">{p.name}</div>
                    {p.isDefault && <Tag tone="ai" className="mt-1">{t('models.default')}</Tag>}
                  </td>
                  <td className="px-4 py-3 text-ink2">{p.modelName}</td>
                  <td className="px-4 py-3">
                    <Tag tone={p.enabled ? 'ok' : 'mute'}>{p.enabled ? t('models.enabled') : t('models.disabled')}</Tag>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-2">
                      <Button variant="ghost" size="sm" onClick={() => openEdit(p)}>
                        {t('models.editTitle')}
                      </Button>
                      <Button variant="ghost" size="sm" onClick={test}>
                        {t('models.testConnection')}
                      </Button>
                      {!p.isDefault && (
                        <Button variant="ghost" size="sm" onClick={() => setDefault(p.id)}>
                          {t('models.useAsDefault')}
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}

      <Modal
        open={showForm}
        title={t('models.editTitle')}
        onClose={() => setShowForm(false)}
        widthClass="max-w-lg"
        footer={
          <>
            <Button variant="ghost" onClick={() => setShowForm(false)}>
              {t('models.cancel')}
            </Button>
            <Button variant="primary" onClick={save} disabled={saving}>
              {t('models.save')}
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold text-ink2">{t('models.name')}</label>
              <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder={t('models.namePlaceholder')} />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-ink2">{t('models.providerType')}</label>
              <select
                className="w-full rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-soft"
                value={form.providerType}
                onChange={(e) => setForm({ ...form, providerType: e.target.value })}
              >
                <option value="openai_compatible">{t('models.openaiCompatible')}</option>
              </select>
            </div>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('models.baseUrl')}</label>
            <Input value={form.baseUrl} onChange={(e) => setForm({ ...form, baseUrl: e.target.value })} placeholder={t('models.baseUrlPlaceholder')} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold text-ink2">{t('models.modelName')}</label>
              <Input value={form.modelName} onChange={(e) => setForm({ ...form, modelName: e.target.value })} placeholder={t('models.modelNamePlaceholder')} />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-ink2">{t('models.visionModel')}</label>
              <Input value={form.visionModel} onChange={(e) => setForm({ ...form, visionModel: e.target.value })} />
            </div>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold text-ink2">{t('models.apiKey')}</label>
            <Input
              type="password"
              value={form.apiKey}
              onChange={(e) => setForm({ ...form, apiKey: e.target.value })}
              placeholder={t('models.apiKeyPlaceholder')}
            />
          </div>
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold text-ink2">{t('models.temperature')}</label>
              <Input type="number" step="0.1" value={form.temperature} onChange={(e) => setForm({ ...form, temperature: Number(e.target.value) })} />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-ink2">{t('models.maxTokens')}</label>
              <Input type="number" value={form.maxTokens} onChange={(e) => setForm({ ...form, maxTokens: Number(e.target.value) })} />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-ink2">{t('models.timeoutMs')}</label>
              <Input type="number" value={form.timeoutMs} onChange={(e) => setForm({ ...form, timeoutMs: Number(e.target.value) })} />
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm text-ink2">
            <input
              type="checkbox"
              checked={form.isDefault}
              onChange={(e) => setForm({ ...form, isDefault: e.target.checked })}
            />
            {t('models.useAsDefault')}
          </label>
          <Button variant="ai" onClick={test}>
            {t('models.testConnection')}
          </Button>
          {testResult && <div className="text-xs text-ink2">{testResult}</div>}
        </div>
      </Modal>
    </div>
  );
}
