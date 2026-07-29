import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import { Button, Input, Modal, Tag } from '../components/ui';
import { useTranslation } from '../i18n';

type Config = {
  id: string;
  name: string;
  modelName: string;
  enabled: boolean;
  defaultConfig: boolean;
  configured: boolean;
};

export default function ModelConfigsPage() {
  const { t } = useTranslation();
  const [rows, setRows] = useState<Config[]>([]);
  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState('');
  const [form, setForm] = useState({
    name: '',
    providerType: 'OPENAI_COMPATIBLE',
    baseUrl: '',
    modelName: '',
    visionModel: '',
    apiKey: '',
    timeoutMs: 20000,
    temperature: 0,
    maxTokens: 800,
    enabled: true,
    defaultConfig: true,
  });
  const load = () => api('/model-configurations').then((items) => setRows(items ?? []));
  useEffect(() => {
    load();
  }, []);
  const save = async () => {
    await api('/model-configurations', { method: 'POST', body: JSON.stringify(form) });
    setOpen(false);
    load();
  };
  const test = async (id: string) => {
    const result = await api(`/model-configurations/${id}/test`, { method: 'POST' });
    setMessage(result.reachable ? t('models.connectionOk') : t('models.connectionFailed'));
  };
  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-ink">{t('models.title')}</h1>
          <p className="text-sm text-ink2">{t('models.subtitle')}</p>
        </div>
        <Button variant="ai" onClick={() => setOpen(true)}>
          {t('models.addProvider')}
        </Button>
      </div>
      {message && (
        <div className="mb-3 rounded border border-line bg-white p-3 text-sm">{message}</div>
      )}
      <div className="overflow-hidden rounded-xl border border-line bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-ink2">
            <tr>
              <th className="px-4 py-2 text-left">{t('models.name')}</th>
              <th className="px-4 py-2 text-left">{t('models.model')}</th>
              <th className="px-4 py-2 text-left">{t('models.state')}</th>
              <th className="px-4 py-2 text-left">{t('models.action')}</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr className="border-t border-line" key={row.id}>
                <td className="px-4 py-2">
                  {row.name}
                  {row.defaultConfig && <Tag tone="ai">{t('models.default')}</Tag>}
                </td>
                <td className="px-4 py-2">{row.modelName}</td>
                <td className="px-4 py-2">
                  <Tag tone={row.enabled && row.configured ? 'ok' : 'warn'}>
                    {row.enabled ? t('models.enabled') : t('models.disabled')}
                  </Tag>
                </td>
                <td className="px-4 py-2">
                  <button className="text-ai hover:underline" onClick={() => test(row.id)}>
                    {t('models.testConnection')}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Modal
        open={open}
        title={t('models.editTitle')}
        onClose={() => setOpen(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setOpen(false)}>
              {t('models.cancel')}
            </Button>
            <Button
              variant="ai"
              disabled={!form.name || !form.baseUrl || !form.modelName || !form.apiKey}
              onClick={save}
            >
              {t('models.save')}
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <Input
            placeholder={t('models.namePlaceholder')}
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
          <Input
            placeholder={t('models.baseUrlPlaceholder')}
            value={form.baseUrl}
            onChange={(e) => setForm({ ...form, baseUrl: e.target.value })}
          />
          <Input
            placeholder={t('models.modelNamePlaceholder')}
            value={form.modelName}
            onChange={(e) => setForm({ ...form, modelName: e.target.value })}
          />
          <Input
            placeholder={t('models.visionModel')}
            value={form.visionModel}
            onChange={(e) => setForm({ ...form, visionModel: e.target.value })}
          />
          <Input
            type="password"
            placeholder={t('models.apiKeyPlaceholder')}
            value={form.apiKey}
            onChange={(e) => setForm({ ...form, apiKey: e.target.value })}
          />
          <div className="grid grid-cols-3 gap-2">
            <Input
              type="number"
              value={form.timeoutMs}
              onChange={(e) => setForm({ ...form, timeoutMs: Number(e.target.value) })}
            />
            <Input
              type="number"
              step="0.1"
              value={form.temperature}
              onChange={(e) => setForm({ ...form, temperature: Number(e.target.value) })}
            />
            <Input
              type="number"
              value={form.maxTokens}
              onChange={(e) => setForm({ ...form, maxTokens: Number(e.target.value) })}
            />
          </div>
          <label className="flex gap-2 text-sm">
            <input
              type="checkbox"
              checked={form.defaultConfig}
              onChange={(e) => setForm({ ...form, defaultConfig: e.target.checked })}
            />
            {t('models.useAsDefault')}
          </label>
        </div>
      </Modal>
    </div>
  );
}
