import { spaces } from './spaces';
import type { AuthSpace } from './spaces';
import { detailSearch, employeeDetailHref, isEmployeeId, listReturnTo } from '../../features/identity/users/queries/detailSearch';
import { parseWebSearch } from '../../features/identity/users/queries/search';

export function safeReturnTo(space: AuthSpace, input: unknown): string {
  const home = spaces[space].path;
  if (typeof input !== 'string' || input.length > 2048 || !input.startsWith('/') || input.startsWith('//') || /[\\\u0000-\u0020]/.test(input)) return home;
  try {
    const url = new URL(input, 'https://local.invalid');
    if (space === 'STAFF' && url.origin === 'https://local.invalid' && !url.hash) {
      if (url.pathname === '/admin/identity/users') return listReturnTo(input) ?? home;
      const id = url.pathname.match(/^\/admin\/identity\/users\/([^/]+)$/)?.[1];
      if (isEmployeeId(id)) return employeeDetailHref(id, detailSearch(parseWebSearch(url.search)).returnTo);
    }
    const paths = [home, spaces[space].security, ...(space === 'STAFF' ? ['/admin/identity/users'] : [])];
    if (url.origin !== 'https://local.invalid' || !paths.includes(url.pathname) || /%|;/.test(url.pathname) || url.hash) return home;
    for (const [key, value] of url.searchParams) {
      if (/password|token|secret|csrf|credential|returnto|redirect/i.test(key) || /(?:bearer\s|[?&](?:token|password)=)/i.test(value)) return home;
    }
    return url.pathname + url.search;
  } catch { return home; }
}
