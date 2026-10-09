import { queryOptions } from '@tanstack/react-query';
import type { components, operations } from '@pet/api-contracts';
import type { AuthService } from '../auth/api/AuthService';
import { authKeys } from '../auth/api/queryKeys';
import type { AuthSpace, WebIdentity } from '../../shared/auth/spaces';
import { hasPermission } from '../../shared/auth/permissions';
import type { RequestOptions } from '../../shared/api/request';
import { parseWebSearch } from '../identity/users/queries/search';
export type Tenant = components['schemas']['TenantView'];
export type Account = components['schemas']['ControlAccountView'];
export type Store = components['schemas']['ControlStoreView'];
export type Result = components['schemas']['ControlMutation'];
export type TenantPage = components['schemas']['PageResponseTenantView'];
export type AccountPage = components['schemas']['PageResponseControlAccountView'];
export type Search = Required<Pick<NonNullable<operations['listControlTenants']['parameters']['query']>, 'page' | 'pageSize' | 'sortBy' | 'sortOrder'>> & { keyword?: string; status?: 'ACTIVE' | 'DISABLED' };
export function controlSearch(raw: string, account = false): Search {
  const p = parseWebSearch(raw), fields = account ? ['loginName', 'displayName', 'status', 'createdAt'] : ['code', 'name', 'status', 'createdAt'];
  const number = (v: unknown, fallback: number, max: number) => typeof v === 'string' && /^[1-9][0-9]{0,9}$/.test(v) && Number(v) <= max ? Number(v) : typeof v === 'number' && Number.isInteger(v) && v > 0 && v <= max ? v : fallback;
  return { page: number(p.page, 1, 2147483647), pageSize: number(p.pageSize, 20, 100), sortBy: typeof p.sortBy === 'string' && fields.includes(p.sortBy) ? p.sortBy : 'createdAt', sortOrder: p.sortOrder === 'asc' ? 'asc' : 'desc', keyword: typeof p.keyword === 'string' && p.keyword.trim() && [...p.keyword].length <= 100 ? p.keyword : undefined, status: p.status === 'ACTIVE' || p.status === 'DISABLED' ? p.status : undefined };
}
export function listHref(path: string, q: Search) { const p = new URLSearchParams(); for (const [key, value] of Object.entries(q)) if (value !== undefined) p.set(key, String(value)); return path + '?' + p; }
export function returnHref(raw: string, path: string, account = false) { const value = new URLSearchParams(raw).get('returnTo'); return value?.startsWith(path + '?') ? listHref(path, controlSearch(value.slice(path.length), account)) : listHref(path, controlSearch('', account)); }
export const detailHref = (path: string, id: string, search: Search) => `${path}/${id}?returnTo=${encodeURIComponent(listHref(path, search))}`;
export function options<T>(auth: AuthService, identity: WebIdentity, space: AuthSpace, resource: string, permission: string, path: string, query?: RequestOptions['query']) {
  const epoch = auth.runtime.epoch(space);
  return queryOptions({ queryKey: authKeys.protected(identity, epoch, space === 'PLATFORM' ? 'control' : 'identity', resource, query ? 'list' : 'detail', { path, ...query }), enabled: hasPermission(identity, space, permission) && !auth.runtime.isBusy(space), networkMode: 'always', retry: false, staleTime: 0, gcTime: 0, queryFn: ({ signal }) => { auth.runtime.assertCurrent(space, epoch); return auth.runtime.request<T>(space, path, { signal, query }); } });
}
export const permissionNames: Record<string, string> = {
  'platform:session:manage': '本人会话管理', 'platform:credential:change': '本人改密', 'platform:redis:operate': '受控缓存操作',
  ...Object.fromEntries(['tenant', 'account'].flatMap(n => Object.entries({ list: '查看列表', detail: '查看详情', create: '创建', update: '修改资料', enable: '启用', disable: '停用', grant: '配置权限', 'reset-password': '重置密码', 'revoke-sessions': '撤销会话' }).map(([a, name]) => [`platform:${n}:${a}`, `${n === 'tenant' ? '租户' : '平台账号'}：${name}`]))),
  'platform:store:control-list': '查看门店目录', 'platform:store:control-create': '创建门店元数据', 'platform:store:control-update': '修改门店名称', 'platform:store:control-status': '启停门店',
};
