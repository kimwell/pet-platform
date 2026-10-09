import pathlib,os,subprocess,json,secrets,time,socket,shutil,urllib.request
from datetime import datetime,timezone
root=pathlib.Path('/Users/kimwell/work/pet-platform');local=root/'.local-data/b02';ev=root/'docs/testing/evidence/B02/completion';local.chmod(0o700)
records=[];secret_values=[secrets.token_urlsafe(30) for _ in range(8)]
admin,dbpass,redispass,bootpass,platformdbpass,staffpass,platformpass,readerpass=secret_values
node='/var/folders/5y/tt1690cx7k1gcq12252yf5rh0000gn/T/enterprise-scaffold-p01-01-plwt53cm/tools/node-v24.21.0-darwin-arm64/bin'
jdk='/Users/kimwell/.vscode/extensions/redhat.java-1.56.0-darwin-arm64/jre/21.0.12.1-macosx-aarch64'
def secure(name,value):
 p=local/name;p.write_text(value);p.chmod(0o600);return p
def run(label,args,data=None,env=None):
 start=time.time();p=subprocess.run(args,cwd=root,input=data,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,env=env,timeout=90)
 summary=p.stdout
 for value in secret_values:
  summary=summary.replace(value,'<私有值已删除>')
 item={'label':label,'cwd':str(root),'command':args,'exitCode':p.returncode,'durationSeconds':round(time.time()-start,2),'summary':summary[-1200:]}
 records.append(item);(ev/'runtime-setup.json').write_text(json.dumps(records,ensure_ascii=False,indent=2))
 if p.returncode:raise RuntimeError(label+'失败；见脱敏证据')
 return p.stdout.strip()
secure('credentials.json',json.dumps({'staffPassword':staffpass,'platformPassword':platformpass,'readerPassword':readerpass,'newPassword':secrets.token_urlsafe(30)}))
pg='pet-b02-pg';redis='pet-b02-redis';db='b02_acceptance'
run('既有容器只读保全基线',['docker','ps','-a','--format','{{.Names}}\t{{.State}}'])
for port in [18101,18102]:
 with socket.socket() as s:s.bind(('127.0.0.1',port))
pg_env=secure('postgres.env',f'POSTGRES_DB={db}\nPOSTGRES_USER=postgres\nPOSTGRES_PASSWORD={admin}\n')
redis_conf=secure('redis.conf',f'bind 0.0.0.0\nprotected-mode yes\nrequirepass {redispass}\nsave ""\nappendonly no\n');redis_conf.chmod(0o644)
run('本轮冻结PG',['docker','run','-d','--rm','--name',pg,'--label','pet.validation=B02','--tmpfs','/var/lib/postgresql/data','--env-file',str(pg_env),'-p','127.0.0.1::5432','postgres:17.11-bookworm@sha256:3645570cccdfa447589da9f57dd740faa29b30938e861289a5574b6ca6b03826','-c','log_statement=none','-c','log_min_error_statement=panic','-c','log_parameter_max_length=0','-c','log_parameter_max_length_on_error=0'])
run('本轮冻结Redis',['docker','run','-d','--rm','--name',redis,'--label','pet.validation=B02','--tmpfs','/data','-v',str(redis_conf)+':/tmp/validation.conf:ro','-p','127.0.0.1::6379','redis:8.2.10-bookworm@sha256:47670742d7924adbcdb404d1288b6327815b23141969c0939b8e5d61f78c2634','redis-server','/tmp/validation.conf'])
psql=['docker','exec','-i',pg,'psql','-X','-qAt','-v','ON_ERROR_STOP=1','-U','postgres','-d',db]
for _ in range(60):
 if subprocess.run(psql,input='SELECT 1;',text=True,capture_output=True).returncode==0:break
 time.sleep(.2)
