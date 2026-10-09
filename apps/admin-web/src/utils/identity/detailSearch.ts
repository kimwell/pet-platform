import { employeeHref, employeePath, normalizeSearch, parseWebSearch, stringifyWebSearch } from './search';

export const employeeDetailPath = '/admin/identity/users/$employeeId';
export function isEmployeeId(value: unknown): value is string {
  return typeof value === 'string' && /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(value);
}
/** 仅接受员工列表和已有查询白名单；非法、重复或未知参数整组拒绝。 */
export function listReturnTo(input: unknown): string | undefined {
  if (typeof input !== 'string' || input.length > 2048 || input.split('?')[0] !== employeePath || /[\\\u0000-\u0020]/.test(input)) return;
  try {
    const url = new URL(input, 'https://local.invalid');
    if (url.origin !== 'https://local.invalid' || url.pathname !== employeePath || url.hash) return;
    const parsed = normalizeSearch(parseWebSearch(url.search));
    if (parsed.notice) return;
    return employeeHref(parsed.query);
  } catch { return; }
}
export function detailSearch(input: Record<string, unknown>): { returnTo?: string } {
  const returnTo = listReturnTo(input.returnTo);
  return returnTo ? { returnTo } : {};
}
export function employeeDetailHref(employeeId: string, returnTo?: string): string {
  return `${employeePath}/${encodeURIComponent(employeeId)}` + stringifyWebSearch(detailSearch({ returnTo }));
}
