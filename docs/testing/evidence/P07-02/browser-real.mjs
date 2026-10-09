import {chromium} from '/Users/kimwell/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs';
import fs from 'node:fs';import {execFileSync} from 'node:child_process';
const root=process.cwd(),local=root+'/.local-data/p07-02',ev=root+'/docs/testing/evidence/P07-02',url='http://127.0.0.1:5173',path='/admin/identity/users';
const password=fs.readFileSync(local+'/staff-password','utf8'),platformPassword=fs.readFileSync(local+'/platform-password','utf8');
const browser=await chromium.launch({executablePath:'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',headless:true});
const context=await browser.newContext({viewport:{width:1280,height:900}}),p=await context.newPage();
const result={cwd:root,command:['node','.local-data/p07-02/browser-real.mjs'],exitCode:0,environment:'正式生产JAR/local、受限runtime、独立PG/Redis、正式bootstrap；目标/授权SQL隔离夹具；无网络替身',scenarios:[],requests:[],responses:[],consoleErrors:[],pageErrors:[],snapshots:[]};
const save=()=>fs.writeFileSync(ev+'/browser-real.json',JSON.stringify(result,null,2));
function record(name,ok,details={}){result.scenarios.push({name,result:ok?'PASS':'FAIL',...details});save();if(!ok)throw Error(name)}
const pause=ms=>new Promise(r=>setTimeout(r,ms));
p.on('request',r=>{if(new URL(r.url()).pathname==='/api/admin/identity/users')result.requests.push({query:new URL(r.url()).search})});
p.on('response',async response=>{if(new URL(response.url()).pathname==='/api/admin/identity/users'){try{const body=await response.json();result.responses.push({status:response.status(),query:new URL(response.url()).search,code:body.error?.code,total:body.data?.total,page:body.data?.page});}catch{}}});
p.on('pageerror',e=>result.pageErrors.push(e.message));p.on('console',m=>{if(m.type()==='error')result.consoleErrors.push(m.text())});
const sql=q=>execFileSync('docker',['exec','-i','pet-p07-02-pg','psql','-X','-qAt','-v','ON_ERROR_STOP=1','-U','postgres','-d','p07_02_acceptance'],{input:q,encoding:'utf8'}).trim();
const tid=sql("SELECT id FROM platform_tenant WHERE code='p07-02-acceptance'");
const op=sql(`SELECT id FROM identity_employee WHERE tenant_id='${tid}' AND login_name='p07-staff'`);
const role=sql(`SELECT id FROM identity_role WHERE tenant_id='${tid}' AND code='tenant-admin'`);
const assoc=JSON.parse(sql(`SELECT row_to_json(x) FROM identity_employee_store x WHERE tenant_id='${tid}' AND employee_id='${op}'`));
const cols='id,login_name,display_name,status,created_at,updated_at';
const scope=kind=>sql(`UPDATE identity_role_permission SET scope_type='${kind}' WHERE tenant_id='${tid}' AND role_id='${role}' AND permission_code='identity:user:list'`);
const restoreGrant=()=>sql(`INSERT INTO identity_role_permission(id,tenant_id,role_id,permission_code,scope_type) VALUES(gen_random_uuid(),'${tid}','${role}','identity:user:list','TENANT') ON CONFLICT(tenant_id,role_id,permission_code,scope_type) DO NOTHING; UPDATE identity_role_permission SET scope_type='TENANT' WHERE tenant_id='${tid}' AND role_id='${role}' AND permission_code='identity:user:list'; GRANT SELECT(${cols}) ON identity_employee TO pet_runtime; INSERT INTO identity_employee_store(id,tenant_id,employee_id,store_id) VALUES('${assoc.id}','${tid}','${op}','${assoc.store_id}') ON CONFLICT DO NOTHING;`);
async function login(page,space='admin',tenant='p07-02-acceptance'){
 await page.goto(url+'/'+space+'/login');await page.getByLabel('账号',{exact:true}).waitFor();
 if(space==='admin')await page.getByLabel('租户编码',{exact:true}).fill(tenant);
 await page.getByLabel('账号',{exact:true}).fill(space==='admin'?'p07-staff':'p07-platform');await page.getByLabel('密码',{exact:true}).fill(space==='admin'?password:platformPassword);await page.getByLabel('密码',{exact:true}).press('Enter');await page.waitForURL(u=>u.pathname==='/'+space,{timeout:20000});
}
async function loaded(page=p){await page.getByRole('heading',{name:'员工列表',exact:true}).waitFor();await page.getByRole('status').filter({hasText:/本页 \d+ 条，共 \d+ 条/}).waitFor({timeout:20000});}
async function go(search=''){await p.goto(url+path+search);await loaded();}
async function oracle(name,total){
 await loaded();const data=await p.evaluate(async()=>{const r=await fetch('/api/admin/identity/users'+location.search);if(r.status!==200)throw Error('safe API result unavailable');return (await r.json()).data});
 await p.getByText(`本页 ${data.items.length} 条，共 ${data.total} 条。`,{exact:true}).waitFor({timeout:20000});
 const rows=await p.locator('.ant-table-tbody > tr[data-row-key]').evaluateAll(xs=>xs.map(x=>x.getAttribute('data-row-key')));
 const text=await p.locator('.employee-list').innerText();
 const ok=JSON.stringify(rows)===JSON.stringify(data.items.map(x=>x.id))&&text.includes(`本页 ${data.items.length} 条，共 ${data.total} 条。`)&&(total===undefined||data.total===String(total))&&data.items.every(x=>Object.keys(x).sort().join(',')==='createdAt,displayName,id,loginName,status,updatedAt');
 result.snapshots.push({name,query:new URL(p.url()).search,total:data.total,page:data.page,pageSize:data.pageSize,ids:rows,fields:Object.keys(data.items[0]??{})});record(name,ok,{total:data.total,displayed:rows.length});return data;
}
async function revalidate(){await p.getByRole('link',{name:'当前身份',exact:true}).click();await p.getByRole('button',{name:'刷新当前身份',exact:true}).waitFor();await p.getByRole('link',{name:'员工列表',exact:true}).click();await loaded();}
async function cache(){return p.evaluate(async()=>{const {services}=await import(performance.getEntriesByType('resource').map(x=>x.name).find(x=>/\/src\/app\/router\/router\.ts(?:\?|$)/.test(x)));return services.queryClient.getQueryCache().getAll().filter(q=>q.queryKey[2]==='employees').map(q=>({scope:q.queryKey[0],total:q.state.data?.total,ids:q.state.data?.items?.map(x=>x.id)}))})}
async function selectStatus(value){await p.getByRole('combobox',{name:'状态',exact:true}).click();await p.locator(`.ant-select-item-option[title="${value}"]`).click();}
try{
 await login(p);await p.getByRole('link',{name:'员工列表',exact:true}).click();await oracle('TENANT显示与正式安全接口一致',56);
 await p.screenshot({path:ev+'/desktop.png',fullPage:true});
 record('数据库独立租户统计一致',sql(`SELECT count(*) FROM identity_employee WHERE tenant_id='${tid}'`)==='56');
 const before=result.requests.length;await p.getByLabel('关键词',{exact:true}).fill('验收员工');await pause(350);record('筛选草稿不触发请求且URL未变化',result.requests.length===before&&!new URL(p.url()).search);
 await p.getByLabel('关键词',{exact:true}).press('Enter');await p.waitForURL('**/identity/users?keyword=*');await oracle('Enter一次提交关键词',55);record('Enter无重复列表请求',result.requests.length-before===2,{note:'1次页面查询+1次独立API核对'});
 await p.locator('.ant-pagination-item-2').click();await p.waitForURL('**&page=2');await oracle('服务端第二页',55);
 await p.getByLabel('关键词',{exact:true}).fill('employee-01');await p.getByRole('button',{name:'查询',exact:true}).click();await p.waitForURL('**?keyword=employee-01');await oracle('查询回第一页',1);
 await p.goBack();await p.waitForURL(u=>u.searchParams.get('keyword')==='验收员工'&&u.searchParams.get('page')==='2');await p.waitForFunction(()=>document.querySelector('#keyword')?.value==='验收员工');await loaded();record('后退恢复条件与表单',await p.getByLabel('关键词',{exact:true}).inputValue()==='验收员工'&&new URL(p.url()).searchParams.get('page')==='2');
 await p.goForward();await p.waitForURL(u=>u.searchParams.get('keyword')==='employee-01'&&!u.searchParams.has('page'));await p.waitForFunction(()=>document.querySelector('#keyword')?.value==='employee-01');await loaded();record('前进恢复实际提交条件',await p.getByLabel('关键词',{exact:true}).inputValue()==='employee-01'&&!new URL(p.url()).searchParams.has('page'));
 await p.getByRole('button',{name:'重置',exact:true}).click();await p.waitForURL(url+path);await oracle('重置筛选分页与排序',56);record('重置表单同步',await p.getByLabel('关键词',{exact:true}).inputValue()==='');
 await selectStatus('停用');record('状态草稿未立即查询',!new URL(p.url()).searchParams.has('status'));await p.getByRole('button',{name:'查询',exact:true}).click();await p.waitForURL('**?status=DISABLED');await oracle('真实状态筛选',13);
 await p.getByRole('button',{name:'重置',exact:true}).click();await p.waitForURL(url+path);await loaded();
 await p.locator('.ant-pagination-item-3').click();await p.waitForURL('**?page=3');await loaded();
 await p.locator('.ant-pagination-options-size-changer').click();await p.locator('.ant-select-item-option[title="10 条/页"]').click();await p.waitForURL('**?pageSize=10');await oracle('pageSize变化回第一页',56);record('每页10条',await p.locator('.ant-table-tbody > tr[data-row-key]').count()===10);
 const names=['员工 ID','账号','姓名','状态','创建时间','更新时间'],fields=['id','loginName','displayName','status','createdAt','updatedAt'];
 for(let i=0;i<fields.length;i++){
   await go('?page=2&pageSize=10');
   const cell=p.locator('.ant-table-thead').last().locator('th').filter({hasText:names[i]});await cell.click();
   await p.waitForURL(u=>u.searchParams.get('sortBy')===fields[i]&&u.searchParams.get('sortOrder')==='asc'&&!u.searchParams.has('page'));await oracle(fields[i]+'升序',56);
   record(fields[i]+'排序箭头升序',await cell.getAttribute('aria-sort')==='ascending');
   await cell.click();await p.waitForURL(u=>fields[i]==='createdAt'?!u.searchParams.has('sortBy'):u.searchParams.get('sortOrder')==='desc');await oracle(fields[i]+'降序',56);
   await p.reload();await loaded();record(fields[i]+'刷新恢复排序箭头',await p.locator('.ant-table-thead').last().locator('th').filter({hasText:names[i]}).getAttribute('aria-sort')==='descending');
   if(fields[i]!=='createdAt')await p.locator('.ant-table-thead').last().locator('th').filter({hasText:names[i]}).click();await p.waitForURL(u=>!u.searchParams.has('sortBy'));await loaded();record(fields[i]+'清除恢复默认',true);
 }
 await go('?keyword=employee-01&status=ACTIVE&pageSize=10&sortBy=loginName&sortOrder=asc');await oracle('直接带search地址与刷新恢复',1);await p.reload();await loaded();record('刷新表单恢复',await p.getByLabel('关键词',{exact:true}).inputValue()==='employee-01');
 await go('?keyword=%25_');await oracle('特殊关键词按字面量匹配',1);
 await go('?keyword=不存在的技术员工');await oracle('真实空筛选结果',0);record('筛选空态区别失败',await p.getByText('没有符合筛选条件的员工',{exact:true}).isVisible());
 await go('?page=99');await oracle('真实超末页replace回退',56);record('末页纠正有界',new URL(p.url()).searchParams.get('page')==='3');
 await go('?keyword=不存在的技术员工&page=99');await oracle('total零回第一页',0);record('零总数纠正到1',!new URL(p.url()).searchParams.has('page'));
 await go('?page=2&page=3&storeId=not-a-store');await oracle('重复URL恢复默认且有提示',56);record('重复敏感参数提示可见',await p.getByText('查询地址包含无效或重复参数，已恢复默认条件。',{exact:true}).isVisible());
 await go('?keyword=001');record('数字字面量关键词不被JSON解析改写',await p.getByLabel('关键词',{exact:true}).inputValue()==='001');
 await go();const old=await cache();scope('SELF');await revalidate();await oracle('SELF只显示当前STAFF本人',1);record('同版本范围变化清旧TENANT缓存',old.length>0&&(await cache()).every(q=>q.total!=='56'));
 scope('STORES');await revalidate();await oracle('STORES限定任一门店交集',20);
 record('STORES与数据库独立交集一致',sql(`SELECT count(*) FROM identity_employee e WHERE e.tenant_id='${tid}' AND EXISTS(SELECT 1 FROM identity_employee_store s WHERE s.tenant_id=e.tenant_id AND s.employee_id=e.id AND s.store_id='${assoc.store_id}')`)==='20');
 sql(`DELETE FROM identity_employee_store WHERE id='${assoc.id}'`);await revalidate();await oracle('撤销有效门店后范围清空',0);record('无范围数据空态',await p.getByText('当前授权范围内暂无员工',{exact:true}).isVisible());
 sql(`INSERT INTO identity_employee_store(id,tenant_id,employee_id,store_id) VALUES('${assoc.id}','${tid}','${op}','${assoc.store_id}')`);scope('TENANT');await revalidate();await oracle('范围恢复重新查询',56);
 sql(`REVOKE SELECT(${cols}) ON identity_employee FROM pet_runtime`);await p.getByRole('button',{name:'刷新列表',exact:true}).click();await p.getByText('刷新失败，以下仍为上次成功结果',{exact:true}).waitFor({timeout:20000});
 record('真实DB503刷新失败保留旧行与有效身份',await p.locator('.ant-table-tbody > tr[data-row-key]').count()===20&&!p.url().includes('/login'));await p.screenshot({path:ev+'/refresh-failure.png',fullPage:true});
 sql(`GRANT SELECT(${cols}) ON identity_employee TO pet_runtime`);await p.getByRole('button',{name:'重试',exact:true}).click();await oracle('真实DB恢复无需重登',56);
 sql(`REVOKE SELECT(${cols}) ON identity_employee FROM pet_runtime`);await p.getByLabel('关键词',{exact:true}).fill('employee-01');await p.getByLabel('关键词',{exact:true}).press('Enter');await p.getByText('员工列表加载失败',{exact:true}).waitFor({timeout:20000});record('真实首次失败无旧条件行和假空态',await p.locator('.ant-table-tbody > tr[data-row-key]').count()===0&&await p.getByText('没有符合筛选条件的员工',{exact:true}).count()===0);sql(`GRANT SELECT(${cols}) ON identity_employee TO pet_runtime`);await p.getByRole('button',{name:'重试',exact:true}).click();await oracle('首次失败重试恢复',1);
 await go();sql(`DELETE FROM identity_role_permission WHERE tenant_id='${tid}' AND role_id='${role}' AND permission_code='identity:user:list'`);
 await p.getByRole('button',{name:'刷新列表',exact:true}).click();await p.getByText('当前账号没有访问权限',{exact:true}).waitFor();record('权限撤销后旧行与缓存隐藏清理',await p.locator('.ant-table-tbody > tr[data-row-key]').count()===0&&(await cache()).length===0);
 const req=result.requests.length;await p.goto(url+path);await p.getByText('当前账号没有访问权限',{exact:true}).waitFor();record('无权限直接地址不发送列表请求',result.requests.length===req);await p.goto(url+'/admin/security');await p.getByRole('heading',{name:'账号安全',exact:true}).waitFor();record('无list本人安全入口保持独立',true);
 restoreGrant();await go();
 await p.setViewportSize({width:390,height:844});await p.screenshot({path:ev+'/list-390.png',fullPage:true});const size=await p.evaluate(()=>({width:innerWidth,scroll:document.documentElement.scrollWidth,table:document.querySelector('.employee-table-region .ant-table-content').scrollWidth}));record('390px页面无整体横向溢出且表格独立滚动',size.scroll<=size.width&&size.table>size.width,size);
 await p.getByLabel('关键词',{exact:true}).focus();await p.keyboard.type('employee-01');await p.keyboard.press('Enter');await oracle('390px键盘查询',1);await p.getByRole('button',{name:'重置',exact:true}).click();await loaded();const item=p.locator('.ant-pagination-item-2');await item.focus();await p.keyboard.press('Enter');await p.waitForURL('**?page=2');await oracle('390px键盘分页',56);
 record('实际行稳定rowKey使用DTO id',await p.locator('.ant-table-tbody > tr[data-row-key]').evaluateAll(xs=>xs.every(x=>/^[a-f0-9-]{36}$/.test(x.dataset.rowKey))));
 const platform=await browser.newContext({viewport:{width:1280,height:900}}),platformPage=await platform.newPage();const platformRequests=[];platformPage.on('request',r=>{if(new URL(r.url()).pathname==='/api/admin/identity/users')platformRequests.push(r.url())});await login(platformPage,'platform');await platformPage.goto(url+path);await platformPage.waitForURL('**/admin/login*');record('仅平台Cookie不能打开STAFF列表或发列表请求',platformRequests.length===0);await platform.close();
 // 同源标签真实切换租户，当前页通过合法路由重验；旧scope不得作占位。
 const b=await context.newPage();await b.goto(url+'/admin');await b.getByRole('button',{name:'当前账号菜单'}).click();await b.getByRole('menuitem',{name:'退出当前会话',exact:true}).click();await b.waitForURL('**/admin/login*');await login(b,'admin','p07-02-second');await p.getByRole('link',{name:'当前身份',exact:true}).click();await p.getByRole('button',{name:'刷新当前身份',exact:true}).waitFor();await p.getByRole('link',{name:'员工列表',exact:true}).click();await oracle('同源切换租户不沿用旧员工占位',1);record('跨身份缓存无旧租户', (await cache()).every(q=>q.scope.tenantId!==tid));await b.close();
 const unexpected=result.consoleErrors.filter(x=>!/^Failed to load resource: the server responded with a status of (401|403|404|503)/.test(x));record('无新增JavaScript异常与组件控制台错误',result.pageErrors.length===0&&unexpected.length===0,{expectedNegativeHttpConsole:result.consoleErrors.length,pageErrors:result.pageErrors.length,unexpected});
} catch(e){result.exitCode=1;result.error=String(e.message).split('\n').slice(0,8).join('\n').replaceAll(password,'<REDACTED>').replaceAll(platformPassword,'<REDACTED>');save();}
finally {restoreGrant();await p.evaluate(async()=>{const {services}=await import(performance.getEntriesByType('resource').map(x=>x.name).find(x=>/\/src\/app\/router\/router\.ts(?:\?|$)/.test(x)));await services.auth.logout('STAFF')}).catch(()=>{});await browser.close();save()}
console.log(JSON.stringify({exitCode:result.exitCode,scenarios:result.scenarios.map(s=>({name:s.name,result:s.result})),error:result.error}));process.exitCode=result.exitCode;
