import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, clearToken, setToken, setUnauthorizedHandler } from './api';

export interface AuthUser {
  email: string;
  tenantName: string;
}

interface AuthValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
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
  const navigate = useNavigate();

  // 注册「未授权/无权限」处理器：清理本地状态并跳回登录页，形成闭环。
  useEffect(() => {
    setUnauthorizedHandler(() => {
      clearToken();
      try {
        localStorage.removeItem('auth');
      } catch {
        /* ignore */
      }
      setUser(null);
      navigate('/login', { replace: true });
    });
    return () => setUnauthorizedHandler(null);
  }, [navigate]);

  const finishLogout = () => {
    clearToken();
    try {
      localStorage.removeItem('auth');
    } catch {
      /* ignore */
    }
    setUser(null);
    navigate('/login', { replace: true });
  };

  const login = async (email: string, password: string) => {
    const response = await api('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    if (!response?.accessToken || !response?.email) throw new Error('Login response is invalid');
    setToken(response.accessToken);
    const u: AuthUser = { email: response.email, tenantName: 'Current tenant' };
    try {
      localStorage.setItem('auth', JSON.stringify(u));
    } catch {
      /* ignore */
    }
    setUser(u);
  };

  const logout = async () => {
    // 调用登出接口（无状态 JWT，best-effort），随后清理本地状态并跳转登录页。
    try {
      await api('/auth/logout', { method: 'POST' });
    } catch {
      /* 即使接口失败也继续本地清理 */
    }
    finishLogout();
  };

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within <AuthProvider>');
  return ctx;
}
