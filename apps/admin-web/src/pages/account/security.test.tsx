import { describe, expect, it, vi } from 'vitest';
import { QueryClient } from '@tanstack/react-query';
import { createMemoryHistory, createRouter } from '@tanstack/react-router';
import { routeTree } from '../../router/router';
import { canAccessPage, navigationFor, pageFor } from '../../router/pageAccess';
import { canManageSelf, hasPermission } from '../../utils/auth/permissions';
import { ApiError } from '../../api/ApiError';
import { RequestClient } from '../../api/request';
import type { WebIdentity } from '../../utils/auth/spaces';
import { safeReturnTo } from '../../utils/auth/returnTo';
import { AuthService, createServices } from '../../api/auth/AuthService';
import { authKeys } from '../../api/auth/queryKeys';
import { fail, reply } from '../../test/responses';
import { confirmationError, passwordError, securityFieldErrors, securityErrorText, securityUnmappedErrors } from './securityForm';

const staff: WebIdentity = { principalType: 'STAFF', principalId: 'p', sessionId: 's', tenantId: 't', displayName: '技术夹具', authorizationVersion: '0', permissionCodes: ['identity:user:list'], authorizedStoreIds: [], dataScope: { grants: [{ permissionCode: 'identity:user:list', scopes: [{ type: 'TENANT' }] }] }, passwordChangeRequired: false, expiresAt: new Date(Date.now()+60_000).toISOString(), idleTimeoutSeconds: 1800 };
const platform: WebIdentity = { ...staff, principalType: 'PLATFORM', tenantId: null, dataScope: null, permissionCodes: ['platform:session:manage', 'platform:credential:change'] };
const csrf = { csrfToken: 'a'.repeat(43), expiresAt: new Date(Date.now()+60_000).toISOString() };
const pendingReply = () => { const response = reply(null); response.headers.set('X-Session-Cleanup', 'PENDING'); return response; };
const input = () => ({ currentPassword: ' 技术原密码12345 ', newPassword: ' 新技术密码12345 ' });
const deferred = () => { let resolve!: (r: Response) => void; const promise = new Promise<Response>(r => { resolve = r; }); return { promise, resolve }; };

function service(fetcher: typeof fetch) { return new AuthService(new QueryClient(), new RequestClient(fetcher)); }

describe('权限和页面条件（确定性技术夹具）', () => {
  it('代码精确匹配且按空间隔离，不推断角色或通配符', () => {
    expect(hasPermission(staff, 'PLATFORM', 'identity:user:list')).toBe(false);
    expect(hasPermission(platform, 'STAFF', 'platform:session:manage')).toBe(false);
    expect(hasPermission({ ...staff, permissionCodes: ['*'] }, 'STAFF', 'identity:user:list')).toBe(false);
    expect(hasPermission({ ...staff, permissionCodes: ['*'] }, 'STAFF', '*')).toBe(false);
    expect(hasPermission(staff, 'STAFF', 'identity:user:list')).toBe(true);
  });
  it.each([undefined, null])('权限未加载%s默认拒绝，导航不显示', identity => {
    expect(hasPermission(identity, 'STAFF', 'identity:user:list')).toBe(false); expect(navigationFor('STAFF', identity)).toEqual([]);
  });
  it.each(['STAFF', 'PLATFORM'] as const)('%s导航和直接页面权限共用元数据', space => {
    const identity = space === 'STAFF' ? staff : platform;
    for (const page of navigationFor(space, identity)) expect(canAccessPage(pageFor(space, page.path), identity)).toBe(true);
    expect(navigationFor(space, identity).every(page => page.space === space)).toBe(true);
  });
  it('员工无管理权限及受限状态仍允许本人改密和全退出', () => {
    const identity = { ...staff, permissionCodes: [], dataScope: { grants: [] }, passwordChangeRequired: true } as WebIdentity;
    expect(canManageSelf(identity, 'STAFF', 'password')).toBe(true); expect(canManageSelf(identity, 'STAFF', 'logout-all')).toBe(true);
    expect(navigationFor('STAFF', identity).map(page => page.path)).toEqual(['/admin/security']);
    expect(canAccessPage(pageFor('STAFF', '/admin'), identity)).toBe(false);
    expect(hasPermission(identity, 'STAFF', 'identity:user:list')).toBe(false);
  });
  it('平台只有本人会话权限时安全页可访问，但改密操作隐藏', () => {
    const identity = { ...platform, permissionCodes: ['platform:session:manage'] };
    expect(canAccessPage(pageFor('PLATFORM', '/platform/security'), identity)).toBe(true);
    expect(canManageSelf(identity, 'PLATFORM', 'password')).toBe(false); expect(canManageSelf(identity, 'PLATFORM', 'logout-all')).toBe(true);
  });
});