pgport=int(run('PG回环端口',['docker','port',pg,'5432']).split(':')[-1]);redisport=int(run('Redis回环端口',['docker','port',redis,'6379']).split(':')[-1])
for name in ['identity','platform','customer','control-management']:run('正式角色:'+name,psql,(root/f'infra/database/provision-{name}-roles.sql').read_text())
sql=f"CREATE ROLE b02_migration LOGIN PASSWORD '{dbpass}' INHERIT; GRANT pet_migrator TO b02_migration WITH INHERIT TRUE, SET TRUE; CREATE ROLE b02_runtime LOGIN PASSWORD '{dbpass}' INHERIT; GRANT pet_runtime TO b02_runtime WITH INHERIT TRUE, SET FALSE; CREATE ROLE b02_bootstrap LOGIN PASSWORD '{bootpass}' NOINHERIT; GRANT pet_bootstrap TO b02_bootstrap WITH INHERIT FALSE, SET TRUE; CREATE ROLE b02_platform_bootstrap LOGIN PASSWORD '{platformdbpass}' NOINHERIT; GRANT pet_platform_bootstrap TO b02_platform_bootstrap WITH INHERIT FALSE, SET TRUE;"
run('本轮独立受限登录身份',psql,sql)
env=os.environ.copy();env['JAVA_HOME']=jdk;url=f'jdbc:postgresql://127.0.0.1:{pgport}/{db}'
env.update(PET_MIGRATION_DATABASE_URL=url,PET_MIGRATION_DATABASE_USERNAME='b02_migration',PET_MIGRATION_DATABASE_PASSWORD=dbpass,PET_BOOTSTRAP_DATABASE_URL=url,PET_BOOTSTRAP_DATABASE_USERNAME='b02_bootstrap',PET_BOOTSTRAP_DATABASE_PASSWORD=bootpass,PET_PLATFORM_BOOTSTRAP_DATABASE_URL=url,PET_PLATFORM_BOOTSTRAP_DATABASE_USERNAME='b02_platform_bootstrap',PET_PLATFORM_BOOTSTRAP_DATABASE_PASSWORD=platformdbpass)
secure('command-env.json',json.dumps({k:v for k,v in env.items() if k.startswith('PET_') and ('DATABASE' in k)}))
run('生产JAR独立迁移',['scripts/backend-identity.sh','migrate'],env=env)
run('平台首次正式初始化',['scripts/backend-identity.sh','platform-bootstrap','--admin-login','b02-platform','--admin-name','B02验收平台管理员','--password-stdin'],platformpass+'\n',env)
run('平台管理显式升级',['scripts/backend-identity.sh','platform-management-upgrade'],env=env)
run('保全对照租户B正式初始化',['scripts/backend-identity.sh','bootstrap','--tenant-code','b02-second','--tenant-name','B02对照租户','--admin-login','admin','--password-stdin'],staffpass+'\n',env)
shutil.copy2(root/'apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar',local/'backend.jar')
wechat=(root/'.local-data/p05-05-wechat/application-local.yaml').read_text().replace('p05-05-wechat-acceptance','b02-acceptance')
secure('wechat.yaml',wechat)
backend_env={k:v for k,v in os.environ.items() if k in ['PATH','HOME','USER','TMPDIR']}
backend_env.update(SPRING_PROFILES_ACTIVE='local',SPRING_CONFIG_ADDITIONAL_LOCATION='file:'+str(local/'wechat.yaml'),PET_DATABASE_URL=url,PET_DATABASE_USERNAME='b02_runtime',PET_DATABASE_PASSWORD=dbpass,PET_DATABASE_MIGRATION_ENABLED='false',PET_REDIS_HOST='127.0.0.1',PET_REDIS_PORT=str(redisport),PET_REDIS_PASSWORD=redispass,PET_PUBLIC_ORIGIN='http://127.0.0.1:18102',SERVER_PORT='18101')
backend=subprocess.Popen([jdk+'/bin/java','-jar',str(local/'backend.jar')],cwd=root,env=backend_env,stdout=open(local/'backend.log','w'),stderr=subprocess.STDOUT,start_new_session=True)
resources={'backendPid':backend.pid,'pg':pg,'redis':redis,'backendPort':18101,'webPort':18102,'pgPort':pgport,'redisPort':redisport,'database':db};secure('resources.json',json.dumps(resources))
for _ in range(100):
 try:
  if urllib.request.urlopen('http://127.0.0.1:18101/actuator/health/readiness',timeout=2).status==200:break
 except Exception:time.sleep(.3)
else:raise RuntimeError('生产JAR未就绪；忽略日志保留')
web_env=os.environ.copy();web_env.update(PATH=node+':'+str(root/'.local-data/p07-02/tools/bin')+':'+web_env['PATH'],DEV_BACKEND_ORIGIN='http://127.0.0.1:18101')
web=subprocess.Popen(['pnpm','--filter','@pet/admin-web','dev','--port','18102'],cwd=root,env=web_env,stdout=open(local/'web.log','w'),stderr=subprocess.STDOUT,start_new_session=True);resources['webPid']=web.pid;secure('resources.json',json.dumps(resources))
for _ in range(80):
 try:
  if urllib.request.urlopen('http://127.0.0.1:18102',timeout=2).status==200:break
 except Exception:time.sleep(.25)
else:raise RuntimeError('Vite未就绪')
records.append({'label':'正式local运行','cwd':str(root),'command':[jdk+'/bin/java','-jar',str(local/'backend.jar')],'exitCode':None,'status':'STARTED','summary':'readiness200；PLATFORM正式密码认证；独立受限runtime/PG/Redis；正式WxJava配置，无测试profile、Mock或测试API'})
records.append({'label':'本轮Vite','cwd':str(root),'command':['pnpm','--filter','@pet/admin-web','dev','--port','18102'],'exitCode':None,'status':'STARTED','summary':'HTTP200；同源代理至18101'})
(ev/'runtime-setup.json').write_text(json.dumps(records,ensure_ascii=False,indent=2));print('B02正式生产JAR与Web就绪，凭据未输出')
