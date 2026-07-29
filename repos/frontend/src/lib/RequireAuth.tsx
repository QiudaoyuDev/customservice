import { Navigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from './auth';

/** 路由守卫：未登录（无 token）时重定向到登录页。 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
}
