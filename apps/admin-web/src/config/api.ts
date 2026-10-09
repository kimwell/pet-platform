// 站点根部署；只接受 /api 下的相对路径，开发由 Vite 保留路径代理。
export const apiConfig = { baseUrl: '/api', timeoutMs: 15_000 } as const;
