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
import { employeeHref, employeeSearchValues, initialEmployeeHistoryState, normalizeSearch, parseWebSearch, stringifyWebSearch } from '../../features/identity/users/queries/search';
import { controlSearch, listHref } from '../../features/platform/api';
import { detailSearch, employeeDetailPath } from '../../features/identity/users/queries/detailSearch';

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
const staffUsers = createRoute({ getParentRoute: () => rootRoute, path: '/admin/identity/users',
  validateSearch: (search: Record<string, unknown>) => normalizeSearch(search).query,
  search: { middlewares: [({ search, next }) => {
    // 路由可能合并原 search；只序列化允许字段，防止残留未知参数引发二次纠正并覆盖提示。
    return employeeSearchValues(normalizeSearch(next(search)).query) as typeof search;
  }] },
  beforeLoad: async ({ context, location }) => {
    const access = await requireSession(context.auth, 'STAFF', location.href);
    if (access.sessionError) return access;
    const parsed = normalizeSearch(parseWebSearch(location.searchStr));
    const canonical = employeeHref(parsed.query);
    if (location.pathname + location.searchStr !== canonical) throw redirect({ href: canonical, replace: true,
      state: { employeeUrlNotice: parsed.notice } });
    return access;
  }, component: lazyRouteComponent(() => import('../layout/SessionLayout'), 'StaffLayout'),
});
const staffUserDetail = createRoute({ getParentRoute: () => rootRoute, path: employeeDetailPath,
  validateSearch: detailSearch,
  search: { middlewares: [({ search, next }) => detailSearch(next(search))] },
  beforeLoad: ({ context, location }) => requireSession(context.auth, 'STAFF', location.href),
  component: lazyRouteComponent(() => import('../layout/SessionLayout'), 'StaffLayout'),
});
const staffRoles = createRoute({ getParentRoute: () => rootRoute, path: '/admin/identity/roles',
  validateSearch: (search: Record<string, unknown>) => ({ page: typeof search.page === 'string' && /^[1-9][0-9]{0,9}$/.test(search.page) && Number(search.page) <= 2147483647 ? Number(search.page) : typeof search.page === 'number' && Number.isInteger(search.page) && search.page > 0 && search.page <= 2147483647 ? search.page : 1 }),
  beforeLoad: ({ context, location }) => requireSession(context.auth, 'STAFF', location.href),
  component: lazyRouteComponent(() => import('../layout/SessionLayout'), 'StaffLayout'),
});
const staffRoleDetail = createRoute({ getParentRoute: () => rootRoute, path: '/admin/identity/roles/$roleId',
  beforeLoad: ({ context, location }) => requireSession(context.auth, 'STAFF', location.href),
  component: lazyRouteComponent(() => import('../layout/SessionLayout'), 'StaffLayout'),
});
const platformTenants = createRoute({ getParentRoute: () => rootRoute, path: '/platform/tenants',
  validateSearch: (search: Record<string, unknown>) => search,
  beforeLoad: async ({ context, location }) => {
    const access = await requireSession(context.auth, 'PLATFORM', location.href);
    if (access.sessionError) return access;
    const canonical = listHref('/platform/tenants', controlSearch(location.searchStr, false));
    if (location.pathname + location.searchStr !== canonical) throw redirect({ href: canonical, replace: true });
    return access;
  },
  component: lazyRouteComponent(() => import('../layout/SessionLayout'), 'PlatformLayout'),
});
const platformTenant = createRoute({ getParentRoute: () => rootRoute, path: '/platform/tenants/$tenantId',
  validateSearch: (search: Record<string, unknown>) => search,
  beforeLoad: ({ context, location }) => requireSession(context.auth, 'PLATFORM', location.href),
  component: lazyRouteComponent(() => import('../layout/SessionLayout'), 'PlatformLayout'),
});
const platformAccounts = createRoute({ getParentRoute: () => rootRoute, path: '/platform/accounts',
  validateSearch: (search: Record<string, unknown>) => search,
  beforeLoad: async ({ context, location }) => {
    const access = await requireSession(context.auth, 'PLATFORM', location.href);
    if (access.sessionError) return access;
    const canonical = listHref('/platform/accounts', controlSearch(location.searchStr, true));
    if (location.pathname + location.searchStr !== canonical) throw redirect({ href: canonical, replace: true });
    return access;
  },
  component: lazyRouteComponent(() => import('../layout/SessionLayout'), 'PlatformLayout'),
});
const platformAccount = createRoute({ getParentRoute: () => rootRoute, path: '/platform/accounts/$accountId',
  validateSearch: (search: Record<string, unknown>) => search,
  beforeLoad: ({ context, location }) => requireSession(context.auth, 'PLATFORM', location.href),
  component: lazyRouteComponent(() => import('../layout/SessionLayout'), 'PlatformLayout'),
});
const organizations = createRoute({ getParentRoute: () => rootRoute, path: '/admin/identity/organizations',
  validateSearch: (search: Record<string, unknown>) => search,
  beforeLoad: ({ context, location }) => requireSession(context.auth, 'STAFF', location.href),
  component: lazyRouteComponent(() => import('../layout/SessionLayout'), 'StaffLayout'),
});
const organization = createRoute({ getParentRoute: () => rootRoute, path: '/admin/identity/organizations/$organizationId',
  validateSearch: (search: Record<string, unknown>) => search,
  beforeLoad: ({ context, location }) => requireSession(context.auth, 'STAFF', location.href),
  component: lazyRouteComponent(() => import('../layout/SessionLayout'), 'StaffLayout'),
});
export const routeTree = rootRoute.addChildren([entryRoute, staffLogin, platformLogin, staff, platform, staffSecurity, platformSecurity, staffUsers, staffUserDetail, staffRoles, staffRoleDetail, platformTenants, platformTenant, platformAccounts, platformAccount, organizations, organization]);
export const services = createServices();
export const router = createRouter({ routeTree, context: services, defaultPreload: false,
  parseSearch: parseWebSearch, stringifySearch: stringifyWebSearch,
  defaultPendingMs: 0, defaultPendingComponent: () => '正在确认当前会话…', defaultStaleTime: 0,
});
if (typeof document !== 'undefined') {
  const location = router.history.location;
  const state = initialEmployeeHistoryState(location.href, location.state);
  if (state !== location.state) router.history.replace(location.href, state);
}
// 身份变化时重算路由；过渡中的查询会等待结束。只重算当前路由所属空间。
services.auth.runtime.subscribe(() => {
  const path = router.state.location.pathname;
  const space = path.startsWith('/admin') ? 'STAFF' : path.startsWith('/platform') ? 'PLATFORM' : undefined;
  if (space && !services.auth.runtime.isBusy(space)) void router.invalidate();
});

declare module '@tanstack/react-router' {
  interface Register { router: typeof router }
}
