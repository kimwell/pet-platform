import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';
import type { ProxyOptions } from 'vite';
import { resolveDevBackendOrigin } from './dev-config';

export default defineConfig(({ mode, command, isPreview }) => {
  const proxy: Record<string, ProxyOptions> = command === 'serve' && !isPreview
    ? { '/api': { target: resolveDevBackendOrigin(loadEnv(mode, process.cwd(), 'DEV_').DEV_BACKEND_ORIGIN), changeOrigin: false } }
    : {};
  return {
    plugins: [react()],
    server: {
      host: '127.0.0.1',
      port: 5173,
      strictPort: true,
      // 仅开发期同源代理，保留 /api 路径；目标不是客户端公开配置。
      proxy
    },
    preview: { host: '127.0.0.1', port: 4173, strictPort: true, proxy: {} }
  };
});
