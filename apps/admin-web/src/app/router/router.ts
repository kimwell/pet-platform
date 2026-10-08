import { createRootRouteWithContext, createRoute, createRouter, lazyRouteComponent, redirect } from '@tanstack/react-router';
import { SystemLayout } from '../layout/SystemLayout';
import { SystemEntry } from '../../shared/SystemEntry';
import { SystemError } from '../../shared/SystemError';
import { SystemNotFound } from '../../shared/SystemNotFound';
import { createServices } from '../../features/auth/api/AuthService';
import type { Services } from '../../features/auth/api/AuthService';
import { requireSession } from '../guards/requireSession';
import { safeReturnTo } from '../../shared/auth/returnTo';
import type { AuthSpace } from '../../shared/auth/spaces';
import { ApiError } from '../../shared/api/ApiError';
import { isRestricted } from '../../shared/auth/permissions';
import { spaces } from '../../shared/auth/spaces';

const rootRoute = createRootRouteWithContext<Services>()({ component: SystemLayout, errorComponent: SystemError, notFoundComponent: SystemNotFound });
const entryRoute = createRoute({ getParentRoute: () => rootRoute, path: '/', component: SystemEntry });
const loginGuard = (space: AuthSpace) => async ({ context, search }: { context: Services; search: { returnTo?: string } }) => {
  let identity;
  try { identity = await context.auth.current(space); }
  catch (error) {
    if (error instanceof ApiError && error.kind !== 'CANCELLED') return { sessionError: error };
    throw error;
  }
  if (identity) throw redirect({ href: isRestricted(identity) ? spaces[space].security : safeReturnTo(space, search.returnTo), replace: true });
  return { sessionError: undefined };
};
const staffLogin = createRoute({ getParentRoute: () => rootRoute, path: '/admin/login',
  validateSearch: (search: Record<string, unknown>): { returnTo?: string } => ({ returnTo: typeof search.returnTo === 'string' ? safeReturnTo('STAFF', search.returnTo) : undefined }),
  beforeLoad: loginGuard('STAFF'), component: lazyRouteComponent(() => import('../../features/auth/pages/LoginPage'), 'StaffLoginPage'),
});
const platformLogin = createRoute({ getParentRoute: () => rootRoute, path: '/platform/login',
  validateSearch: (search: Record<string, unknown>): { returnTo?: string } => ({ returnTo: typeof search.returnTo === 'string' ? safeReturnTo('PLATFORM', search.returnTo) : undefined }),
  beforeLoad: loginGuard('PLATFORM'), component: lazyRouteComponent(() => import('../../features/auth/pages/LoginPage'), 'PlatformLoginPage'),
});
const staff = createRoute({ getParentRoute: () => rootRoute, path: '/admin',
  beforeLoad: ({ context, location }) => requireSession(context.auth, 'STAFF', location.href),
  component: lazyRouteComponent(() => import('../layout/SessionLayout'), 'StaffLayout'),
});
const platform = createRoute({ getParentRoute: () => rootRoute, path: '/platform',
  beforeLoad: ({ context, location }) => requireSession(context.auth, 'PLATFORM', location.href),
  component: lazyRouteComponent(() => import('../layout/SessionLayout'), 'PlatformLayout'),
});
const staffSecurity = createRoute({ getParentRoute: () => rootRoute, path: '/admin/security',
  beforeLoad: ({ context, location }) => requireSession(context.auth, 'STAFF', location.href),
  component: lazyRouteComponent(() => import('../layout/SessionLayout'), 'StaffLayout'),
});
const platformSecurity = createRoute({ getParentRoute: () => rootRoute, path: '/platform/security',
  beforeLoad: ({ context, location }) => requireSession(context.auth, 'PLATFORM', location.href),
  component: lazyRouteComponent(() => import('../layout/SessionLayout'), 'PlatformLayout'),
});
export const routeTree = rootRoute.addChildren([entryRoute, staffLogin, platformLogin, staff, platform, staffSecurity, platformSecurity]);
export const services = createServices();
export const router = createRouter({ routeTree, context: services, defaultPreload: false,
  defaultPendingMs: 0, defaultPendingComponent: () => '正在确认当前会话…', defaultStaleTime: 0,
});
// 身份变化时重算路由；过渡中的查询会等待结束。只重算当前路由所属空间。
services.auth.runtime.subscribe(() => {
  const path = router.state.location.pathname;
  const space = path.startsWith('/admin') ? 'STAFF' : path.startsWith('/platform') ? 'PLATFORM' : undefined;
  if (space && !services.auth.runtime.isBusy(space)) void router.invalidate();
});

declare module '@tanstack/react-router' {
  interface Register { router: typeof router }
}
