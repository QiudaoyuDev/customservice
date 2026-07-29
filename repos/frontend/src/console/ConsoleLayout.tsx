import { ReactNode } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth';
import { LANGS, langNames, useTranslation } from '../i18n';

const NAV = [
  { to: '/console/products', key: 'console.nav.products' },
  { to: '/console/qrs', key: 'console.nav.qrs' },
  { to: '/console/documents', key: 'console.nav.documents' },
  { to: '/console/search', key: 'console.nav.search' },
  { to: '/console/flows', key: 'console.nav.flows' },
  { to: '/console/handoffs', key: 'console.nav.handoffs' },
  { to: '/console/models', key: 'console.nav.models' },
  { to: '/console/analytics', key: 'console.nav.analytics' },
];

export default function ConsoleLayout() {
  const { logout, user } = useAuth();
  const navigate = useNavigate();
  const { t, i18n } = useTranslation();

  const onLogout = () => {
    logout();
    navigate('/');
  };

  const onLangChange = (l: string) => {
    i18n.changeLanguage(l);
  };

  return (
    <div className="flex h-screen">
      <aside className="flex w-56 shrink-0 flex-col border-r border-line bg-white">
        <div className="border-b border-line px-4 py-4">
          <div className="text-sm font-bold text-ink">HARDWARE AI</div>
          <div className="text-xs text-ink2">{t('console.title')}</div>
        </div>
        <nav className="flex-1 p-2">
          {NAV.map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              className={({ isActive }) =>
                `block rounded-lg px-3 py-2 text-sm ${isActive ? 'bg-ai-soft text-ai font-semibold' : 'text-ink2 hover:bg-slate-50'}`
              }
            >
              {t(n.key)}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-line p-2">
          <label className="mb-1 block text-xs text-ink2">{t('console.language')}</label>
          <select
            className="w-full rounded border border-line px-2 py-1.5 text-sm"
            value={i18n.language}
            onChange={(e) => onLangChange(e.target.value)}
          >
            {LANGS.map((l) => (
              <option key={l} value={l}>
                {langNames[l] ?? l}
              </option>
            ))}
          </select>
        </div>
      </aside>

      <div className="flex flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-line bg-white px-6 py-3">
          <div className="text-sm font-semibold text-ink">
            {user?.tenantName ?? t('console.tenant')}
          </div>
          <div className="flex items-center gap-3">
            <span className="text-xs text-ink2">{user?.email}</span>
            <button
              onClick={onLogout}
              className="rounded border border-line px-3 py-1 text-xs text-ink2 hover:bg-slate-50"
            >
              {t('console.logout')}
            </button>
          </div>
        </header>
        <main className="flex-1 overflow-y-auto bg-slate-50 p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
