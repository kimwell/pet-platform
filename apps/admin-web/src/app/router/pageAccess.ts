import type { AuthSpace, WebIdentity } from '../../shared/auth/spaces';
import { canManageSelf, hasPermission, isRestricted } from '../../shared/auth/permissions';

export type PageAccess = { space: AuthSpace; path: string; title: string; condition: 'session' | 'self-session'; permissions?: readonly string[]; allowRestricted: boolean };
export const sessionPages: readonly PageAccess[] = [
  { space: 'STAFF', path: '/admin', title: '当前身份', condition: 'session', allowRestricted: false },
  { space: 'STAFF', path: '/admin/security', title: '账号安全', condition: 'self-session', allowRestricted: true },
  { space: 'PLATFORM', path: '/platform', title: '当前身份', condition: 'session', permissions: ['platform:session:manage'], allowRestricted: false },
  { space: 'PLATFORM', path: '/platform/security', title: '账号安全', condition: 'self-session', allowRestricted: false },
];
export function canAccessPage(page: PageAccess, identity: WebIdentity | null | undefined): boolean {
  if (!identity || identity.principalType !== page.space || (isRestricted(identity) && !page.allowRestricted)) return false;
  if (page.condition === 'self-session' && !canManageSelf(identity, page.space, 'logout-all')) return false;
  return !page.permissions || page.permissions.every(code => hasPermission(identity, page.space, code));
}
export function navigationFor(space: AuthSpace, identity: WebIdentity | null | undefined): readonly PageAccess[] {
  return sessionPages.filter(page => page.space === space && canAccessPage(page, identity));
}
export function pageFor(space: AuthSpace, path: string): PageAccess {
  const page = sessionPages.find(item => item.space === space && item.path === path);
  if (!page) throw new Error('页面未登记访问条件');
  return page;
}
