import { ReactNode } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth';
import { useTranslation } from '../i18n';

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
  const { t } = useTranslation();

  const onLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="flex h-screen">
      <aside className="w-56 shrink-0 border-r border-line bg-white">
        <div className="border-b border-line px-4 py-4">
          <div className="text-sm font-bold text-ink">HARDWARE AI</div>
          <div className="text-xs text-ink2">{t('console.title')}</div>
        </div>
        <nav className="p-2">
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
