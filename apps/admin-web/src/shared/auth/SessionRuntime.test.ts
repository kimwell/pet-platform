import { describe, expect, it, vi } from 'vitest';
import { QueryClient } from '@tanstack/react-query';
import { RequestClient } from '../api/request';
import { fail, reply } from '../../test/responses';
import { SessionRuntime } from './SessionRuntime';
import { AuthService } from '../../features/auth/api/AuthService';
import { authKeys } from '../../features/auth/api/queryKeys';
import { normalizeName } from './spaces';
import { safeReturnTo } from './returnTo';

const csrf = (char: string) => ({ csrfToken: char.repeat(43), expiresAt: new Date(Date.now() + 60_000).toISOString() });
const deferred = () => { let resolve!: (r: Response) => void; const promise = new Promise<Response>(r => { resolve = r; }); return { promise, resolve }; };
const identity = { principalType: 'STAFF', principalId: 'p', sessionId: 's', tenantId: 't', displayName: '技术夹具', authorizationVersion: '0', permissionCodes: [], authorizedStoreIds: [], dataScope: { grants: [] }, passwordChangeRequired: false, expiresAt: new Date(Date.now() + 60_000).toISOString(), idleTimeoutSeconds: 1800 } as const;

describe('内存会话代际与隔离（受控网络替身）', () => {
  it('同空间并发CSRF合并、两个空间不共享', async () => {
    const fetcher = vi.fn(async (url: string | URL | Request) => reply(csrf(String(url).includes('/admin/') ? 'a' : 'p')));
    const runtime = new SessionRuntime(new RequestClient(fetcher), vi.fn());
    const values = await Promise.all([runtime.csrf('STAFF'), runtime.csrf('STAFF'), runtime.csrf('PLATFORM')]);
    expect(fetcher).toHaveBeenCalledTimes(2); expect(values[0]).toEqual(values[1]); expect(values[0]).not.toEqual(values[2]);
  });
  it('CSRF跨越轮换不写回旧值', async () => {
    const old = deferred(); const fetcher = vi.fn().mockReturnValueOnce(old.promise).mockResolvedValueOnce(reply(csrf('b')));
    const runtime = new SessionRuntime(new RequestClient(fetcher), vi.fn()); const first = runtime.csrf('STAFF');
    const check = expect(first).rejects.toMatchObject({ kind: 'CANCELLED' }); runtime.advance('STAFF');
    await runtime.csrf('STAFF'); old.resolve(reply(csrf('a'))); await check;
    expect((await runtime.csrf('STAFF')).csrfToken).toBe('b'.repeat(43));
  });
  it('登录失败401不调用全局失效，也不自动重试', async () => {
    const clear = vi.fn(); const fetcher = vi.fn().mockResolvedValueOnce(reply(csrf('a'))).mockResolvedValueOnce(fail(401, 'LOGIN_FAILED'));
    const runtime = new SessionRuntime(new RequestClient(fetcher), clear);
    await expect(runtime.request('STAFF', '/admin/auth/login', { method: 'POST', json: {} }, false)).rejects.toMatchObject({ code: 'LOGIN_FAILED' });
    expect(clear).not.toHaveBeenCalled(); expect(runtime.epoch('STAFF')).toBe(0); expect(fetcher).toHaveBeenCalledTimes(2);
  });
  it('多个旧代际401只触发一次清理', async () => {
    const clear = vi.fn(); const runtime = new SessionRuntime(new RequestClient(vi.fn().mockImplementation(async () => fail())), clear);
    await Promise.allSettled([runtime.request('STAFF', '/admin/a'), runtime.request('STAFF', '/admin/b')]);
    expect(clear).toHaveBeenCalledTimes(1); expect(runtime.epoch('STAFF')).toBe(1);
  });
  it.each([200, 401])('旧请求%d晚于新登录不影响新代际', async status => {
    const old = deferred(); const clear = vi.fn(); const runtime = new SessionRuntime(new RequestClient(vi.fn().mockReturnValue(old.promise)), clear);
    const request = runtime.request('STAFF', '/admin/auth/me'); const check = expect(request).rejects.toMatchObject({ kind: 'CANCELLED' });
    runtime.advance('STAFF'); old.resolve(status === 200 ? reply(identity) : fail()); await check;
    expect(runtime.epoch('STAFF')).toBe(1); expect(clear).toHaveBeenCalledTimes(1);
  });
  it('旧me在退出后返回，不能恢复Query身份', async () => {
    const old = deferred(); const service = new AuthService(new QueryClient(), new RequestClient(vi.fn().mockReturnValueOnce(old.promise).mockResolvedValueOnce(reply(csrf('a'))).mockResolvedValueOnce(reply(null))));
    const pending = service.queryClient.fetchQuery(service.meOptions('STAFF')).catch(() => {});
    await Promise.resolve(); await Promise.resolve(); await service.logout('STAFF'); old.resolve(reply(identity)); await pending;
    expect(service.queryClient.getQueryData(authKeys.me('STAFF', service.runtime.epoch('STAFF')))).toBeUndefined(); service.queryClient.clear();
  });
  it('网络/503/非JSON401不当作退出', async () => {
    for (const response of [Promise.reject(new TypeError()), Promise.resolve(fail(503, 'DEPENDENCY_UNAVAILABLE')), Promise.resolve(new Response('HTML', { status: 401 }))]) {
      const clear = vi.fn(); const runtime = new SessionRuntime(new RequestClient(vi.fn().mockReturnValue(response)), clear);
      await expect(runtime.request('STAFF', '/admin/auth/me')).rejects.toBeDefined(); expect(clear).not.toHaveBeenCalled();
    }
  });
  it('CSRF403下次重新获取，权限403保留凭据，写入不重放', async () => {
    for (const code of ['CSRF_INVALID', 'PERMISSION_DENIED']) {
      const fetcher = vi.fn().mockResolvedValueOnce(reply(csrf('a'))).mockResolvedValueOnce(fail(403, code)).mockResolvedValueOnce(reply(csrf('b')));
      const runtime = new SessionRuntime(new RequestClient(fetcher), vi.fn());
      await expect(runtime.request('STAFF', '/admin/write', { method: 'POST' })).rejects.toMatchObject({ code });
      const value = await runtime.csrf('STAFF'); expect(value.csrfToken).toBe((code === 'CSRF_INVALID' ? 'b' : 'a').repeat(43));
      expect(fetcher).toHaveBeenCalledTimes(code === 'CSRF_INVALID' ? 3 : 2);
    }
  });
  it('退出只清当前空间缓存与CSRF，平台保留', async () => {
    const queryClient = new QueryClient(); const fetcher = vi.fn().mockResolvedValueOnce(reply(csrf('p'))).mockResolvedValueOnce(reply(csrf('s'))).mockResolvedValueOnce(reply(null));
    const service = new AuthService(queryClient, new RequestClient(fetcher));
    queryClient.setQueryData(authKeys.me('PLATFORM', 0), { displayName: '平台夹具' }); await service.runtime.csrf('PLATFORM');
    queryClient.setQueryData(authKeys.me('STAFF', 0), identity); await service.logout('STAFF');
    expect(queryClient.getQueryData(authKeys.me('STAFF', 0))).toBeUndefined(); expect(queryClient.getQueryData(authKeys.me('PLATFORM', 0))).toEqual({ displayName: '平台夹具' });
    expect(service.runtime.epoch('PLATFORM')).toBe(0); expect((await service.runtime.csrf('PLATFORM')).csrfToken).toBe('p'.repeat(43)); queryClient.clear();
  });
  it('退出失败不宣称成功且可以重新恢复有效身份', async () => {
    const fetcher = vi.fn().mockResolvedValueOnce(reply(csrf('a'))).mockResolvedValueOnce(fail(503, 'DEPENDENCY_UNAVAILABLE')).mockResolvedValueOnce(reply(identity));
    const service = new AuthService(new QueryClient(), new RequestClient(fetcher));
    await expect(service.logout('STAFF')).rejects.toMatchObject({ status: 503 }); expect(service.runtime.notice('STAFF')).toContain('退出未确认');
    await expect(service.current('STAFF')).resolves.toMatchObject({ displayName: '技术夹具' }); service.queryClient.clear();
  });
  it('已失效退出按真实401契约处理', async () => {
    const service = new AuthService(new QueryClient(), new RequestClient(vi.fn().mockResolvedValue(fail(401, 'SESSION_EXPIRED'))));
    await expect(service.logout('STAFF')).resolves.toBeUndefined(); expect(service.runtime.notice('STAFF')).toContain('已失效'); service.queryClient.clear();
  });
  it('A退出后B登录，身份来自me，密码不作为mutation或query数据', async () => {
    const password = ' 技术Password-原输入 ';
    const fetcher = vi.fn().mockResolvedValueOnce(reply(csrf('a'))).mockResolvedValueOnce(reply(null)).mockResolvedValueOnce(reply(csrf('b'))).mockResolvedValueOnce(reply(identity)).mockResolvedValueOnce(reply(csrf('c'))).mockResolvedValueOnce(reply({ ...identity, principalId: 'b', displayName: 'B' }));
    const service = new AuthService(new QueryClient(), new RequestClient(fetcher)); service.queryClient.setQueryData(authKeys.me('STAFF',0),identity); await service.logout('STAFF');
    await service.login('STAFF', { tenantCode: ' TENANT ', loginName: ' B ', password }); const result = await service.current('STAFF');
    expect(result?.displayName).toBe('B'); const body = JSON.parse(fetcher.mock.calls[3][1].body); expect(body).toEqual({ tenantCode: 'tenant', loginName: 'b', password });
    expect(JSON.stringify(service.queryClient.getQueryCache().getAll().map(q => [q.queryKey, q.state.data]))).not.toContain(password);
    expect(service.queryClient.getMutationCache().getAll()).toHaveLength(0); service.queryClient.clear();
  });
  it('旧401晚于真实login流程，不能清新B会话', async () => {
    const old = deferred(); const fetcher = vi.fn().mockReturnValueOnce(old.promise).mockResolvedValueOnce(reply(csrf('a'))).mockResolvedValueOnce(reply(identity)).mockResolvedValueOnce(reply(csrf('b'))).mockResolvedValueOnce(reply({ ...identity, principalId: 'b' }));
    const service = new AuthService(new QueryClient(), new RequestClient(fetcher));
    const request = service.runtime.request('STAFF', '/admin/old'); const check = expect(request).rejects.toMatchObject({ kind: 'CANCELLED' });
    await service.login('STAFF', { tenantCode:'tenant',loginName:'b',password:'技术输入不持久化' }); const value = await service.current('STAFF');
    const epoch = service.runtime.epoch('STAFF'); old.resolve(fail(401,'SESSION_REVOKED')); await check;
    expect(value?.principalId).toBe('b'); expect(service.runtime.epoch('STAFF')).toBe(epoch); service.queryClient.clear();
  });
  it('CSRF旧请求跨越login，不覆盖轮换后凭据', async () => {
    const old = deferred(); const fetcher = vi.fn().mockReturnValueOnce(old.promise).mockResolvedValueOnce(reply(csrf('a'))).mockResolvedValueOnce(reply(identity)).mockResolvedValueOnce(reply(csrf('b')));
    const service = new AuthService(new QueryClient(),new RequestClient(fetcher));
    const pending = service.runtime.csrf('STAFF'); const check = expect(pending).rejects.toMatchObject({kind:'CANCELLED'});
    await service.login('STAFF',{tenantCode:'tenant',loginName:'b',password:'技术输入不持久化'});old.resolve(reply(csrf('z')));await check;
    expect((await service.runtime.csrf('STAFF')).csrfToken).toBe('b'.repeat(43));service.queryClient.clear();
  });
  it('权威授权变化清理当前受保护业务范围，另一个空间保留',async()=>{
    const queryClient=new QueryClient();const service=new AuthService(queryClient,new RequestClient(vi.fn().mockResolvedValue(reply({...identity,authorizationVersion:'1'}))));
    const generatedIdentity = {...identity,permissionCodes:[],authorizedStoreIds:[],dataScope:{grants:[]}};
    const oldKey=authKeys.protected(generatedIdentity,0,'example','item','list');queryClient.setQueryData(oldKey,['技术夹具']);queryClient.setQueryData(authKeys.me('STAFF',0),generatedIdentity);queryClient.setQueryData(authKeys.me('PLATFORM',0),{displayName:'另一空间'});
    await service.current('STAFF');expect(queryClient.getQueryData(oldKey)).toBeUndefined();expect(queryClient.getQueryData(authKeys.me('PLATFORM',0))).toEqual({displayName:'另一空间'});queryClient.clear();
  });
  it('两个空间过渡互不阻塞，同空间拒绝重复登录/退出', async () => {
    let finish!: () => void; const runtime = new SessionRuntime(new RequestClient(), vi.fn()); const pending = runtime.transition('STAFF', () => new Promise<void>(r => { finish = r; }));
    await expect(runtime.transition('STAFF', async () => {})).rejects.toThrow('正在处理'); await expect(runtime.transition('PLATFORM', async () => 'ok')).resolves.toBe('ok'); finish(); await pending;
  });
  it('不能把STAFF认证头发往PLATFORM路径', async () => {
    const fetcher = vi.fn(); const runtime = new SessionRuntime(new RequestClient(fetcher), vi.fn());
    await expect(runtime.request('STAFF', '/platform/auth/logout', { method: 'POST' })).rejects.toMatchObject({ kind: 'PROTOCOL' }); expect(fetcher).not.toHaveBeenCalled();
  });
});

describe('登录规范化及合法返回地址', () => {
  it('只strip后ASCII小写，不处理密码，NBSP不额外trim', () => { expect(normalizeName('　 Foo.BAR \t')).toBe('foo.bar'); expect(normalizeName('\u00a0USER\u00a0')).toBe('\u00a0user\u00a0'); });
  it('保留同域合法search', () => { expect(safeReturnTo('STAFF', '/admin?tab=profile&label=%E4%B8%AD')).toBe('/admin?tab=profile&label=%E4%B8%AD'); });
  it.each(['https://evil.invalid', '//evil.invalid', '/platform', '/admin/login', '/admin/login?returnTo=/admin', '/admin/../platform', '/admin%2flogin', '/admin?token=secret', '/admin?PASSWORD=x', '/admin?returnTo=//evil.invalid', '/admin#token=x', '/admin\\evil', '/admin?x=a\n'])('拒绝非法目标%s', input => { expect(safeReturnTo('STAFF', input)).toBe('/admin'); });
});
