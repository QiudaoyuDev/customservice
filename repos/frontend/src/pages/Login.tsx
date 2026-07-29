import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth';
import { Button, Input } from '../components/ui';
import { useTranslation, LANGS, langNames } from '../i18n';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const { t, i18n } = useTranslation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await login(email, password);
      navigate('/console');
    } catch {
      setError(t('login.error'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-canvas p-4">
      <div
        className="pointer-events-none absolute inset-x-0 top-0 h-64 opacity-60"
        style={{
          background:
            'radial-gradient(80% 120% at 50% -20%, rgba(31,182,166,.18), transparent 60%)',
        }}
      />
      <div className="absolute right-4 top-4 z-10">
        <select
          className="h-9 rounded-lg border border-line bg-white px-2 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-soft"
          value={i18n.language}
          onChange={(e) => i18n.changeLanguage(e.target.value)}
        >
          {LANGS.map((l) => (
            <option key={l} value={l}>
              {langNames[l] ?? l}
            </option>
          ))}
        </select>
      </div>

      <form
        onSubmit={submit}
        className="relative w-full max-w-sm space-y-4 rounded-2xl border border-line bg-white p-8 shadow-card enter-up"
      >
        <div className="flex flex-col items-center text-center">
          <div className="mb-3 grid h-11 w-11 place-items-center rounded-xl bg-gradient-to-br from-brand-700 to-ai-500 font-display text-lg font-extrabold text-white">
            H
          </div>
          <div className="font-display text-lg font-bold text-ink">HARDWARE AI</div>
          <div className="text-sm text-ink2">{t('login.title')}</div>
        </div>

        <div>
          <label className="mb-1 block text-xs font-semibold text-ink2">{t('login.email')}</label>
          <Input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" />
        </div>
        <div>
          <label className="mb-1 block text-xs font-semibold text-ink2">{t('login.password')}</label>
          <Input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
          />
        </div>

        {error && (
          <div className="rounded-lg bg-danger-bg px-3 py-2 text-sm text-danger">{error}</div>
        )}

        <Button type="submit" variant="ai" className="w-full" disabled={busy}>
          {busy ? t('login.submitting') : t('login.submit')}
        </Button>
      </form>
    </div>
  );
}
