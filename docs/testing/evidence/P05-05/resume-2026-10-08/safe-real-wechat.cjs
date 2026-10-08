/* 独立进程真实验收：敏感值只在内存；stdout仅固定字段白名单，不订阅工具console。 */
'use strict';
const {createRequire} = require('node:module');
const {readFileSync} = require('node:fs');
const {spawnSync} = require('node:child_process');
const {randomBytes} = require('node:crypto');
const net = require('node:net');
const settings = JSON.parse(readFileSync(0, 'utf8'));
const writeResult = process.stdout.write.bind(process.stdout);
// 第三方库输出不进入stdout；协议调试明确关闭，仅最终自建JSON可输出。
process.stdout.write = () => true;
process.env.DEBUG = '';
const automator = createRequire(settings.toolPath + '/package.json')('miniprogram-automator');
const result = {startedAt:new Date().toISOString(),steps:[],repeatCustomerReuse:'NOT_EXECUTED',codeLogged:false,tokenLogged:false,upstreamAttempts:0};
let mini;
let token;
let token2;
const liveTokens = new Set();
let stage = 'TOOL_LAUNCH';
function assertion(value,label) { if (!value) { const error = new Error(); error.safeLabel=label; throw error; } }
function uuid(value) { assertion(typeof value==='string'&&/^[a-f0-9-]{36}$/.test(value),'IDENTITY_UUID_INVALID'); return value; }
function classify(error) {
  if(error.safeLabel)return error.safeLabel;
  if(stage.startsWith('TOOL')||stage.startsWith('FRESH_CODE'))return 'DEVTOOLS_LOGIN_OR_AUTOMATION_BLOCKED';
  if(stage.startsWith('REDIS'))return 'LOCAL_REDIS_CHECK_FAILED';
  if(stage.startsWith('DATABASE'))return 'LOCAL_DATABASE_CHECK_FAILED';
  return 'LOCAL_HTTP_OR_ASSERTION_FAILED';
}
async function http(method,path,body,currentToken) {
  const trace=randomBytes(16).toString('hex');
  const headers={'X-Trace-Id':trace};
  if(body)headers['Content-Type']='application/json';
  if(currentToken)headers['X-Customer-Token']='Bearer '+currentToken;
  let response;
  try {response=await fetch(settings.backendUrl+path,{method,headers,body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(15000)});}
  catch {const error=new Error();error.safeLabel='LOCAL_HTTP_NETWORK_OR_TIMEOUT';throw error;}
  const parsed=await response.json();
  const metadata={method,path,httpStatus:response.status,traceId:parsed.traceId,traceHeaderMatches:response.headers.get('X-Trace-Id')===trace,errorCode:parsed.error?.code??null,noStore:response.headers.get('Cache-Control')?.includes('no-store')??false,setCookiePresent:response.headers.has('Set-Cookie')};
  assertion(metadata.traceHeaderMatches,'TRACE_MISMATCH');
  return {metadata,parsed};
}
function identityAssertions(identity) {
  assertion(identity.principalType==='CUSTOMER','CUSTOMER_DOMAIN_REQUIRED');
  assertion(identity.tenantId===settings.tenantId,'TENANT_MISMATCH');
  assertion(JSON.stringify(identity.permissionCodes)===JSON.stringify(['customer:session:manage']),'CUSTOMER_PERMISSION_MISMATCH');
  assertion(identity.authorizedStoreIds.length===0,'CUSTOMER_STORE_RIGHTS');
  assertion(identity.dataScope.grants.length===1&&identity.dataScope.grants[0].permissionCode==='customer:session:manage'&&identity.dataScope.grants[0].scopes.length===1&&identity.dataScope.grants[0].scopes[0].type==='SELF','CUSTOMER_SELF_REQUIRED');
  assertion(!('openId'in identity)&&!('unionId'in identity)&&!('session_key'in identity),'SECRET_IDENTITY_FIELDS');
  return {principalId:uuid(identity.principalId),tenantId:uuid(identity.tenantId),principalType:'CUSTOMER',scope:'SELF',staffPermissions:false,platformPermissions:false,authorizedStoreCount:0};
}
function database(customerId) {
  stage='DATABASE_RELATION';
  const query="select json_build_object('customerCount',(select count(*) from public.customer_subject),'bindingCount',(select count(*) from public.customer_wechat_binding),'activeCustomerForTenant',(select count(*) from public.customer_subject where id='"+uuid(customerId)+"'::uuid and tenant_id='"+uuid(settings.tenantId)+"'::uuid and status='ACTIVE'),'correctActiveBinding',(select count(*) from public.customer_wechat_binding b join public.customer_subject c on c.id=b.customer_id and c.tenant_id=b.tenant_id where c.id='"+customerId+"'::uuid and c.tenant_id='"+settings.tenantId+"'::uuid and b.app_id='wx9bcab67d52e2ee04' and b.status='ACTIVE'));";
  const command=spawnSync('docker',['exec','-i',settings.pgContainer,'psql','-X','-qAt','-v','ON_ERROR_STOP=1','-U','postgres','-d','p05_05_wechat_acceptance'],{input:query,encoding:'utf8',timeout:10000});
  assertion(command.status===0,'DATABASE_OBSERVER_FAILED');
  const projection=JSON.parse(command.stdout);
  assertion(projection.customerCount===1&&projection.bindingCount===1&&projection.activeCustomerForTenant===1&&projection.correctActiveBinding===1,'DATABASE_CUSTOMER_BINDING_MISMATCH');
  return {...projection,observerExitCode:command.status,openIdSelected:false,unionIdSelected:false};
}
function redisCommand(args) {
  return new Promise((resolve,reject)=>{
    const socket=net.createConnection({host:'127.0.0.1',port:settings.redisPort});
    const queue=[['AUTH',settings.redisPassword],args];
    let buffer=Buffer.alloc(0),index=0;
    const fail=()=>{socket.destroy();reject(new Error());};
    socket.setTimeout(5000,fail);socket.on('error',fail);
    function send(){const values=queue[index].map(String);socket.write('*'+values.length+'\r\n'+values.map(s=>'$'+Buffer.byteLength(s)+'\r\n'+s+'\r\n').join(''));}
    socket.on('connect',send);
    socket.on('data',chunk=>{
      buffer=Buffer.concat([buffer,chunk]);const end=buffer.indexOf('\r\n');if(end<0)return;
      const marker=String.fromCharCode(buffer[0]);const line=buffer.subarray(1,end).toString();let length=end+2,value;
      if(marker==='-')return fail();
      if(marker==='$') {const size=Number(line);if(size<0)value=null;else{if(buffer.length<length+size+2)return;value=buffer.subarray(length,length+size).toString();length+=size+2;}}
      else if(marker===':')value=Number(line);else if(marker==='+')value=line;else return fail();
      buffer=buffer.subarray(length);
      if(index===0){index=1;send();}else{socket.end();resolve(value);}
    });
  });
}
async function session(currentToken,customerId) {
  stage='REDIS_CUSTOMER_SESSION';
  const key='pet:local:customer:customer:token-session:'+currentToken;
  const raw=await redisCommand(['GET',key]);assertion(raw!==null,'REDIS_CUSTOMER_SESSION_MISSING');
  const wire=JSON.parse(raw),ttl=await redisCommand(['TTL',key]);
  assertion(wire.domain==='customer'&&wire.data.principal===customerId&&wire.data.tenant===settings.tenantId&&wire.data.channel==='MINIPROGRAM','REDIS_SESSION_DOMAIN_MISMATCH');
  assertion(ttl>0&&ttl<=2592000,'REDIS_CUSTOMER_TTL_INVALID');
  return {customerTokenSessionExists:true,domain:'CUSTOMER',tenantMatches:true,principalMatches:true,channel:'MINIPROGRAM',ttlWithinFrozenPolicy:true,sessionValueLogged:false};
}
async function freshLogin(number) {
  stage='FRESH_CODE_'+number;
  let wx=await mini.callWxMethod('login',{timeout:10000});
  let code=wx?.code;wx=null;
  assertion(typeof code==='string'&&/^[A-Za-z0-9_-]{1,256}$/.test(code),'WX_LOGIN_CODE_UNAVAILABLE');
  result.currentDeveloperPermission='RUNTIME_VERIFIED_BY_WX_LOGIN';
  stage='REAL_LOGIN_'+number;result.upstreamAttempts++;
  let reply;
  try{reply=await http('POST','/api/customer/auth/wechat/login',{tenantCode:settings.tenantCode,entryId:settings.entryId,code});}
  finally{code=null;}
  result.steps.push({name:number===1?'realWechatLogin':'repeatWechatLogin',...reply.metadata});
  if(reply.metadata.httpStatus!==200){
    const error=new Error();const code=reply.metadata.errorCode;
    error.safeLabel=({WECHAT_CONFIGURATION_MISSING:'APPID_OR_APPSECRET_CONFIGURATION',WECHAT_CODE_INVALID:'FRESH_CODE_INVALID_OR_USED',LOGIN_FAILED:'TENANT_ENTRY_OR_IDENTITY_REJECTED',WECHAT_RESULT_UNCERTAIN:'WECHAT_NETWORK_OR_TIMEOUT_UNCERTAIN',WECHAT_UPSTREAM_ERROR:'WECHAT_UPSTREAM_CONFIGURATION_OR_LIMIT',WECHAT_RESPONSE_INVALID:'WECHAT_RESPONSE_INVALID',DEPENDENCY_UNAVAILABLE:'LOCAL_DATABASE_OR_REDIS_UNAVAILABLE'})[code]??'REAL_LOGIN_REJECTED';throw error;
  }
  assertion(!reply.metadata.setCookiePresent&&reply.metadata.noStore,'LOGIN_TRANSPORT_MISMATCH');
  const {identity,token:issued}=reply.parsed.data;
  assertion(issued.headerName==='X-Customer-Token'&&typeof issued.value==='string','CUSTOMER_TOKEN_HEADER_MISMATCH');
  liveTokens.add(issued.value);
  return {identity:identityAssertions(identity),token:issued.value};
}
async function logout(currentToken,label) {
  stage='LOGOUT_'+label;const reply=await http('POST','/api/customer/auth/logout',null,currentToken);
  result.steps.push({name:label,...reply.metadata});assertion(reply.metadata.httpStatus===200&&reply.parsed.data===null,'LOGOUT_FAILED');liveTokens.delete(currentToken);
}
(async()=>{
  try {
    mini=settings.existingAutomationPort
      ? await automator.connect({wsEndpoint:'ws://127.0.0.1:'+settings.existingAutomationPort})
      : await automator.launch({cliPath:settings.cliPath,projectPath:settings.probePath,port:settings.autoPort,timeout:30000,trustProject:true});
    assertion(!!mini,'AUTOMATION_LAUNCH_UNAVAILABLE');
    stage='TOOL_APPID';const appId=await mini.evaluate(()=>wx.getAccountInfoSync().miniProgram.appId);
    assertion(appId==='wx9bcab67d52e2ee04','DEVTOOLS_APPID_MISMATCH');result.appIdMatches=true;
    const system=await mini.systemInfo();result.libraryVersion=system.SDKVersion;assertion(system.SDKVersion==='3.17.2','DEVTOOLS_LIBRARY_MISMATCH');
    const first=await freshLogin(1);token=first.token;result.customerIdentity=first.identity;
    result.databaseRelation=database(first.identity.principalId);result.customerSession=await session(token,first.identity.principalId);
    result.steps.push({name:'customerSession',httpStatus:null,status:'RUNTIME_VERIFIED',pgRelationCorrect:true,redisCustomerSessionExists:true});
    stage='CUSTOMER_ME';const me=await http('GET','/api/customer/auth/me',null,token);result.steps.push({name:'currentIdentity',...me.metadata});
    assertion(me.metadata.httpStatus===200,'CURRENT_IDENTITY_FAILED');identityAssertions(me.parsed.data);assertion(me.parsed.data.principalId===first.identity.principalId,'CURRENT_CUSTOMER_MISMATCH');
    await logout(token,'currentLogout');
    stage='OLD_TOKEN_ME';const old=await http('GET','/api/customer/auth/me',null,token);result.steps.push({name:'oldTokenAfterLogout',...old.metadata});
    assertion(old.metadata.httpStatus===401&&old.metadata.errorCode==='SESSION_EXPIRED','OLD_CUSTOMER_TOKEN_STILL_VALID');
    stage='REDIS_AFTER_LOGOUT';assertion(await redisCommand(['EXISTS','pet:local:customer:customer:token-session:'+token])===0,'OLD_REDIS_TOKEN_SESSION_REMAINS');token=null;
    result.oldSessionInvalidated=true;
    const second=await freshLogin(2);token2=second.token;
    assertion(second.identity.principalId===first.identity.principalId,'REPEAT_LOGIN_CREATED_NEW_CUSTOMER');result.repeatDatabaseRelation=database(second.identity.principalId);
    result.repeatCustomerReuse='RUNTIME_VERIFIED';await logout(token2,'repeatSessionCleanupLogout');token2=null;
    result.status='PASS';result.fourSteps='RUNTIME_VERIFIED';
  } catch(error) {result.status='BLOCKED';result.failedStage=stage;result.failureCategory=classify(error);}
  finally {
    // 只清理本进程签发的设备；不重试code，也不清理日常数据。
    result.unreturnedSessionsCleanup=[];
    for(const currentToken of liveTokens){try{const response=await http('POST','/api/customer/auth/logout',null,currentToken);result.unreturnedSessionsCleanup.push({httpStatus:response.metadata.httpStatus});}catch{result.unreturnedSessionsCleanup.push({status:'NOT_CONFIRMED'});}}
    token=null;token2=null;liveTokens.clear();settings.redisPassword=null;
    if(mini){try{await mini.close();result.probeClosed=true;}catch{mini.disconnect();result.probeClosed=false;}}
    result.finishedAt=new Date().toISOString();
    writeResult(JSON.stringify(result)+'\n');
    process.exit(result.status==='PASS'?0:1);
  }
})();
