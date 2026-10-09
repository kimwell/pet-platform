import { describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from '@tanstack/react-router';
import { QueryObserver } from '@tanstack/react-query';
import type { WebIdentity } from '../auth/spaces';
import { canAccessPage, navigationFor, pageFor } from '../../router/pageAccess';
import { routeTree } from '../../router/router';
import { createServices } from '../../api/auth/AuthService';
import { authKeys } from '../../api/auth/queryKeys';
import { RequestClient } from '../../api/request';
import { ApiError, queryRetry } from '../../api/ApiError';
import { safeReturnTo } from '../auth/returnTo';
import { fail, reply } from '../../test/responses';
import { getEmployee } from '../../api/identity/employees';
import type { Employee } from '../../api/identity/employees';
import { employeeDetailOptions } from '../../hooks/identity/employeeQueries';
import { detailSearch, employeeDetailHref, employeeDetailPath, isEmployeeId, listReturnTo } from './detailSearch';
import { defaultSearch, employeeHref, normalizeSearch, parseWebSearch, stringifyWebSearch } from './search';

const id = 'b9dd5dbf-4d0c-4a3f-b2a4-20440f8f8a2b', other = 'c9dd5dbf-4d0c-4a3f-b2a4-20440f8f8a2b';
const staff: WebIdentity = { principalType: 'STAFF', principalId: id, sessionId: 's', tenantId: 't', displayName: '技术夹具', authorizationVersion: '0', permissionCodes: ['identity:user:detail'], authorizedStoreIds: [], dataScope: { grants: [{ permissionCode: 'identity:user:detail', scopes: [{ type: 'SELF' }] }] }, passwordChangeRequired: false, expiresAt: '2030-01-01T00:00:00.000Z', idleTimeoutSeconds: 1800 };
const item: Employee = { id, loginName: 'fixture-account', displayName: '技术员工', status: 'ACTIVE', createdAt: '2026-10-09T00:00:00.000Z', updatedAt: '2026-10-09T00:00:00.000Z' };
const make = (fetcher: typeof fetch) => createServices(new RequestClient(fetcher));

describe('详情地址和受限列表返回', () => {
  it('UUID v4 合法且格式校验不证明存在性', () => { expect(isEmployeeId(id)).toBe(true); expect(isEmployeeId(other)).toBe(true); });
  it.each(['bad', '', id.toUpperCase(), id.replace('-4a3f-', '-1a3f-'), id + '/x', '../users', '%2f', null])('非法员工ID %s 不请求', async value => {
    expect(isEmployeeId(value)).toBe(false); const fetcher = vi.fn(), services = make(fetcher);
    await expect(getEmployee(services.auth, String(value), new AbortController().signal)).rejects.toMatchObject({ kind: 'PROTOCOL' });
    expect(employeeDetailOptions(services.auth, staff, String(value)).enabled).toBe(false); expect(fetcher).not.toHaveBeenCalled(); services.queryClient.clear();
  });
  it.each(['https://evil.example/admin/identity/users', '//evil.example/admin/identity/users', '/platform', '/admin/security', '/admin/identity/users/x', '/admin/identity/users/../users', '/admin/identity/users#x', '/admin/identity/users?storeId=x', '/admin/identity/users?page=2&page=3', '/admin/identity/users?sortOrder=asc', '/admin/identity/users?keyword='+ 'a'.repeat(101), '/admin/identity/users?token=secret', '/admin/identity/users\\x', '/admin/identity/users?keyword=', 'x'.repeat(2049)])('拒绝不合法returnTo %s', value => { expect(listReturnTo(value)).toBeUndefined(); expect(detailSearch({ returnTo: value })).toEqual({}); });
  it('无返回、非字符串、重复返回安全清除；未知外层参数不带到导航', () => { for (const returnTo of [undefined, 1, [employeeHref(defaultSearch), employeeHref(defaultSearch)]]) expect(detailSearch({ returnTo })).toEqual({}); expect(detailSearch({ returnTo: employeeHref(defaultSearch), password: 'x' })).toEqual({returnTo:'/admin/identity/users'}); });
  it('六个已提交条件经过详情刷新地址无损往返，默认规范化幂等', () => {
    const query = { ...defaultSearch, keyword: ' A%_\\😀 ', status: 'DISABLED' as const, page: 3, pageSize: 10, sortBy: 'loginName' as const, sortOrder: 'asc' as const };
    const url = new URL(employeeDetailHref(id, employeeHref(query)), 'https://local.invalid');
    const search = detailSearch(parseWebSearch(url.search)); expect(search.returnTo).toBe(employeeHref(query));
    expect(normalizeSearch(parseWebSearch(new URL(search.returnTo!, url).search)).query).toEqual(query);
    expect(listReturnTo('/admin/identity/users?page=0002')).toBe('/admin/identity/users?page=2');
    expect(safeReturnTo('STAFF', url.pathname + url.search)).toBe(employeeDetailHref(id, employeeHref(query)));
    expect(safeReturnTo('PLATFORM', url.pathname + url.search)).toBe('/platform');
  });
});

describe('独立详情权限、正式接口与当前授权缓存', () => {
  it.each([{ permissionCodes: [] }, { permissionCodes: ['identity:user:list'] }])('无detail %j 导航/直达/Query均拒绝且不请求详情', async ({ permissionCodes }) => {
    const identity = { ...staff, permissionCodes }, fetcher = vi.fn().mockResolvedValue(reply(identity)), services = make(fetcher);
    const router = createRouter({routeTree, context:services, history:createMemoryHistory({initialEntries:[employeeDetailHref(id)]}), isServer:true}); await router.load();
    expect(canAccessPage(pageFor('STAFF', employeeDetailHref(id)), identity)).toBe(false);
    expect(router.state.matches.at(-1)?.context).toMatchObject({sessionError:{code:'PERMISSION_DENIED'}});
    const options = employeeDetailOptions(services.auth, identity, id); expect(options.enabled).toBe(false);
    await expect(services.queryClient.fetchQuery({ ...options, retry:false })).rejects.toMatchObject({status:403});
    expect(fetcher.mock.calls.every(([url])=>!String(url).includes('/identity/users'))).toBe(true);services.queryClient.clear();
  });
  it('只有detail可直达；无list导航，详情不生成静态菜单',async()=>{
    const services=make(vi.fn().mockResolvedValue(reply(staff))); const router=createRouter({routeTree,context:services,history:createMemoryHistory({initialEntries:[employeeDetailHref(id)]}),isServer:true});await router.load();
    expect(router._serverResult?.type).toBe('redirect'); if (router._serverResult?.type==='redirect') expect(router._serverResult.redirect.options).toMatchObject({href:'/admin/identity/users',replace:true,state:{legacyModal:{resource:'employees',id}}});
    expect(canAccessPage(pageFor('STAFF','/admin/identity/users'),staff)).toBe(true); expect(navigationFor('STAFF',staff).map(x=>x.path)).not.toContain(employeeDetailPath);services.queryClient.clear();
  });
  it('PLATFORM不能借STAFF详情代码或路径',()=>{expect(canAccessPage(pageFor('STAFF',employeeDetailHref(id)),{...staff,principalType:'PLATFORM',tenantId:null,dataScope:null})).toBe(false);});
  it('只使用生成EmployeeView和正式详情接口、不发送returnTo查询，signal到fetch',async()=>{
    const fetcher=vi.fn().mockResolvedValue(reply(item)),services=make(fetcher);expect(await services.queryClient.fetchQuery(employeeDetailOptions(services.auth,staff,id))).toEqual(item);
    expect(fetcher.mock.calls[0][0]).toBe('/api/admin/identity/users/'+id);expect(fetcher.mock.calls[0][1].signal).toBeInstanceOf(AbortSignal);
    expect(Object.keys(item).sort()).toEqual(['createdAt','displayName','id','loginName','status','updatedAt']);services.queryClient.clear();
  });
  it.each(['不存在','跨租户','详情范围外但列表可见'])('%s 保持正式同一404，不借list范围',async()=>{
    const services=make(vi.fn().mockResolvedValue(fail(404,'RESOURCE_NOT_FOUND')));await expect(getEmployee(services.auth,id,new AbortController().signal)).rejects.toMatchObject({status:404,code:'RESOURCE_NOT_FOUND'});expect(services.auth.runtime.epoch('STAFF')).toBe(0);services.queryClient.clear();
  });
  it.each([{id:other},{loginName:null},{displayName:null},{status:'UNKNOWN'},{createdAt:'bad'},{updatedAt:null},{createdAt:'2026-10-09T00:00:00Z'},{createdAt:'2026-02-30T00:00:00.000Z'}])('响应类型/可空性/目标/时间错误保留trace %j',async change=>{const services=make(vi.fn().mockResolvedValue(reply({...item,...change})));await expect(getEmployee(services.auth,id,new AbortController().signal)).rejects.toMatchObject({kind:'PROTOCOL',traceId:'a'.repeat(32)});services.queryClient.clear();});
  it('目标/身份/租户/会话/授权范围各自隔离且没有placeholder或秘密',()=>{
    const services=make(vi.fn()),base=employeeDetailOptions(services.auth,staff,id);expect(base).not.toHaveProperty('placeholderData');
    for(const identity of [{...staff,principalId:other},{...staff,tenantId:'other'},{...staff,sessionId:'other'},{...staff,authorizationVersion:'1'},{...staff,dataScope:{grants:[]}},{...staff,authorizedStoreIds:[other]}])expect(employeeDetailOptions(services.auth,identity,id).queryKey).not.toEqual(base.queryKey);
    expect(employeeDetailOptions(services.auth,staff,other).queryKey).not.toEqual(base.queryKey);expect(JSON.stringify(base.queryKey)).not.toMatch(/token|password|cookie/i);services.queryClient.clear();
  });
  it('目标切换取消旧请求；晚到旧响应不能成为新详情',async()=>{
    let finish!:(r:Response)=>void;const fetcher=vi.fn().mockImplementationOnce(()=>new Promise<Response>(r=>{finish=r})).mockResolvedValue(reply({...item,id:other})),services=make(fetcher);
    const old=employeeDetailOptions(services.auth,staff,id),pending=services.queryClient.fetchQuery(old).catch(e=>e);await Promise.resolve();await services.queryClient.cancelQueries({queryKey:old.queryKey});services.queryClient.removeQueries({queryKey:old.queryKey});
    const next=employeeDetailOptions(services.auth,staff,other);await services.queryClient.fetchQuery(next);finish(reply(item));await pending;
    expect(fetcher.mock.calls[0][1].signal.aborted).toBe(true);expect(services.queryClient.getQueryData(old.queryKey)).toBeUndefined();expect(services.queryClient.getQueryData(next.queryKey)).toMatchObject({id:other});services.queryClient.clear();
  });
  it.each([{tenantId:'other'},{principalId:other},{dataScope:{grants:[]}},{authorizedStoreIds:[other]},{permissionCodes:[]}])('权威me变化 %j 无虚构版本也清旧详情',async change=>{
    const services=make(vi.fn().mockResolvedValue(reply({...staff,...change}))),key=employeeDetailOptions(services.auth,staff,id).queryKey;
    services.queryClient.setQueryData(authKeys.me('STAFF',0),staff);services.queryClient.setQueryData(key,item);const platform=authKeys.me('PLATFORM',0);services.queryClient.setQueryData(platform,{principalType:'PLATFORM'});
    await services.auth.current('STAFF');expect(services.auth.runtime.epoch('STAFF')).toBe(1);expect(services.queryClient.getQueryData(key)).toBeUndefined();expect(services.queryClient.getQueryData(platform)).toBeDefined();services.queryClient.clear();
  });
  it('401合并身份失效；503不退出且查询可恢复',async()=>{
    const fetcher=vi.fn().mockResolvedValueOnce(fail(503,'DEPENDENCY_UNAVAILABLE')).mockResolvedValueOnce(reply(item)).mockResolvedValue(fail()),services=make(fetcher),options={...employeeDetailOptions(services.auth,staff,id),retry:false};services.queryClient.setQueryData(authKeys.me('STAFF',0),staff);
    const observer=new QueryObserver(services.queryClient,options);await observer.refetch();expect(observer.getCurrentResult()).toMatchObject({isError:true,data:undefined});expect(services.auth.runtime.epoch('STAFF')).toBe(0);await observer.refetch();expect(observer.getCurrentResult().data).toEqual(item);
    await Promise.allSettled([getEmployee(services.auth,id,new AbortController().signal),getEmployee(services.auth,other,new AbortController().signal)]);expect(services.auth.runtime.epoch('STAFF')).toBe(1);expect(services.queryClient.getQueryData(options.queryKey)).toBeUndefined();observer.destroy();services.queryClient.clear();
  });
  it.each(['NETWORK','TIMEOUT','CANCELLED','PROTOCOL'] as const)('%s遵循既有重试语义，取消静默',kind=>{expect(queryRetry(0,new ApiError(kind,'技术错误'))).toBe(['NETWORK','TIMEOUT'].includes(kind));expect(queryRetry(2,new ApiError(kind,''))).toBe(false);});
  it('正式请求层的详情超时保留身份，不伪装404或退出',async()=>{
    vi.useFakeTimers();
    const services=make(vi.fn().mockImplementation(()=>new Promise(()=>{})));
    services.queryClient.setQueryData(authKeys.me('STAFF',0),staff);
    try {
      const pending=getEmployee(services.auth,id,new AbortController().signal).catch(e=>e);
      await vi.advanceTimersByTimeAsync(15_000);
      expect(await pending).toMatchObject({kind:'TIMEOUT'});
      expect(services.auth.runtime.epoch('STAFF')).toBe(0);
      expect(services.queryClient.getQueryData(authKeys.me('STAFF',0))).toEqual(staff);
    } finally { services.queryClient.clear();vi.useRealTimers(); }
  });
  it('保护路由详情刷新仍保留受限返回查询',async()=>{const services=make(vi.fn().mockResolvedValue(reply(staff))),href=employeeDetailHref(id,employeeHref({...defaultSearch,page:3}));const router=createRouter({routeTree,context:services,history:createMemoryHistory({initialEntries:[href]}),isServer:true,parseSearch:parseWebSearch,stringifySearch:stringifyWebSearch});await router.load();expect(router._serverResult?.type).toBe('redirect');if(router._serverResult?.type==='redirect')expect(router._serverResult.redirect.options.href).toBe('/admin/identity/users?page=3');services.queryClient.clear();});
});
