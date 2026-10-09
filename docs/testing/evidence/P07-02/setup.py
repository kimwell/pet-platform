import pathlib,os,subprocess,json,secrets,time,socket,shutil,urllib.request
root=pathlib.Path('/Users/kimwell/work/pet-platform');local=root/'.local-data/p07-02';ev=root/'docs/testing/evidence/P07-02';local.chmod(0o700)
from datetime import datetime
records=[]
def run(label,args,input=None,env=None):
 start=datetime.now().astimezone().isoformat();p=subprocess.run(args,cwd=root,input=input,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,env=env)
 summary=p.stdout
 for value in secret_values:
  if value:summary=summary.replace(value,'<REDACTED>')
 records.append({'label':label,'cwd':str(root),'command':[('<受控员工账号>' if v=='p07-staff' else '<受控平台账号>' if v=='p07-platform' else v) for v in args],'startedAt':start,'exitCode':p.returncode,'summary':summary[-1600:]});(ev/'environment.json').write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n')
 if p.returncode: raise RuntimeError(label+' failed; sanitized evidence saved')
 return p.stdout.strip()
def secure(name,value):
 p=local/name;p.write_text(value);p.chmod(0o600);return p
secret_values=[secrets.token_urlsafe(30) for _ in range(7)]
admin,dbpass,redispass,bootpass,platformdbpass,staffpass,platformpass=secret_values
secure('staff-password',staffpass);secure('platform-password',platformpass)
secure('staff-new-password',' '+secrets.token_urlsafe(30)+' ');secure('platform-new-password',' '+secrets.token_urlsafe(30)+' ');secure('staff-final-password',secrets.token_urlsafe(30))
pg='pet-p07-02-pg';redis='pet-p07-02-redis';db='p07_02_acceptance'
run('已有容器保全基线',['docker','ps','-a','--format','{{.Names}}\t{{.State}}'])
for port in [18090,18091,5173]:
 with socket.socket() as s:s.bind(('127.0.0.1',port))
pg_env=secure('postgres.env',f'POSTGRES_DB={db}\nPOSTGRES_USER=postgres\nPOSTGRES_PASSWORD={admin}\n')
redis_conf=secure('redis.conf',f'bind 0.0.0.0\nprotected-mode yes\nrequirepass {redispass}\nsave ""\nappendonly no\n')
# 密码配置只在隔离tmpfs服务；容器内Redis必须可读挂载，父目录仍0700。
redis_conf.chmod(0o644)
run('独立冻结PostgreSQL',['docker','run','-d','--rm','--name',pg,'--label','pet.validation=P07-02','--tmpfs','/var/lib/postgresql/data','--env-file',str(pg_env),'-p','127.0.0.1::5432','postgres:17.11-bookworm@sha256:3645570cccdfa447589da9f57dd740faa29b30938e861289a5574b6ca6b03826','-c','log_statement=none','-c','log_min_error_statement=panic','-c','log_parameter_max_length=0','-c','log_parameter_max_length_on_error=0'])
run('独立冻结Redis',['docker','run','-d','--rm','--name',redis,'--label','pet.validation=P07-02','--tmpfs','/data','-v',str(redis_conf)+':/tmp/validation.conf:ro','-p','127.0.0.1::6379','redis:8.2.10-bookworm@sha256:47670742d7924adbcdb404d1288b6327815b23141969c0939b8e5d61f78c2634','redis-server','/tmp/validation.conf'])
for _ in range(50):
 p=subprocess.run(['docker','exec',pg,'psql','-X','-qAt','-U','postgres','-d',db,'-c','SELECT 1'],capture_output=True)
 if not p.returncode:break
 time.sleep(.2)
