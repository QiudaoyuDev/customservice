import { useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import i18n, { LANGS, langNames } from '../i18n';
import { useAuth } from '../lib/auth';
import { useTenant, TenantProvider } from '../lib/tenant';
import { Avatar } from '../components/ui';

interface NavItem {
  to: string;
  labelKey: string;
  icon: string;
}
const GROUPS: { titleKey: string; items: NavItem[] }[] = [
  {
    titleKey: 'console.groups.ops',
    items: [
      { to: 'products', labelKey: 'console.nav.products', icon: '▦' },
      { to: 'qrs', labelKey: 'console.nav.qrs', icon: '▣' },
      { to: 'documents', labelKey: 'console.nav.documents', icon: '❏' },
      { to: 'search', labelKey: 'console.nav.search', icon: '⌕' },
    ],
  },
  {
    titleKey: 'console.groups.diag',
    items: [
      { to: 'flows', labelKey: 'console.nav.flows', icon: '⤳' },
      { to: 'handoffs', labelKey: 'console.nav.handoffs', icon: '🧑' },
      { to: 'models', labelKey: 'console.nav.models', icon: '⚙' },
      { to: 'analytics', labelKey: 'console.nav.analytics', icon: '◷' },
    ],
  },
];

export default function ConsoleLayout() {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const { allTenants, currentTenantId, setTenant } = useTenant();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);

  const activeKey = GROUPS.flatMap((g) => g.items).find((it) =>
    location.pathname.includes('/' + it.to),
  )?.labelKey;

  const sidebar = (
    <div className="flex h-full flex-col bg-gradient-to-b from-brand-800 to-brand-700 p-3 text-white">
      <div className="flex items-center gap-2.5 px-2 py-3">
        <div className="grid h-8 w-8 place-items-center rounded-xl bg-gradient-to-br from-ai-500 to-white font-display font-extrabold text-brand-700">
          H
        </div>
        <div className="leading-tight">
          <div className="font-display text-sm font-extrabold">HARDWARE AI</div>
          <div className="font-mono text-[10px] text-brand-100/80">售后诊断控制台</div>
        </div>
      </div>

      <nav className="mt-2 flex-1">
        {GROUPS.map((g) => (
          <div key={g.titleKey} className="mb-3">
            <div className="px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-brand-100/70">
              {t(g.titleKey)}
            </div>
            {g.items.map((it) => (
              <NavLink
                key={it.to}
                to={`/console/${it.to}`}
                onClick={() => setMobileOpen(false)}
                className={({ isActive }) =>
                  clsxNav(
                    isActive ||
                      location.pathname === `/console/${it.to}` ||
                      (it.to === 'products' && location.pathname === '/console'),
                  )
                }
              >
                <span className="w-5 text-center">{it.icon}</span>
                <span>{t(it.labelKey)}</span>
              </NavLink>
            ))}
          </div>
        ))}
      </nav>

      <div className="mt-auto border-t border-white/10 pt-3">
        <div className="flex items-center gap-2 px-2">
          <Avatar name={user?.email?.[0]?.toUpperCase() ?? 'U'} size={30} />
          <div className="min-w-0 leading-tight">
            <div className="truncate text-sm font-semibold">{user?.email ?? 'user'}</div>
            <div className="text-[11px] text-brand-100/80">{t('console.tenant')}</div>
          </div>
        </div>
        <button
          onClick={logout}
          className="mt-2 w-full rounded-lg border border-white/15 px-2 py-1.5 text-left text-xs text-brand-100 transition hover:bg-white/10"
        >
          {t('console.logout')} ↩
        </button>
      </div>
    </div>
  );

  return (
    <TenantProvider>
    <div className="flex h-screen overflow-hidden">
      <aside className="hidden w-60 flex-none md:block">{sidebar}</aside>

      {mobileOpen && (
        <div className="fixed inset-0 z-40 md:hidden">
          <div className="absolute inset-0 bg-black/40" onClick={() => setMobileOpen(false)} />
          <div className="absolute left-0 top-0 h-full w-60">{sidebar}</div>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="glass sticky top-0 z-30 flex items-center gap-3 border-b border-line px-5 py-3">
          <button
            className="grid h-9 w-9 place-items-center rounded-lg border border-line text-ink2 md:hidden"
            onClick={() => setMobileOpen((v) => !v)}
            aria-label="menu"
          >
            ☰
          </button>
          <div className="flex items-center gap-2 text-sm">
            <span className="text-ink2">{t('console.title')}</span>
            {activeKey && (
              <>
                <span className="text-ink3">›</span>
                <span className="font-semibold text-ink">{t(activeKey)}</span>
              </>
            )}
          </div>

          <div className="ml-auto flex items-center gap-3">
            <span className="hidden items-center gap-1.5 text-xs font-semibold text-ok sm:flex">
              <span className="h-2 w-2 rounded-full bg-ok shadow-[0_0_0_3px] shadow-ok-bg" />
              {t('ui.healthy')}
            </span>

            {allTenants.length > 1 && (
              <select
                value={currentTenantId}
                onChange={(e) => setTenant(e.target.value)}
                className="h-9 rounded-lg border border-line bg-white px-2 text-sm text-ink"
                aria-label={t('console.tenant')}
              >
                {allTenants.map((tn) => (
                  <option key={tn.id} value={tn.id}>
                    {tn.name}
                  </option>
                ))}
              </select>
            )}

            <select
              value={i18n.language}
              onChange={(e) => i18n.changeLanguage(e.target.value)}
              className="h-9 rounded-lg border border-line bg-white px-2 text-sm text-ink"
              aria-label={t('console.language')}
            >
              {LANGS.map((l) => (
                <option key={l} value={l}>
                  {langNames[l]}
                </option>
              ))}
            </select>
          </div>
        </header>

        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
    </TenantProvider>
  );
}

function clsxNav(active: boolean) {
  return [
    'flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-semibold transition',
    active
      ? 'bg-ai-500/15 text-white'
      : 'text-brand-100/90 hover:bg-white/10 hover:text-white',
  ].join(' ');
}
