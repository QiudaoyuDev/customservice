import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import '@fontsource-variable/sora';
import '@fontsource-variable/manrope';
import '@fontsource-variable/jetbrains-mono';
import './index.css';
import './i18n';
import { AuthProvider } from './lib/auth';
import { RequireAuth } from './lib/RequireAuth';
import { Root } from './app/Root';
import Login from './pages/Login';
import SupportPage from './pages/SupportPage';
import ConsoleLayout from './console/ConsoleLayout';
import ProductsPage from './console/ProductsPage';
import QrsPage from './console/QrsPage';
import DocumentsPage from './console/DocumentsPage';
import SearchPage from './console/SearchPage';
import HandoffsPage from './console/HandoffsPage';
import FlowsPage from './console/FlowsPage';
import ModelConfigsPage from './console/ModelConfigsPage';
import AnalyticsPage from './console/AnalyticsPage';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/support/:qrToken" element={<SupportPage />} />
          <Route path="/login" element={<Login />} />
          <Route
            path="/console"
            element={
              <RequireAuth>
                <ConsoleLayout />
              </RequireAuth>
            }
          >
            <Route index element={<Navigate to="products" replace />} />
            <Route path="products" element={<ProductsPage />} />
            <Route path="qrs" element={<QrsPage />} />
            <Route path="documents" element={<DocumentsPage />} />
            <Route path="search" element={<SearchPage />} />
            <Route path="flows" element={<FlowsPage />} />
            <Route path="handoffs" element={<HandoffsPage />} />
            <Route path="models" element={<ModelConfigsPage />} />
            <Route path="analytics" element={<AnalyticsPage />} />
          </Route>
          <Route path="/" element={<Root />} />
          <Route path="*" element={<Root />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
);
