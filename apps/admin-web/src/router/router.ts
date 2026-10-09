import { useSession } from '../hooks/useSession';
import { createElement } from 'react';
import type { ComponentType } from 'react';
import type { WebIdentity } from '../utils/auth/spaces';
import { usePageIdentity } from '../hooks/usePageIdentity';
import { createRootRouteWithContext, createRoute, createRouter, lazyRouteComponent, redirect } from '@tanstack/react-router';
import { SystemLayout } from '../layouts/SystemLayout';
import { SystemEntry } from '../pages/system/SystemEntry';
import { SystemError } from '../pages/system/SystemError';
import { SystemNotFound } from '../pages/system/SystemNotFound';
import { createServices } from '../api/auth/AuthService';
import type { Services } from '../api/auth/AuthService';
import { requireSession } from './guards/requireSession';
import { safeReturnTo } from '../utils/auth/returnTo';
import type { AuthSpace } from '../utils/auth/spaces';
import { ApiError } from '../api/ApiError';
import { isRestricted } from '../utils/auth/permissions';
import { spaces } from '../utils/auth/spaces';
import { employeeHref, employeeSearchValues, initialEmployeeHistoryState, normalizeSearch, parseWebSearch, stringifyWebSearch } from '../utils/identity/search';
import { controlSearch, listHref, returnHref } from '../api/control';
import { isEmployeeId, listReturnTo } from '../utils/identity/detailSearch';

