import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';

// 开发服务器：将 /api 与 /public 代理到本地后端 (Spring Boot, 默认 8080)。
// 这样前端在 5173 端口开发时，fetch('/api/...') 会转发到后端，无需 CORS 配置。
export default defineConfig({
    plugins: [react()],
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
            '/public': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
        },
    },
});
