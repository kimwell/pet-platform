import type { AuthSpace, WebIdentity } from '../utils/auth/spaces';
import { canManageSelf, hasPermission, isRestricted } from '../utils/auth/permissions';
import { employeeDetailPath } from '../utils/identity/detailSearch';

export type PageAccess = { space: AuthSpace; path: string; title: string; condition: 'session' | 'self-session'; permissions?: readonly string[]; anyPermission?: boolean; allowRestricted: boolean; navigation?: false };
export const sessionPages: readonly PageAccess[] = [
  { space: 'STAFF', path: '/admin', title: '当前身份', condition: 'session', allowRestricted: false },
  { space: 'STAFF', path: '/admin/security', title: '账号安全', condition: 'self-session', allowRestricted: true },
  { space: 'STAFF', path: '/admin/identity/users', title: '员工列表', condition: 'session', permissions: ['identity:user:list', 'identity:user:detail', 'identity:user:create'], anyPermission: true, allowRestricted: false },
  { space: 'STAFF', path: employeeDetailPath, title: '员工详情', condition: 'session', permissions: ['identity:user:detail'], allowRestricted: false, navigation: false },
  { space: 'STAFF', path: '/admin/identity/roles', title: '角色管理', condition: 'session', permissions: ['identity:role:list', 'identity:role:detail', 'identity:role:create'], anyPermission: true, allowRestricted: false },
  { space: 'STAFF', path: '/admin/identity/roles/$roleId', title: '角色详情', condition: 'session', permissions: ['identity:role:detail'], allowRestricted: false, navigation: false },
  { space: 'PLATFORM', path: '/platform', title: '当前身份', condition: 'session', permissions: ['platform:session:manage'], allowRestricted: false },
  { space: 'PLATFORM', path: '/platform/tenants', title: '租户管理', condition: 'session', permissions: ['platform:tenant:list', 'platform:tenant:detail', 'platform:tenant:create'], anyPermission: true, allowRestricted: false },
  { space: 'PLATFORM', path: '/platform/tenants/$tenantId', title: '租户详情', condition: 'session', permissions: ['platform:tenant:detail'], allowRestricted: false, navigation: false },
  { space: 'PLATFORM', path: '/platform/accounts', title: '平台账号管理', condition: 'session', permissions: ['platform:account:list', 'platform:account:detail', 'platform:account:create'], anyPermission: true, allowRestricted: false },
  { space: 'PLATFORM', path: '/platform/accounts/$accountId', title: '平台账号详情', condition: 'session', permissions: ['platform:account:detail'], allowRestricted: false, navigation: false },
  { space: 'STAFF', path: '/admin/identity/organizations', title: '内部组织管理', condition: 'session', permissions: ['identity:organization:list', 'identity:organization:detail', 'identity:organization:create'], anyPermission: true, allowRestricted: false },
  { space: 'STAFF', path: '/admin/identity/organizations/$organizationId', title: '内部组织详情', condition: 'session', permissions: ['identity:organization:detail'], allowRestricted: false, navigation: false },
  { space: 'PLATFORM', path: '/platform/security', title: '账号安全', condition: 'self-session', allowRestricted: false },
];
export function canAccessPage(page: PageAccess, identity: WebIdentity | null | undefined): boolean {
  if (!identity || identity.principalType !== page.space || (isRestricted(identity) && !page.allowRestricted)) return false;
  if (page.condition === 'self-session' && !canManageSelf(identity, page.space, 'logout-all')) return false;
  return !page.permissions || (page.anyPermission ? page.permissions.some(code => hasPermission(identity, page.space, code)) : page.permissions.every(code => hasPermission(identity, page.space, code)));
}
export function navigationFor(space: AuthSpace, identity: WebIdentity | null | undefined): readonly PageAccess[] {
  return sessionPages.filter(page => page.space === space && page.navigation !== false && (!page.anyPermission || hasPermission(identity, space, page.permissions![0])) && canAccessPage(page, identity));
}
export function pageFor(space: AuthSpace, path: string): PageAccess {
  const page = sessionPages.find(item => item.space === space && (item.path === (path.endsWith('/') ? path.slice(0, -1) : path) || (item.path.includes('$') && new RegExp('^' + item.path.replace(/\$[A-Za-z]+/g, '[^/]+') + '$').test(path)) || (item.path === '/admin/identity/roles/$roleId' && /^\/admin\/identity\/roles\/[^/]+$/.test(path)) || (item.path === employeeDetailPath && /^\/admin\/identity\/users\/[^/]+$/.test(path))));
  if (!page) throw new Error('页面未登记访问条件');
  return page;
}