describe('强制改密 beforeLoad 与合法返回', () => {
  it.each(['/admin', '/admin?tab=profile', '/admin/login?returnTo=%2Fadmin', '/admin/login?returnTo=%2Fadmin%2Fsecurity'])('%s优先重定向到本人改密，不被返回目标绕过', async path => {
    const services = createServices(new RequestClient(vi.fn().mockResolvedValue(reply({ ...staff, passwordChangeRequired: true }))));
    const router = createRouter({ routeTree, context: services, history: createMemoryHistory({ initialEntries: [path] }), isServer: true });
    await router.load(); expect(router._serverResult?.type).toBe('redirect');
    if (router._serverResult?.type === 'redirect') expect(router._serverResult.redirect.headers.get('Location')).toBe('/admin/security'); services.queryClient.clear();
  });
  it('受限本人安全页不会被普通管理权限或自身守卫拦截，不循环', async () => {
    const services = createServices(new RequestClient(vi.fn().mockResolvedValue(reply({ ...staff, permissionCodes: [], dataScope: { grants: [] }, passwordChangeRequired: true }))));
    const router = createRouter({ routeTree, context: services, history: createMemoryHistory({ initialEntries: ['/admin/security'] }), isServer: true }); await router.load();
    expect(router._serverResult?.type).toBe('render'); expect(router.state.matches.at(-1)?.status).toBe('success'); services.queryClient.clear();
  });
  it.each(['/admin/security', '/platform/security'])('新增安全页%s未登录仍保护', async path => {
    const services = createServices(new RequestClient(vi.fn().mockImplementation(async () => fail()))); const router = createRouter({ routeTree, context: services, history: createMemoryHistory({ initialEntries: [path] }), isServer: true }); await router.load();
    expect(router._serverResult?.type).toBe('redirect'); services.queryClient.clear();
  });
  it('安全页返回目标白名单按空间隔离', () => { expect(safeReturnTo('STAFF', '/admin/security')).toBe('/admin/security'); expect(safeReturnTo('PLATFORM', '/admin/security')).toBe('/platform'); });
  it('平台缺会话权限进入无权限状态，区别网络或未登录', async () => {
    const services = createServices(new RequestClient(vi.fn().mockResolvedValue(reply({ ...platform, permissionCodes: [] })))); const router = createRouter({ routeTree, context: services, history: createMemoryHistory({ initialEntries: ['/platform/security'] }), isServer: true }); await router.load();
    expect(router._serverResult?.type).toBe('render'); const context=router.state.matches.at(-1)?.context;expect(context && 'sessionError' in context ? context.sessionError?.code : undefined).toBe('PERMISSION_DENIED'); services.queryClient.clear();
  });
});

describe('密码和字段错误', () => {
  it('原输入空格和大小写参与长度，不 trim；无格式组合', () => { expect(passwordError('            ')).toBeUndefined(); expect(passwordError('中文密码中文密码中文密码')).toBeUndefined(); expect(passwordError('a'.repeat(11))).toBeDefined(); });
  it('Unicode码点和UTF16边界对齐后端，非法代理字符拒绝', () => { expect(passwordError('😀'.repeat(128))).toBeUndefined(); expect(passwordError('😀'.repeat(129))).toBeDefined(); expect(passwordError('a'.repeat(128)+'b')).toBeDefined(); expect(passwordError('a'.repeat(12)+'\uD800')).toBeDefined(); });
  it('确认不一致须阻止提交，不改变原输入', () => { expect(confirmationError('原密码 ', '原密码')).toBeDefined(); expect(confirmationError('原密码 ', '原密码 ')).toBeUndefined(); });
  it('422只映射精确已知字段，嵌套和原型字段成为总错误', () => {
    const error = new ApiError('HTTP', '请检查填写内容', 422, 'VALIDATION_FAILED', undefined, ['currentPassword', 'newPassword', '__proto__', 'constructor.prototype.polluted', 'items[0].name'].map(field => ({ field, code: 'INVALID_VALUE', message: '字段不合法' })));
    expect(securityFieldErrors(error).map(field => field.name)).toEqual(['currentPassword', 'newPassword']); expect(securityUnmappedErrors(error)).toHaveLength(3); expect(Object.prototype).not.toHaveProperty('polluted');
  });
  it.each([403,409,429])('%d不把字段错误误放到422校验流程', status => { expect(securityFieldErrors(new ApiError('HTTP', '安全失败', status, 'PERMISSION_DENIED'))).toEqual([]); });
  it.each(['NETWORK','TIMEOUT','PROTOCOL','CANCELLED'] as const)('%s不宣称操作失败或成功', kind => { expect(securityErrorText(new ApiError(kind,'技术失败'))).toContain('结果未确认'); });
  it('503未确认与CSRF/权限/重新确认区分', () => { expect(securityErrorText(new ApiError('HTTP','基础设施不可用',503,'DEPENDENCY_UNAVAILABLE'))).toContain('结果未确认'); expect(securityErrorText(new ApiError('HTTP','失败',403,'CSRF_INVALID'))).toContain('安全凭据'); expect(securityErrorText(new ApiError('HTTP','失败',403,'SECURITY_CONFIRMATION_FAILED'))).toContain('当前密码不正确'); });
});

