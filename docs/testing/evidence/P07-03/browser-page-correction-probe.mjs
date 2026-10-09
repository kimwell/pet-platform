// 确定性页面测试，传输替身仅为竞态/错误/边界证据，不是正式业务验收。
import { chromium } from '/Users/kimwell/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs';
import fs from 'node:fs';
const root=process.cwd(),ev=root+'/docs/testing/evidence/P07-03',origin='http://127.0.0.1:5175',list='/admin/identity/users';
const id='b9dd5dbf-4d0c-4a3f-b2a4-20440f8f8a2b',other='c9dd5dbf-4d0c-4a3f-b2a4-20440f8f8a2b';
const me={principalType:'STAFF',principalId:id,tenantId:'b9dd5dbf-4d0c-4a3f-b2a4-20440f8f8a2c',sessionId:'b9dd5dbf-4d0c-4a3f-b2a4-20440f8f8a2d',displayName:'组件技术夹具',authorizationVersion:'0',permissionCodes:['identity:user:list','identity:user:detail'],authorizedStoreIds:[],dataScope:{grants:[{permissionCode:'identity:user:list',scopes:[{type:'TENANT'}]},{permissionCode:'identity:user:detail',scopes:[{type:'TENANT'}]}]},passwordChangeRequired:false,expiresAt:'2030-01-01T00:00:00.000Z',idleTimeoutSeconds:1800};
const item={id,loginName:'fixture-account',displayName:'组件技术夹具员工',status:'ACTIVE',createdAt:'2026-10-09T00:00:00.000Z',updatedAt:'2026-10-09T00:00:00.000Z'};
const browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
const result={cwd:root,command:['node','docs/testing/evidence/P07-03/browser-components.mjs'],evidenceType:'确定性组件测试（网络替身），不能替代正式业务联调',exitCode:0,scenarios:[],pageErrors:[],consoleErrors:[]};
const save=()=>fs.writeFileSync(ev+'/browser-page-correction-probe.json',JSON.stringify(result,null,2));
function check(name,ok,details={}){result.scenarios.push({name,result:ok?'PASS':'FAIL',...details});save();if(!ok)throw Error(name)}
const pass=data=>({status:200,contentType:'application/json',body:JSON.stringify({success:true,data,traceId:'a'.repeat(32)})});
const fail=(status,code)=>({status,contentType:'application/json',body:JSON.stringify({success:false,error:{code,message:'技术故障场景'},traceId:'a'.repeat(32)})});
async function fixture(){let identity=structuredClone(me),handler=async(route,u)=>route.fulfill(pass({...item,id:u.pathname.split('/').at(-1)})),listHandler=(route,u)=>route.fulfill(pass({items:[item],page:Number(u.searchParams.get('page')??1),pageSize:Number(u.searchParams.get('pageSize')??20),total:'41'}));const context=await browser.newContext({viewport:{width:1280,height:900}}),p=await context.newPage();const requests=[];
 p.on('pageerror',e=>result.pageErrors.push(e.message));p.on('console',m=>{if(m.type()==='error')result.consoleErrors.push(m.text())});
 await p.route('**/api/admin/**',async route=>{const u=new URL(route.request().url());if(u.pathname==='/api/admin/auth/me')return route.fulfill(identity?pass(identity):fail(401,'SESSION_EXPIRED'));if(u.pathname===list.replace('/admin','/api/admin'))return listHandler(route,u);if(u.pathname.startsWith('/api/admin/identity/users/')){requests.push(u.pathname);return handler(route,u);}throw Error('未登记技术接口')});
 return {p,context,requests,setMe:x=>identity=x,handle:x=>handler=x,handleList:x=>listHandler=x};
}
async function loaded(p){await p.getByText('员工详情已加载。',{exact:true}).waitFor();}
async function navigate(p,href){await p.evaluate(async href=>{const {router}=await import(performance.getEntriesByType('resource').map(x=>x.name).find(x=>/\/src\/app\/router\/router\.ts(?:\?|$)/.test(x)));await router.navigate({href});},href)}
async function revalidate(p){await p.evaluate(async()=>{const {router}=await import(performance.getEntriesByType('resource').map(x=>x.name).find(x=>/\/src\/app\/router\/router\.ts(?:\?|$)/.test(x)));await router.invalidate();})}
async function cache(p){return p.evaluate(async()=>{const {services}=await import(performance.getEntriesByType('resource').map(x=>x.name).find(x=>/\/src\/app\/router\/router\.ts(?:\?|$)/.test(x)));return services.queryClient.getQueryCache().getAll().filter(q=>q.queryKey[3]==='detail').map(q=>({scope:q.queryKey[0],id:q.state.data?.id}));})}
try{
 let f=await fixture();let listQueries=[];f.handleList((route,u)=>{listQueries.push(u.search);return route.fulfill(pass({items:[item],page:Number(u.searchParams.get('page')??1),pageSize:20,total:'1'}))});await f.p.goto(origin+list+'/'+id+'?returnTo='+encodeURIComponent(list+'?page=99'));await loaded(f.p);await f.p.getByRole('button',{name:'返回列表',exact:true}).click();await f.p.waitForURL(origin+list);await f.p.getByText('本页 1 条，共 1 条。',{exact:true}).waitFor();check('返回原页越界复用一次纠正不循环',listQueries.length===2&&new URLSearchParams(listQueries[0]).get('page')==='99'&&new URLSearchParams(listQueries[1]).get('page')==='1',{listQueries});await f.context.close();

}catch(e){result.exitCode=1;result.error=e.message}finally{await browser.close();save()}
console.log(JSON.stringify(result));process.exitCode=result.exitCode;
