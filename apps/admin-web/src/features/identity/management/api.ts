import { queryOptions } from '@tanstack/react-query';
import type { components } from '@pet/api-contracts';
import type { AuthService } from '../../auth/api/AuthService';
import { authKeys } from '../../auth/api/queryKeys';
import type { WebIdentity } from '../../../shared/auth/spaces';
import { hasPermission } from '../../../shared/auth/permissions';
import type { RequestOptions } from '../../../shared/api/request';
import { ApiError, errorText } from '../../../shared/api/ApiError';
export type Management = components['schemas']['EmployeeManagement'];
export type Role = components['schemas']['RoleSummary'];
export type RoleDetail = components['schemas']['RoleDetail'];
export type Store = components['schemas']['StoreOption'];
export type Permission = components['schemas']['PermissionOption'];
export type MutationResult = components['schemas']['MutationResult'];
function options<T>(auth: AuthService, identity: WebIdentity, resource: string, kind: string, permission: string, path: string, params: object = {}, query?: RequestOptions['query']) {
  const epoch = auth.runtime.epoch('STAFF');
  return queryOptions({ queryKey: authKeys.protected(identity, epoch, 'identity', resource, kind, params),
    enabled: hasPermission(identity, 'STAFF', permission) && !auth.runtime.isBusy('STAFF'),
    networkMode: 'always', staleTime: 0, gcTime: 0, retry: false,
    queryFn: ({ signal }) => { auth.runtime.assertCurrent('STAFF', epoch); return auth.runtime.request<T>('STAFF', path, { signal, query }); },
  });
}
export const managementOptions = (auth: AuthService, identity: WebIdentity, id: string) => options<Management>(auth, identity, 'employees', 'management', 'identity:user:detail', `/admin/identity/users/${id}/management`, { id });
export const roleOptions = (auth: AuthService, identity: WebIdentity, id: string) => options<RoleDetail>(auth, identity, 'roles', 'detail', 'identity:role:detail', `/admin/identity/roles/${id}`, { id });
export const permissionOptions = (auth: AuthService, identity: WebIdentity) => options<Permission[]>(auth, identity, 'permissions', 'options', 'identity:role:detail', '/admin/identity/permissions');
export const rolePageOptions = (auth: AuthService, identity: WebIdentity, page: number) => options<components['schemas']['PageResponseRoleSummary']>(auth, identity, 'roles', 'list', 'identity:role:list', '/admin/identity/roles', { page }, { page, pageSize: 20 });
export function assignmentOptions(auth: AuthService, identity: WebIdentity, stores: boolean) {
  const epoch = auth.runtime.epoch('STAFF');
  return queryOptions({ queryKey: authKeys.protected(identity, epoch, 'identity', stores ? 'stores' : 'roles', 'assignment-options'),
    enabled: hasPermission(identity, 'STAFF', stores ? 'platform:store:list' : 'identity:role:list'), retry: false, gcTime: 0,
    queryFn: async ({ signal }) => {
      const result: (Role | Store)[] = [];
      for (let page = 1; ; page++) {
        auth.runtime.assertCurrent('STAFF', epoch);
        const data = await auth.runtime.request<components['schemas']['PageResponseStoreOption'] | components['schemas']['PageResponseRoleSummary']>('STAFF', stores ? '/admin/platform/stores/options' : '/admin/identity/roles', { signal, query: { page, pageSize: 100 } });
        if (!data || !Array.isArray(data.items) || !/^(0|[1-9][0-9]*)$/.test(data.total)) throw new ApiError('PROTOCOL', '授权选项响应格式异常');
        result.push(...data.items);
        if (BigInt(page) * 100n >= BigInt(data.total)) break;
        if (page === 2147483647) throw new ApiError('PROTOCOL', '授权选项数量超过可读取范围');
      }
      return result;
    },
  });
}
export function managementFieldErrors<const F extends string>(error: unknown, fields: readonly F[]) {
  return error instanceof ApiError ? (error.fieldErrors ?? []).filter(field => fields.some(name => name === field.field)).map(field => ({ name: field.field as F, errors: [field.message] })) : [];
}
export const uncertain = (error: unknown) => error instanceof ApiError && (['NETWORK', 'TIMEOUT', 'PROTOCOL'].includes(error.kind) || (error.status ?? 0) >= 500 || error.status === 409);

export const requiresRecheck = (error: unknown) => uncertain(error) || (error instanceof ApiError && error.status === 403);

type GrantFieldError = { name: ['grants'] | ['grants', number, 'permissionCode'] | ['grants', number, 'scopeType']; errors: string[] };
export function roleGrantFieldErrors(error: unknown) {
  if (!(error instanceof ApiError)) return [];
  return (error.fieldErrors ?? []).flatMap<GrantFieldError>(field => {
    if (field.field === 'grants') return [{ name: ['grants'] as ['grants'], errors: [field.message] }];
    const match = /^grants\[([0-9]{1,2})\]\.(permissionCode|scopeType)$/.exec(field.field);
    return match ? [{ name: ['grants', Number(match[1]), match[2]] as ['grants', number, 'permissionCode'] | ['grants', number, 'scopeType'], errors: [field.message] }] : [];
  });
}

export function managementNameError(value: string | undefined): string | undefined {
  if (!value?.trim() || [...value.trim()].length > 100 || /[\u0000-\u001f\u007f-\u009f]/.test(value)) return '名称须为1至100个有效字符';
}
export const managementNameRule = { validator: (_: unknown, value: string | undefined) => { const error = managementNameError(value); return error ? Promise.reject(new Error(error)) : Promise.resolve(); } };

export const managementErrorMessages = (error: unknown) => [errorText(error), ...(error instanceof ApiError ? (error.fieldErrors ?? []).map(field => field.message) : [])];
