import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import { Button, Input, Modal, Tag } from '../components/ui';

type Config = {
  id: string;
  name: string;
  modelName: string;
  enabled: boolean;
  defaultConfig: boolean;
  configured: boolean;
};

export default function ModelConfigsPage() {
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
    setMessage(
      result.reachable
        ? 'Connection succeeded.'
        : 'Connection failed. Check endpoint and credentials.',
    );
  };
  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-ink">Model providers</h1>
          <p className="text-sm text-ink2">Credentials are encrypted and never shown again.</p>
        </div>
        <Button variant="ai" onClick={() => setOpen(true)}>
          Add provider
        </Button>
      </div>
      {message && (
        <div className="mb-3 rounded border border-line bg-white p-3 text-sm">{message}</div>
      )}
      <div className="overflow-hidden rounded-xl border border-line bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-ink2">
            <tr>
              <th className="px-4 py-2 text-left">Name</th>
              <th className="px-4 py-2 text-left">Model</th>
              <th className="px-4 py-2 text-left">State</th>
              <th className="px-4 py-2 text-left">Action</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr className="border-t border-line" key={row.id}>
                <td className="px-4 py-2">
                  {row.name}
                  {row.defaultConfig && <Tag tone="ai">Default</Tag>}
                </td>
                <td className="px-4 py-2">{row.modelName}</td>
                <td className="px-4 py-2">
                  <Tag tone={row.enabled && row.configured ? 'ok' : 'warn'}>
                    {row.enabled ? 'Enabled' : 'Disabled'}
                  </Tag>
                </td>
                <td className="px-4 py-2">
                  <button className="text-ai hover:underline" onClick={() => test(row.id)}>
                    Test connection
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Modal
        open={open}
        title="Add model provider"
        onClose={() => setOpen(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="ai"
              disabled={!form.name || !form.baseUrl || !form.modelName || !form.apiKey}
              onClick={save}
            >
              Save encrypted configuration
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <Input
            placeholder="Name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
          <Input
            placeholder="Base URL"
            value={form.baseUrl}
            onChange={(e) => setForm({ ...form, baseUrl: e.target.value })}
          />
          <Input
            placeholder="Model name"
            value={form.modelName}
            onChange={(e) => setForm({ ...form, modelName: e.target.value })}
          />
          <Input
            placeholder="Vision model (optional)"
            value={form.visionModel}
            onChange={(e) => setForm({ ...form, visionModel: e.target.value })}
          />
          <Input
            type="password"
            placeholder="API key (stored encrypted)"
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
            Use as tenant default
          </label>
        </div>
      </Modal>
    </div>
  );
}