describe('真实服务类的安全写与竞态（受控传输）', () => {
  it.each(['STAFF', 'PLATFORM'] as const)('%s改密成功清当前身份/CSRF/缓存，保留另一空间', async space => {
    const fetcher = vi.fn().mockResolvedValueOnce(reply(csrf)).mockResolvedValueOnce(pendingReply()); const auth = service(fetcher);
    const other = space === 'STAFF' ? 'PLATFORM' : 'STAFF'; auth.queryClient.setQueryData(authKeys.me(space,0), space === 'STAFF' ? staff : platform); auth.queryClient.setQueryData(authKeys.me(other,0),other === 'STAFF' ? staff : platform);
    const fields = input(); await auth.secureSelf(space,'password',fields);
    const body = JSON.parse(fetcher.mock.calls[1][1].body); expect(body).toEqual(input()); expect(Object.keys(body)).toEqual(['currentPassword','newPassword']); expect(fields).toEqual({currentPassword:'',newPassword:''});
    expect(auth.runtime.notice(space)).toBe('密码已修改，请重新登录'); expect(auth.queryClient.getQueryData(authKeys.me(space,auth.runtime.epoch(space)))).toBeUndefined(); expect(auth.queryClient.getQueryData(authKeys.me(other,0))).toBeDefined(); expect(auth.runtime.epoch(other)).toBe(0);
    expect(JSON.stringify(auth.queryClient.getQueryCache().getAll().map(q=>q.state.data))).not.toContain(input().newPassword); expect(auth.queryClient.getMutationCache().getAll()).toHaveLength(0); auth.queryClient.clear();
  });
  it('全会话撤销只发送重新确认字段，成功清理当前空间', async () => {
    const fetcher = vi.fn().mockResolvedValueOnce(reply(csrf)).mockResolvedValueOnce(reply(null)); const auth = service(fetcher); const fields=input(); await auth.secureSelf('STAFF','logout-all',fields);
    expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({currentPassword:input().currentPassword}); expect(auth.runtime.notice('STAFF')).toContain('全部设备已退出'); auth.queryClient.clear();
  });
  it.each([403,409,422,429,503])('安全写%d不自动重放，密码引用清理', async status => {
    const fetcher = vi.fn().mockResolvedValueOnce(reply(csrf)).mockResolvedValueOnce(fail(status, status===503?'DEPENDENCY_UNAVAILABLE':status===422?'VALIDATION_FAILED':status===429?'RATE_LIMITED':status===409?'BUSINESS_STATE_CONFLICT':'SECURITY_CONFIRMATION_FAILED')); const auth=service(fetcher);const fields=input(); await expect(auth.secureSelf('STAFF','password',fields)).rejects.toMatchObject({status}); expect(fetcher).toHaveBeenCalledTimes(2);expect(fields.currentPassword).toBe(''); expect(auth.runtime.notice('STAFF')??'').not.toContain('密码已修改'); auth.queryClient.clear();
  });
  it('传输中断不提示成功，保留请求结果未确认且可重新确认身份',async()=>{
    const fetcher=vi.fn().mockResolvedValueOnce(reply(csrf)).mockRejectedValueOnce(new TypeError()).mockResolvedValueOnce(reply(staff));const auth=service(fetcher);await expect(auth.secureSelf('STAFF','logout-all',input())).rejects.toMatchObject({kind:'NETWORK'});expect(auth.runtime.notice('STAFF')).toContain('结果未确认');expect(fetcher).toHaveBeenCalledTimes(2);await expect(auth.current('STAFF')).resolves.toMatchObject({principalType:'STAFF'});auth.queryClient.clear();
  });
  it('成功后旧401不会覆盖成功提示，且不能清后来新登录', async () => {
    const old=deferred();const fetcher=vi.fn().mockReturnValueOnce(old.promise).mockResolvedValueOnce(reply(csrf)).mockResolvedValueOnce(reply(null)).mockResolvedValueOnce(reply(csrf)).mockResolvedValueOnce(reply(platform)).mockResolvedValueOnce(reply(csrf)).mockResolvedValueOnce(reply(platform));const auth=service(fetcher);
    const pending=auth.runtime.request('PLATFORM','/platform/old');const check=expect(pending).rejects.toMatchObject({kind:'CANCELLED'});await auth.secureSelf('PLATFORM','password',input());expect(auth.runtime.notice('PLATFORM')).toBe('密码已修改，请重新登录');await auth.login('PLATFORM',{loginName:'技术账号',password:input().newPassword});const epoch=auth.runtime.epoch('PLATFORM');old.resolve(fail(401,'SESSION_REVOKED'));await check;expect(auth.runtime.epoch('PLATFORM')).toBe(epoch);await expect(auth.current('PLATFORM')).resolves.toMatchObject({principalType:'PLATFORM'});auth.queryClient.clear();
  });
  it('受限状态刷新即使授权版本不变也清旧范围，保留平台',async()=>{
    const auth=service(vi.fn().mockResolvedValue(reply({...staff,passwordChangeRequired:true})));auth.queryClient.setQueryData(authKeys.me('STAFF',0),staff);const key=authKeys.protected(staff,0,'technical','item','list');auth.queryClient.setQueryData(key,['技术夹具']);auth.queryClient.setQueryData(authKeys.me('PLATFORM',0),platform);await auth.current('STAFF');expect(auth.queryClient.getQueryData(key)).toBeUndefined();expect(auth.queryClient.getQueryData(authKeys.me('PLATFORM',0))).toBeDefined();auth.queryClient.clear();
  });
  it('身份刷新503保留会话但标识Query失败，与null未登录区分',async()=>{
    const auth=service(vi.fn().mockResolvedValue(fail(503,'DEPENDENCY_UNAVAILABLE')));auth.queryClient.setQueryData(authKeys.me('STAFF',0),staff);await expect(auth.current('STAFF')).rejects.toMatchObject({status:503});expect(auth.runtime.epoch('STAFF')).toBe(0);expect(auth.queryClient.getQueryState(authKeys.me('STAFF',0))?.status).toBe('error');auth.queryClient.clear();
  });
  it('成功后的确定401保留成功提示，不覆盖成未知失败', async () => {
    const fetcher=vi.fn().mockResolvedValueOnce(reply(csrf)).mockResolvedValueOnce(reply(null)).mockImplementation(async()=>fail(401,'SESSION_REVOKED'));const auth=service(fetcher);await auth.secureSelf('STAFF','password',input());await expect(auth.current('STAFF')).resolves.toBeNull();expect(auth.runtime.notice('STAFF')).toBe('密码已修改，请重新登录');auth.queryClient.clear();
  });
  it('重复敏感操作被互斥拒绝时也清理调用变量，首次写仅一次', async () => {
    const response=deferred();const fetcher=vi.fn().mockResolvedValueOnce(reply(csrf)).mockReturnValueOnce(response.promise);const auth=service(fetcher);const first=auth.secureSelf('STAFF','password',input());const duplicate=input();await expect(auth.secureSelf('STAFF','password',duplicate)).rejects.toThrow('正在处理');expect(duplicate).toEqual({currentPassword:'',newPassword:''});response.resolve(reply(null));await first;expect(fetcher).toHaveBeenCalledTimes(2);auth.queryClient.clear();
  });

  it('权限变化先交付新Query投影再通知观察者，避免中间空身份',async()=>{
    const changed={...staff,authorizationVersion:'1',permissionCodes:[],dataScope:{grants:[]}};const auth=service(vi.fn().mockResolvedValue(reply(changed)));auth.queryClient.setQueryData(authKeys.me('STAFF',0),staff);const observations:unknown[]=[];const stop=auth.runtime.subscribe(()=>observations.push(auth.queryClient.getQueryData(authKeys.me('STAFF',auth.runtime.epoch('STAFF')))));await auth.current('STAFF');expect(observations).toEqual([changed]);stop();auth.queryClient.clear();
  });

});
