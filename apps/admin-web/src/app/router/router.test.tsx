import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';
import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it } from 'vitest';
import { routeTree } from './router';
import { AppProviders } from '../providers/AppProviders';
import { SystemEntry } from '../../shared/SystemEntry';

describe('工程入口路由', () => {
  it('匹配入口并把未知路径交给 NotFound', async () => {
    const router = createRouter({ routeTree, history: createMemoryHistory({ initialEntries: ['/'] }), isServer: true });
    await router.load();
    expect(router.state.matches.at(-1)?.routeId).toBe('/');
    const unknownRouter = createRouter({ routeTree, history: createMemoryHistory({ initialEntries: ['/missing'] }), isServer: true });
    await unknownRouter.load();
    const markup = renderToStaticMarkup(<AppProviders><RouterProvider router={unknownRouter} /></AppProviders>);
    expect(markup).toContain('页面不存在');
    expect(markup).toContain('href="/"');
  });

  it('官方中文配置和 Query provider 下能渲染入口', () => {
    const markup = renderToStaticMarkup(<AppProviders><SystemEntry /></AppProviders>);
    expect(markup).toContain('<h1');
    expect(markup).toContain('role="alert"');
  });
});
