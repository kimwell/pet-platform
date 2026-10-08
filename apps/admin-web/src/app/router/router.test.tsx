import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';
import { renderToStaticMarkup, renderToReadableStream } from 'react-dom/server';
import { describe, expect, it, vi } from 'vitest';
import { routeTree } from './router';
import { AppProviders } from '../providers/AppProviders';
import { createServices } from '../../features/auth/api/AuthService';
import { RequestClient } from '../../shared/api/request';
import { fail, reply } from '../../test/responses';

const staff = { principalType: 'STAFF', principalId: 'p', sessionId: 's', tenantId: 't', displayName: '技术夹具', authorizationVersion: '0', permissionCodes: [], authorizedStoreIds: [], dataScope: { grants: [] }, passwordChangeRequired: false };
function testRouter(path: string, fetcher = vi.fn().mockImplementation(async () => fail())) {
  const services = createServices(new RequestClient(fetcher));
  const router = createRouter({ routeTree, context: services, history: createMemoryHistory({ initialEntries: [path] }), isServer: true });
  return { router, services, fetcher };
}

describe('入口、路由守卫与官方组件（受控网络替身）', () => {
  it('公开入口不请求身份，未知路径为NotFound', async () => {
    const { router, services, fetcher } = testRouter('/missing'); await router.load();
    const markup = renderToStaticMarkup(<AppProviders services={services}><RouterProvider router={router} /></AppProviders>);
    expect(markup).toContain('页面不存在'); expect(markup).toContain('href="/"'); expect(fetcher).not.toHaveBeenCalled(); services.queryClient.clear();
  });
  it('系统入口包含两类实际入口', async () => {
    const { router, services } = testRouter('/'); await router.load();
    const markup = renderToStaticMarkup(<AppProviders services={services}><RouterProvider router={router} /></AppProviders>);
    expect(markup).toContain('员工工作台'); expect(markup).toContain('平台控制台'); services.queryClient.clear();
  });
  it.each(['/admin', '/platform'])('直接访问保护页%s先请求对应me再跳登录', async path => {
    const { router, services, fetcher } = testRouter(path); await router.load();
    const result = router._serverResult; expect(result?.type).toBe('redirect');
    if (result?.type === 'redirect') expect(result.redirect.headers.get('Location')).toContain(`${path}/login`); expect(fetcher.mock.calls.every(([url]) => String(url).startsWith(`/api${path}/`))).toBe(true); services.queryClient.clear();
  });
  it('有效员工只读取员工域，保护页不使用登录草稿造身份', async () => {
    const { router, services, fetcher } = testRouter('/admin', vi.fn().mockResolvedValue(reply(staff))); await router.load();
    expect(router.state.matches.at(-1)?.routeId).toBe('/admin'); expect(router.state.matches.at(-1)?.status).toBe('success'); expect(fetcher.mock.calls[0][0]).toBe('/api/admin/auth/me'); services.queryClient.clear();
  });
  it.each([503, 403])('当前身份%d是服务/权限错误，不跳登录', async status => {
    const { router, services } = testRouter('/admin', vi.fn().mockImplementation(async () => fail(status, status === 503 ? 'DEPENDENCY_UNAVAILABLE' : 'PERMISSION_DENIED'))); await router.load();
    expect(router._serverResult?.type).toBe('render'); expect(router.state.matches.at(-1)?.status).toBe('success');
    const context = router.state.matches.at(-1)?.context;
    expect(context && 'sessionError' in context ? context.sessionError?.status : undefined).toBe(status); services.queryClient.clear();
  });
  it('登录页面使用官方Form、中文label和password autocomplete', async () => {
    const { router, services } = testRouter('/admin/login'); await router.load();
    const stream = await renderToReadableStream(<AppProviders services={services}><RouterProvider router={router} /></AppProviders>); await stream.allReady;
    const markup = await new Response(stream).text();
    expect(markup).toContain('员工登录'); expect(markup).toContain('for="password"'); expect(markup).toContain('autoComplete="current-password"'); expect(markup).not.toContain('记住密码'); services.queryClient.clear();
  });
});
