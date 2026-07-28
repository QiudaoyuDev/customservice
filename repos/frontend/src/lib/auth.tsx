import { createContext, useContext, useState, ReactNode } from 'react';

export interface AuthUser {
  email: string;
  tenantName: string;
}

interface AuthValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (email: string, password?: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthValue | null>(null);

function readStored(): AuthUser | null {
  try {
    const raw = localStorage.getItem('auth');
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(readStored);

  const login = async (email: string) => {
    const u: AuthUser = { email, tenantName: 'Demo Tenant' };
    try {
      localStorage.setItem('auth', JSON.stringify(u));
    } catch {
      /* ignore */
    }
    setUser(u);
  };

  const logout = () => {
    try {
      localStorage.removeItem('auth');
    } catch {
      /* ignore */
    }
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, logout }}>{children}</AuthContext.Provider>
  );
}

export function useAuth(): AuthValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within <AuthProvider>');
  return ctx;
}
