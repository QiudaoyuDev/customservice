import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { api } from './api';

export interface Tenant {
  id: string;
  name: string;
}

interface TenantValue {
  allTenants: Tenant[];
  currentTenantId: string;
  setTenant: (id: string) => void;
  loading: boolean;
}

const Ctx = createContext<TenantValue | null>(null);

export function TenantProvider({ children }: { children: ReactNode }) {
  const [allTenants, setAllTenants] = useState<Tenant[]>([]);
  const [currentTenantId, setCurrentTenantId] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const list = (await api('/tenants')) as Tenant[] | undefined;
        const tenants = list ?? [];
        if (!active) return;
        setAllTenants(tenants);
        const stored =
          typeof localStorage !== 'undefined' ? localStorage.getItem('tenantId') : null;
        setCurrentTenantId(
          stored && tenants.some((t) => t.id === stored) ? stored : tenants[0]?.id ?? '',
        );
      } catch {
        if (active) {
          setAllTenants([]);
          setCurrentTenantId('');
        }
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  const setTenant = (id: string) => {
    setCurrentTenantId(id);
    try {
      localStorage.setItem('tenantId', id);
    } catch {
      /* ignore */
    }
  };

  return (
    <Ctx.Provider value={{ allTenants, currentTenantId, setTenant, loading }}>
      {children}
    </Ctx.Provider>
  );
}

export function useTenant(): TenantValue {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error('useTenant must be used within <TenantProvider>');
  return ctx;
}