function authenticatedPage(loader: () => Promise<unknown>, name: string) {
  const Component = lazyRouteComponent(async () => ({ default: (await loader() as Record<string, ComponentType<{ identity: WebIdentity }>>)[name] }));
  return function AuthenticatedPage() { const identity = usePageIdentity(); const session = useSession(identity.principalType, false); const content = createElement(Component, { identity, refreshing: session.isFetching, disabled: session.busy, refresh: () => void session.refetch() } as { identity: WebIdentity }); return name.endsWith('Page') && !name.endsWith('LoginPage') ? createElement('section', { className: 'management-page' }, content) : content; };
}
const legacyGuard = (resource: string, list: string) => async ({ context, location }: { context: Services & { sessionError?: ApiError }; location: { href: string; pathname: string; searchStr: string } }) => {
  if (context.sessionError) return;
  const id = location.pathname.split('/').at(-1) ?? '';
  const query = new URLSearchParams(location.searchStr);
  const href = resource === 'employees' ? listReturnTo(query.get('returnTo')) ?? list : resource === 'roles' || resource === 'organizations' ? list + '?page=' + (/^[1-9][0-9]{0,9}$/.test(query.get('page') ?? '') ? query.get('page') : '1') : returnHref(location.searchStr, list, resource === 'accounts');
  throw redirect({ href, replace: true, state: isEmployeeId(id) ? { legacyModal: { resource, id } } : { legacyLinkNotice: '详情链接中的目标标识无效，请在列表重新选择。' } });
};
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
  beforeLoad: loginGuard('STAFF'), component: lazyRouteComponent(() => import('../pages/auth/LoginPage'), 'StaffLoginPage'),
});
const platformLogin = createRoute({ getParentRoute: () => rootRoute, path: '/platform/login',
  validateSearch: (search: Record<string, unknown>): { returnTo?: string } => ({ returnTo: typeof search.returnTo === 'string' ? safeReturnTo('PLATFORM', search.returnTo) : undefined }),
  beforeLoad: loginGuard('PLATFORM'), component: lazyRouteComponent(() => import('../pages/auth/LoginPage'), 'PlatformLoginPage'),
});
const staff = createRoute({ getParentRoute: () => rootRoute, path: '/admin',
  beforeLoad: ({ context, location }) => requireSession(context.auth, 'STAFF', location.href),
  component: lazyRouteComponent(() => import('../layouts/BasicLayout/index'), 'StaffLayout'),
});
const platform = createRoute({ getParentRoute: () => rootRoute, path: '/platform',
  beforeLoad: ({ context, location }) => requireSession(context.auth, 'PLATFORM', location.href),
  component: lazyRouteComponent(() => import('../layouts/BasicLayout/index'), 'PlatformLayout'),
});
const staffSecurity = createRoute({ getParentRoute: () => staff, path: 'security',
  component: authenticatedPage(() => import('../pages/account/AccountSecurity'), 'AccountSecurity'),
});
const platformSecurity = createRoute({ getParentRoute: () => platform, path: 'security',
  component: authenticatedPage(() => import('../pages/account/AccountSecurity'), 'AccountSecurity'),
});
const staffUsers = createRoute({ getParentRoute: () => staff, path: 'identity/users',
  validateSearch: (search: Record<string, unknown>) => normalizeSearch(search).query,
  search: { middlewares: [({ search, next }) => {
    // 路由可能合并原 search；只序列化允许字段，防止残留未知参数引发二次纠正并覆盖提示。
    return employeeSearchValues(normalizeSearch(next(search)).query) as typeof search;
  }] },
  beforeLoad: async ({ context, location }) => {
    if (context.sessionError) return;
    const parsed = normalizeSearch(parseWebSearch(location.searchStr));
    const canonical = employeeHref(parsed.query);
    if (location.pathname + location.searchStr !== canonical) throw redirect({ href: canonical, replace: true,
      state: { ...location.state, employeeUrlNotice: parsed.notice } });

  }, component: authenticatedPage(() => import('../pages/admin/identity/users/EmployeeListPage'), 'EmployeeListPage'),
});
const staffUserDetail = createRoute({ getParentRoute: () => staff, path: 'identity/users/$employeeId', beforeLoad: legacyGuard('employees', '/admin/identity/users') });
const staffRoles = createRoute({ getParentRoute: () => staff, path: 'identity/roles',
  validateSearch: (search: Record<string, unknown>) => ({ page: typeof search.page === 'string' && /^[1-9][0-9]{0,9}$/.test(search.page) && Number(search.page) <= 2147483647 ? Number(search.page) : typeof search.page === 'number' && Number.isInteger(search.page) && search.page > 0 && search.page <= 2147483647 ? search.page : 1 }),
  component: authenticatedPage(() => import('../pages/admin/identity/roles/RolesPage'), 'RolesPage'),
});
const staffRoleDetail = createRoute({ getParentRoute: () => staff, path: 'identity/roles/$roleId', beforeLoad: legacyGuard('roles', '/admin/identity/roles') });
const platformTenants = createRoute({ getParentRoute: () => platform, path: 'tenants',
  validateSearch: (search: Record<string, unknown>) => search,
  beforeLoad: async ({ context, location }) => {
    if (context.sessionError) return;
    const canonical = listHref('/platform/tenants', controlSearch(location.searchStr, false));
    if (location.pathname + location.searchStr !== canonical) throw redirect({ href: canonical, replace: true, state: location.state });

  },
  component: authenticatedPage(() => import('../pages/platform/tenants/index'), 'TenantsPage'),
});
const platformTenant = createRoute({ getParentRoute: () => platform, path: 'tenants/$tenantId', beforeLoad: legacyGuard('tenants', '/platform/tenants') });
const platformAccounts = createRoute({ getParentRoute: () => platform, path: 'accounts',
  validateSearch: (search: Record<string, unknown>) => search,
  beforeLoad: async ({ context, location }) => {
    if (context.sessionError) return;
    const canonical = listHref('/platform/accounts', controlSearch(location.searchStr, true));
    if (location.pathname + location.searchStr !== canonical) throw redirect({ href: canonical, replace: true, state: location.state });

  },
  component: authenticatedPage(() => import('../pages/platform/accounts/index'), 'AccountsPage'),
});
const platformAccount = createRoute({ getParentRoute: () => platform, path: 'accounts/$accountId', beforeLoad: legacyGuard('accounts', '/platform/accounts') });
const organizations = createRoute({ getParentRoute: () => staff, path: 'identity/organizations',
  validateSearch: (search: Record<string, unknown>) => search,
  component: authenticatedPage(() => import('../pages/admin/identity/organizations/index'), 'OrganizationsPage'),
});
const organization = createRoute({ getParentRoute: () => staff, path: 'identity/organizations/$organizationId', beforeLoad: legacyGuard('organizations', '/admin/identity/organizations') });
const staffHome = createRoute({ getParentRoute: () => staff, path: '/', component: authenticatedPage(() => import('../pages/account/AccountHome'), 'AccountHome') });
const platformHome = createRoute({ getParentRoute: () => platform, path: '/', component: authenticatedPage(() => import('../pages/account/AccountHome'), 'AccountHome') });
export const routeTree = rootRoute.addChildren([entryRoute, staffLogin, platformLogin, staff.addChildren([staffHome, staffSecurity, staffUsers, staffUserDetail, staffRoles, staffRoleDetail, organizations, organization]), platform.addChildren([platformHome, platformSecurity, platformTenants, platformTenant, platformAccounts, platformAccount])]);
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
