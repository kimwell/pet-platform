import { describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from '@tanstack/react-router';
import { QueryObserver } from '@tanstack/react-query';
import { routeTree } from '../../router/router';
import { createServices } from '../../api/auth/AuthService';
import { authKeys } from '../../api/auth/queryKeys';
import { RequestClient } from '../../api/request';
import { ApiError, queryRetry } from '../../api/ApiError';
import { canAccessPage, navigationFor, pageFor } from '../../router/pageAccess';
import type { WebIdentity } from '../auth/spaces';
import { safeReturnTo } from '../auth/returnTo';
import { fail, reply } from '../../test/responses';
import { displayInstant, displayText } from '../format';
import { defaultSearch, employeeHref, initialEmployeeHistoryState, keywordError, normalizeSearch, parseWebSearch, sortFields, stringifyWebSearch, submitFilters, tableSort } from './search';
import { correctedPage, safePagination } from './pagination';
import { employeeListOptions } from '../../hooks/identity/employeeQueries';
import { listEmployees } from '../../api/identity/employees';
import type { EmployeePage } from '../../api/identity/employees';

const staff: WebIdentity = { principalType: 'STAFF', principalId: 'p', sessionId: 's', tenantId: 't', displayName: '技术夹具', authorizationVersion: '0', permissionCodes: ['identity:user:list'], authorizedStoreIds: [], dataScope: { grants: [{ permissionCode: 'identity:user:list', scopes: [{ type: 'TENANT' }] }] }, passwordChangeRequired: false, expiresAt: '2030-01-01T00:00:00.000Z', idleTimeoutSeconds: 1800 };
const page: EmployeePage = { page: 1, pageSize: 20, total: '1', items: [{ id: 'b9dd5dbf-4d0c-4a3f-b2a4-20440f8f8a2b', loginName: 'literal', displayName: '技术员工', status: 'ACTIVE', createdAt: '2026-10-09T00:00:00.000Z', updatedAt: '2026-10-09T00:00:00.000Z' }] };
const make = (fetcher: typeof fetch) => createServices(new RequestClient(fetcher));

describe('员工 URL 原始解析及契约边界', () => {
  it('默认条件、规范化幂等和零请求依赖', () => { expect(normalizeSearch({}).query).toEqual(defaultSearch); expect(employeeHref(defaultSearch)).toBe('/admin/identity/users'); });
  it.each(['page=0', 'page=2147483648', 'page=-1', 'page=1.5', 'page=1e2', 'page=', 'pageSize=0', 'pageSize=101', 'status=active', 'status=', 'sortBy=version', 'sortBy=', 'sortOrder=asc', 'sortBy=id&sortOrder=ASC', 'keyword=', 'keyword=%20', 'keyword='+ 'a'.repeat(101)])('非法 %s 恢复默认并提示', raw => { const result = normalizeSearch(parseWebSearch(raw)); expect(result.query).toEqual(defaultSearch); expect(result.notice).toContain('无效'); });
  it.each(['page', 'pageSize', 'keyword', 'status', 'sortBy', 'sortOrder'])('重复 %s 不采用第一个值', field => { expect(normalizeSearch(parseWebSearch(`${field}=1&${field}=2`)).notice).toContain('重复'); });
  it('未知参数含 storeId/roleId/日期移除，不送后端', () => { const value = normalizeSearch(parseWebSearch('roleId=a&storeId=b&startAt=x&page=2')); expect(employeeHref(value.query)).toBe('/admin/identity/users?page=2'); expect(value.notice).toContain('不支持'); });
  it('前导零规范化且空值不是省略', () => { expect(normalizeSearch(parseWebSearch('page=0002&pageSize=001')).query).toMatchObject({ page: 2, pageSize: 1 }); });
  it('初始规范化前保留重复参数提示到同一历史条目；合法和其他页面不改状态', () => {
    const state = { employeePageCorrected: false };
    expect(initialEmployeeHistoryState('/admin/identity/users?page=2&page=3&storeId=x', state)).toMatchObject({ employeeUrlNotice: expect.stringContaining('重复') });
    expect(initialEmployeeHistoryState('/admin/identity/users?keyword=001', state)).toBe(state);
    expect(initialEmployeeHistoryState('/admin?page=bad', state)).toBe(state);
  });
  it('字面量关键词空格/大小写/数字/特殊字符无损往返', () => { for (const keyword of [' AbC ', '001', 'false', '%_\\', '😀'.repeat(100), '\u00a0']) { const query = { ...defaultSearch, keyword }; expect(normalizeSearch(parseWebSearch(employeeHref(query).split('?')[1])).query).toEqual(query); } });
  it('Unicode码点边界、不全空白与非法代理字符', () => { expect(keywordError('😀'.repeat(100))).toBeUndefined(); expect(keywordError('😀'.repeat(101))).toBeDefined(); expect(keywordError('\u3000')).toBeDefined(); expect(keywordError('\uD800')).toBeDefined(); });
  it('输入草稿独立，提交回首且不trim；重置恢复默认', () => { const submitted = { ...defaultSearch, page: 4, keyword: '原条件' }; const draft = { keyword: ' 新条件 ', status: 'DISABLED' as const }; expect(submitted.page).toBe(4); expect(submitFilters(submitted, draft)).toEqual({ ...defaultSearch, keyword: ' 新条件 ', status: 'DISABLED' }); expect(submitFilters(submitted, { keyword: '' }).keyword).toBeUndefined(); });
  it.each(sortFields)('公开字段%s单列映射、清除恢复默认', field => { expect(tableSort(field, 'ascend')).toEqual({ sortBy: field, sortOrder: 'asc' }); expect(tableSort(field, 'descend')).toEqual({ sortBy: field, sortOrder: 'desc' }); expect(tableSort(field, undefined)).toEqual({ sortBy: 'createdAt', sortOrder: 'desc' }); });
  it('拒绝任意字段，不构造稳定ID第二排序', () => { expect(tableSort('passwordHash','ascend')).toEqual({ sortBy:'createdAt',sortOrder:'desc' }); expect(employeeHref({ ...defaultSearch, sortBy: 'id', sortOrder:'asc' })).toBe('/admin/identity/users?sortBy=id&sortOrder=asc'); });
});

describe('无损 total、页数和有限纠正', () => {
  it.each(['-1', '1.2', '01', '', '1e3', '+1', 'NaN'])('拒绝非法 total %s', total => { expect(() => safePagination(total,1,20)).toThrow(ApiError); });
  it('最大安全整数、ceil以BigInt计算且0至少1页', () => { expect(safePagination('9007199254740991',1,20)).toMatchObject({ kind: 'safe', total: Number.MAX_SAFE_INTEGER, lastPage: 450359962737050 }); expect(safePagination('0',1,20)).toMatchObject({ kind:'safe',total:0,lastPage:1 }); });
  it('超过MAX_SAFE不丢精度、不向组件造number', () => { const exact='9007199254740993';const result=safePagination(exact,1,20);expect(result.kind).toBe('limited');expect(result).not.toHaveProperty('total');expect(exact).toBe('9007199254740993'); });
  it('空末页回退、零结果首且最多一次不循环', () => { expect(correctedPage(safePagination('21',4,20),4,false)).toBe(2); expect(correctedPage(safePagination('0',4,20),4,false)).toBe(1); expect(correctedPage(safePagination('0',2,20),2,true)).toBeUndefined(); expect(correctedPage(safePagination('9007199254740993',2,20),2,false)).toBeUndefined(); });
  it('合法页码及offset运算受界，无静默clamp', () => { expect(()=>safePagination('1',2147483648,20)).toThrow();expect(()=>safePagination('1',1,101)).toThrow();expect(safePagination('9007199254740991',1,100)).toMatchObject({maximumPage:21474837}); });
});

describe('路由与缓存授权（受控传输，非业务联调）', () => {
  it.each([{ permissionCodes: [] },{ permissionCodes: ['identity:user:detail'] }])('无独立list权限导航/直接页一致且不请求列表 %j', async ({ permissionCodes }) => {
    const fetcher=vi.fn().mockResolvedValue(reply({...staff,permissionCodes})),services=make(fetcher);
    const router=createRouter({routeTree,context:services,history:createMemoryHistory({initialEntries:['/admin/identity/users']}),isServer:true});await router.load();
    expect(canAccessPage(pageFor('STAFF','/admin/identity/users'),{...staff,permissionCodes})).toBe(permissionCodes.includes('identity:user:detail'));
    expect(navigationFor('STAFF',{...staff,permissionCodes}).map(x=>x.path)).not.toContain('/admin/identity/users');
    expect(router.state.matches.at(-1)?.context).toMatchObject(permissionCodes.length ? {sessionError:undefined} : {sessionError:{code:'PERMISSION_DENIED'}});expect(fetcher.mock.calls.every(([url])=>!String(url).includes('/identity/users'))).toBe(true);services.queryClient.clear();
  });
  it('合法返回支持list已提交条件但不同身份域拒绝',()=>{expect(safeReturnTo('STAFF','/admin/identity/users?page=2')).toBe('/admin/identity/users?page=2');expect(safeReturnTo('PLATFORM','/admin/identity/users')).toBe('/platform');});
  it('原始非法/重复URL安全规范化，不送非法值',async()=>{const fetcher=vi.fn().mockResolvedValue(reply(staff));const services=make(fetcher);const router=createRouter({routeTree,context:services,history:createMemoryHistory({initialEntries:['/admin/identity/users?page=2&page=3']}),isServer:true,parseSearch:parseWebSearch,stringifySearch:stringifyWebSearch});await router.load();expect(router._serverResult?.type).toBe('redirect');if(router._serverResult?.type==='redirect')expect(router._serverResult.redirect.options.href).toBe('/admin/identity/users');expect(fetcher.mock.calls.every(([url])=>!String(url).includes('/identity/users'))).toBe(true);services.queryClient.clear();});
  it('生成DTO请求只含实际条件，signal与row id无伪字段',async()=>{const fetcher=vi.fn().mockResolvedValue(reply(page)),services=make(fetcher);const result=await services.queryClient.fetchQuery(employeeListOptions(services.auth,staff,defaultSearch));expect(result).toEqual(page);expect(fetcher.mock.calls[0][1].signal).toBeInstanceOf(AbortSignal);expect(Object.keys(result.items[0]).sort()).toEqual(['createdAt','displayName','id','loginName','status','updatedAt']);services.queryClient.clear();});
  it('同租户不同员工/范围/代际缓存分开且无placeholder',()=>{const services=make(vi.fn());const first=employeeListOptions(services.auth,staff,defaultSearch);expect(first).not.toHaveProperty('placeholderData');expect(first.queryKey).not.toEqual(employeeListOptions(services.auth,{...staff,principalId:'other'},defaultSearch).queryKey);expect(first.queryKey).not.toEqual(employeeListOptions(services.auth,{...staff,dataScope:{grants:[]}},defaultSearch).queryKey);services.auth.runtime.advance('STAFF');expect(first.queryKey).not.toEqual(employeeListOptions(services.auth,staff,defaultSearch).queryKey);services.queryClient.clear();});
  it('取消旧条件后晚到200不写入缓存',async()=>{let finish!:(r:Response)=>void;const fetcher=vi.fn().mockImplementation(()=>new Promise<Response>(r=>{finish=r;})),services=make(fetcher);const options=employeeListOptions(services.auth,staff,defaultSearch);const result=services.queryClient.fetchQuery(options).catch(e=>e);await Promise.resolve();await services.queryClient.cancelQueries({queryKey:options.queryKey});finish(reply(page));await result;expect(fetcher.mock.calls[0][1].signal.aborted).toBe(true);expect(services.queryClient.getQueryData(options.queryKey)).toBeUndefined();services.queryClient.clear();});
  it('身份变更取消旧列表并保留其他空间',async()=>{const fetcher=vi.fn().mockImplementation(()=>new Promise(()=>{})),services=make(fetcher);const options=employeeListOptions(services.auth,staff,defaultSearch);const result=services.queryClient.fetchQuery(options).catch(e=>e);const other=authKeys.me('PLATFORM',0);services.queryClient.setQueryData(other,{principalType:'PLATFORM'});await Promise.resolve();services.auth.runtime.advance('STAFF');await result;expect(fetcher.mock.calls[0][1].signal.aborted).toBe(true);expect(services.queryClient.getQueryData(options.queryKey)).toBeUndefined();expect(services.queryClient.getQueryData(other)).toBeDefined();services.queryClient.clear();});
  it('列表操作范围变化无版本递增也清旧缓存',async()=>{const changed={...staff,dataScope:{grants:[{permissionCode:'identity:user:list',scopes:[{type:'SELF'}]}]}};const services=make(vi.fn().mockResolvedValue(reply(changed)));const key=employeeListOptions(services.auth,staff,defaultSearch).queryKey;services.queryClient.setQueryData(authKeys.me('STAFF',0),staff);services.queryClient.setQueryData(key,page);await services.auth.current('STAFF');expect(services.auth.runtime.epoch('STAFF')).toBe(1);expect(services.queryClient.getQueryData(key)).toBeUndefined();services.queryClient.clear();});
  it('首次失败与刷新失败Query可观察状态不同，503保留身份',async()=>{const fetcher=vi.fn().mockResolvedValue(fail(503,'DEPENDENCY_UNAVAILABLE')),services=make(fetcher);services.queryClient.setQueryData(authKeys.me('STAFF',0),staff);const options={...employeeListOptions(services.auth,staff,defaultSearch),retry:false};const observer=new QueryObserver(services.queryClient,options);await observer.refetch();expect(observer.getCurrentResult()).toMatchObject({isError:true,data:undefined});services.queryClient.setQueryData(options.queryKey,page);await observer.refetch();expect(observer.getCurrentResult()).toMatchObject({isError:true,data:page});expect(services.auth.runtime.epoch('STAFF')).toBe(0);expect(services.queryClient.getQueryData(authKeys.me('STAFF',0))).toEqual(staff);observer.destroy();services.queryClient.clear();});
  it('已确定401清身份而503/422/CSRF没有越权重试',async()=>{const services=make(vi.fn().mockResolvedValue(fail()));services.queryClient.setQueryData(authKeys.me('STAFF',0),staff);await expect(listEmployees(services.auth,defaultSearch,new AbortController().signal)).rejects.toMatchObject({status:401});expect(services.auth.runtime.epoch('STAFF')).toBe(1);expect(queryRetry(0,new ApiError('HTTP','',422,'VALIDATION_FAILED'))).toBe(false);expect(queryRetry(0,new ApiError('HTTP','',403,'CSRF_INVALID'))).toBe(false);expect(queryRetry(0,new ApiError('HTTP','',503,'DEPENDENCY_UNAVAILABLE'))).toBe(true);services.queryClient.clear();});
  it('响应total或分页不合法为协议错误，不伪造total',async()=>{const services=make(vi.fn().mockResolvedValue(reply({...page,total:'01'})));await expect(listEmployees(services.auth,defaultSearch,new AbortController().signal)).rejects.toMatchObject({kind:'PROTOCOL'});services.queryClient.clear();});
  it('时间按上海且空值统一',()=>{expect(displayInstant('2026-10-09T00:00:00.000Z')).toBe('2026/10/09 08:00:00');expect(displayInstant(null)).toBe('—');expect(displayText('')).toBe('—');});
});
