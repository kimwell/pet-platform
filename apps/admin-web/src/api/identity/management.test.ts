import { describe, expect, it } from 'vitest';
import { ApiError } from '../ApiError';
import { managementFieldErrors, uncertain, managementOptions, roleOptions, roleGrantFieldErrors, managementNameError, rolePageOptions, requiresRecheck } from './management';
import { RequestClient } from '../request';
import { AuthService } from '../auth/AuthService';
import { QueryClient } from '@tanstack/react-query';
import type { WebIdentity } from '../../utils/auth/spaces';
const identity = { principalType: 'STAFF', principalId: 'subject', tenantId: 'tenant', displayName: '员工', sessionId: 'session',
  authorizationVersion: '4', authorizedStoreIds: [], permissionCodes: ['identity:user:detail'], dataScope: {}, passwordChangeRequired: false } as unknown as WebIdentity;
describe('管理表单的协议与身份边界', () => {
  it('字段错误只映射明确白名单，不解析任意路径', () => {
    const error = new ApiError('HTTP', '校验失败', 422, 'VALIDATION_FAILED', undefined, [
      { field: 'displayName', code: 'INVALID', message: '姓名无效' }, { field: 'grants[0].scopeType', code: 'INVALID', message: '范围无效' },
    ]);
    expect(roleGrantFieldErrors(error)).toEqual([{ name: ['grants', 0, 'scopeType'], errors: ['范围无效'] }]);
    expect(managementFieldErrors(error, ['displayName'])).toEqual([{ name: 'displayName', errors: ['姓名无效'] }]);
  });
  it.each([new ApiError('NETWORK', '网络不可用'), new ApiError('TIMEOUT', '请求超时'), new ApiError('HTTP', '版本冲突', 409), new ApiError('HTTP', '依赖不可用', 503)])('结果未确认或冲突禁止直接再次提交：%s', error => {
    expect(uncertain(error)).toBe(true);
  });
  it('中文名称按码点验证并拒绝控制字符', () => { expect(managementNameError('😀'.repeat(100))).toBeUndefined(); expect(managementNameError('😀'.repeat(101))).toBeDefined(); expect(managementNameError('姓名\n')).toBeDefined(); });
  it('确定的权限403保留草稿但锁定写入，字段422可修正', () => { expect(requiresRecheck(new ApiError('HTTP', '无权限', 403))).toBe(true); expect(requiresRecheck(new ApiError('HTTP', '字段无效', 422))).toBe(false); });
  it('明确的输入拒绝允许修正字段', () => { expect(uncertain(new ApiError('HTTP', '姓名无效', 422))).toBe(false); });
  it('角色分页通过请求层query参数传输，不把查询拼进路径', async () => {
    let observed: string | undefined;
    const fetcher: typeof fetch = async input => { observed = String(input); return new Response(JSON.stringify({ success: true, data: { items: [], page: 2, pageSize: 20, total: '0' }, traceId: '0123456789abcdef0123456789abcdef' }), { headers: { 'Content-Type': 'application/json' } }); };
    const client = new QueryClient(), auth = new AuthService(client, new RequestClient(fetcher));
    await client.fetchQuery(rolePageOptions(auth, { ...identity, permissionCodes: ['identity:role:list'] }, 2));
    expect(observed).toBe('/api/admin/identity/roles?page=2&pageSize=20');
  });
  it('管理查询包含目标及身份代际，角色读取不借员工详情权限', () => {
    const auth = new AuthService(new QueryClient());
    const first = managementOptions(auth, identity, 'one'), second = managementOptions(auth, identity, 'two');
    expect(first.queryKey).not.toEqual(second.queryKey);
    expect(first.queryKey).not.toEqual(managementOptions(auth, { ...identity, authorizationVersion: '5' }, 'one').queryKey);
    expect(first.enabled).toBe(true); expect(roleOptions(auth, identity, 'role').enabled).toBe(false);
  });
});