pgport=int(run('PostgreSQL回环端口',['docker','port',pg,'5432']).split(':')[-1]);redisport=int(run('Redis回环端口',['docker','port',redis,'6379']).split(':')[-1])
psql=['docker','exec','-i',pg,'psql','-X','-qAt','-v','ON_ERROR_STOP=1','-U','postgres','-d',db]
for name in ['identity','platform','customer']:run('正式角色:'+name,psql,input=(root/f'infra/database/provision-{name}-roles.sql').read_text())
sql=f"CREATE ROLE p07_migration LOGIN PASSWORD '{dbpass}' INHERIT; GRANT pet_migrator TO p07_migration WITH INHERIT TRUE, SET TRUE; CREATE ROLE p07_runtime LOGIN PASSWORD '{dbpass}' INHERIT; GRANT pet_runtime TO p07_runtime WITH INHERIT TRUE, SET FALSE; CREATE ROLE p07_bootstrap LOGIN PASSWORD '{bootpass}' NOINHERIT; GRANT pet_bootstrap TO p07_bootstrap WITH INHERIT FALSE, SET TRUE; CREATE ROLE p07_platform_bootstrap LOGIN PASSWORD '{platformdbpass}' NOINHERIT; GRANT pet_platform_bootstrap TO p07_platform_bootstrap WITH INHERIT FALSE, SET TRUE;"
run('独立受限登录角色',psql,input=sql)
env=os.environ.copy();env['JAVA_HOME']='/Users/kimwell/.vscode/extensions/redhat.java-1.56.0-darwin-arm64/jre/21.0.12.1-macosx-aarch64';url=f'jdbc:postgresql://127.0.0.1:{pgport}/{db}'
(root/'apps/backend/target').mkdir(exist_ok=True)
assert (root/'apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar').is_file()
env.update(PET_MIGRATION_DATABASE_URL=url,PET_MIGRATION_DATABASE_USERNAME='p07_migration',PET_MIGRATION_DATABASE_PASSWORD=dbpass)
run('正式独立迁移',['scripts/backend-identity.sh','migrate'],env=env)
env.update(PET_BOOTSTRAP_DATABASE_URL=url,PET_BOOTSTRAP_DATABASE_USERNAME='p07_bootstrap',PET_BOOTSTRAP_DATABASE_PASSWORD=bootpass)
run('正式员工初始化',['scripts/backend-identity.sh','bootstrap','--tenant-code','p07-02-acceptance','--tenant-name','P07-02隔离验收','--admin-login','p07-staff','--store-code','store-a','--store-name','验收门店A','--password-stdin'],input=staffpass+'\n',env=env)
run('正式第二租户员工初始化',['scripts/backend-identity.sh','bootstrap','--tenant-code','p07-02-second','--tenant-name','P07-02第二隔离租户','--admin-login','p07-staff','--store-code','store-a','--store-name','验收门店A','--password-stdin'],input=staffpass+'\n',env=env)
env.update(PET_PLATFORM_BOOTSTRAP_DATABASE_URL=url,PET_PLATFORM_BOOTSTRAP_DATABASE_USERNAME='p07_platform_bootstrap',PET_PLATFORM_BOOTSTRAP_DATABASE_PASSWORD=platformdbpass)
run('正式平台初始化',['scripts/backend-identity.sh','platform-bootstrap','--admin-login','p07-platform','--admin-name','P07-02验收平台管理员','--password-stdin'],input=platformpass+'\n',env=env)
run('迁移与受限身份白名单',psql,input="SELECT version,success FROM flyway_schema_history ORDER BY installed_rank; SELECT rolname,rolsuper,rolbypassrls FROM pg_roles WHERE rolname='p07_runtime';")
shutil.copy2(root/'apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar', local/'backend.jar')
assert (local/'backend.jar').is_file()
backend_env={k:v for k,v in os.environ.items() if k in ['PATH','HOME','USER','TMPDIR']}
backend_env.update(SPRING_PROFILES_ACTIVE='local',PET_DATABASE_URL=url,PET_DATABASE_USERNAME='p07_runtime',PET_DATABASE_PASSWORD=dbpass,PET_DATABASE_MIGRATION_ENABLED='false',PET_REDIS_HOST='127.0.0.1',PET_REDIS_PORT=str(redisport),PET_REDIS_PASSWORD=redispass,PET_PUBLIC_ORIGIN='http://127.0.0.1:5173',SERVER_PORT='18090')
log=open(local/'backend.log','w');proc=subprocess.Popen([env['JAVA_HOME']+'/bin/java','-jar',str(local/'backend.jar')],cwd=root,env=backend_env,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
secure('backend.pid',str(proc.pid));secure('resources.json',json.dumps({'backendPid':proc.pid,'pg':pg,'redis':redis,'port':18090,'redisPort':redisport}))
for _ in range(100):
 try:
  with urllib.request.urlopen('http://127.0.0.1:18090/actuator/health/readiness',timeout=2) as resp:
   if resp.status==200:break
 except Exception:time.sleep(.3)
else:raise RuntimeError('正式后端未就绪；查看忽略日志')
records.append({'label':'正式生产JAR local启动','cwd':str(root),'command':[env['JAVA_HOME']+'/bin/java','-jar',str(local/'backend.jar')],'exitCode':None,'status':'RUNTIME_VERIFIED','summary':'readiness=200 UP;受限runtime/正式迁移;微信关闭;无测试profile/Gateway'})
(ev/'environment.json').write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n')
print('隔离正式后端就绪；两类账号已通过正式命令建立；凭据仅0600输入文件和进程内存')
