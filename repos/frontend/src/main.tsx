import {StrictMode} from 'react';
import {createRoot} from 'react-dom/client';
import {BrowserRouter, Navigate, Route, Routes} from 'react-router-dom';
import './index.css';
import {I18nProvider} from './i18n';
import {AuthProvider} from './lib/auth';
import {Root} from './app/Root';
import Login from './pages/Login';
import SupportPage from './pages/SupportPage';
import ConsoleLayout from './console/ConsoleLayout';
import ProductsPage from './console/ProductsPage';
import QrsPage from './console/QrsPage';
import DocumentsPage from './console/DocumentsPage';
import SearchPage from './console/SearchPage';
import HandoffsPage from './console/HandoffsPage';
import FlowsPage from './console/FlowsPage';

createRoot(document.getElementById('root')!).render(
    <StrictMode>
    <AuthProvider>
    <I18nProvider>
      <BrowserRouter>
                <Routes>
                    <Route path="/support/:qrToken" element={<SupportPage/>}/>
                    <Route path="/login" element={<Login/>}/>
                    <Route path="/console" element={<ConsoleLayout/>}>
                        <Route index element={<Navigate to="products" replace/>}/>
                        <Route path="products" element={<ProductsPage/>}/>
                        <Route path="qrs" element={<QrsPage/>}/>
                        <Route path="documents" element={<DocumentsPage/>}/>
                        <Route path="search" element={<SearchPage/>}/>
                        <Route path="flows" element={<FlowsPage/>}/>
                        <Route path="handoffs" element={<HandoffsPage/>}/>
                    </Route>
                    <Route path="/" element={<Root/>}/>
                    <Route path="*" element={<Root/>}/>
                </Routes>
        </BrowserRouter>
    </I18nProvider>
    </AuthProvider>
    </StrictMode>,
);
