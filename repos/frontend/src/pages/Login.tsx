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
    <div className="relative flex min-h-screen items-center justify-center bg-slate-50">
      <div className="absolute right-4 top-4">
        <select
          className="rounded border border-line bg-white px-2 py-1 text-sm"
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
        className="w-full max-w-sm space-y-4 rounded-2xl border border-line bg-white p-8 shadow-sm"
      >
        <div className="text-center">
          <div className="text-lg font-bold text-ink">HARDWARE AI</div>
          <div className="text-sm text-ink2">{t('login.title')}</div>
        </div>
        <div>
          <label className="mb-1 block text-xs text-ink2">{t('login.email')}</label>
          <Input
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs text-ink2">{t('login.password')}</label>
          <Input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
          />
        </div>
        {error && (
          <div className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</div>
        )}
        <Button type="submit" variant="ai" className="w-full" disabled={busy}>
          {busy ? t('login.submitting') : t('login.submit')}
        </Button>
      </form>
    </div>
  );
}
