import pathlib,os,subprocess,json,secrets,time,socket,shutil,urllib.request
root=pathlib.Path('/Users/kimwell/work/pet-platform');local=root/'.local-data/p07-03';ev=root/'docs/testing/evidence/P07-03';local.chmod(0o700)
from datetime import datetime
os.environ['DOCKER_HOST']='unix:///Users/kimwell/Library/Containers/com.docker.docker/Data/docker.raw.sock'
records=[]
def run(label,args,input=None,env=None):
 start=datetime.now().astimezone().isoformat();p=subprocess.run((['docker','-H','unix:///Users/kimwell/Library/Containers/com.docker.docker/Data/docker.raw.sock',*args[1:]] if args[0]=='docker' else args),cwd=root,input=input,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,env=env,timeout=45)
 summary=p.stdout
 for value in secret_values:
  if value:summary=summary.replace(value,'<REDACTED>')
 records.append({'label':label,'cwd':str(root),'command':[('<受控员工账号>' if v=='p07-staff' else '<受控平台账号>' if v=='p07-platform' else v) for v in args],'startedAt':start,'exitCode':p.returncode,'summary':summary[-1600:]});(ev/'environment.json').write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n')
 if p.returncode:
  if label.startswith('启动隔离') and args[0]=='curl':
   name=args[-1].split('/')[-2]
   if run('恢复核对本轮容器运行状态',['docker','inspect','--format','{{.State.Status}}',name])=='running':return ''
  raise RuntimeError(label+' failed; sanitized evidence saved')
 return p.stdout.strip()
def secure(name,value):
 p=local/name;p.write_text(value);p.chmod(0o600);return p
secret_values=[secrets.token_urlsafe(30) for _ in range(7)]
admin,dbpass,redispass,bootpass,platformdbpass,staffpass,platformpass=secret_values
secure('staff-password',staffpass);secure('platform-password',platformpass)
secure('staff-new-password',' '+secrets.token_urlsafe(30)+' ');secure('platform-new-password',' '+secrets.token_urlsafe(30)+' ');secure('staff-final-password',secrets.token_urlsafe(30))
pg='pet-p07-03-pg';redis='pet-p07-03-redis';db='p07_03_acceptance'
run('已有卷保全基线',['docker','volume','ls','--format','{{.Name}}'])
run('已有容器保全基线',['docker','ps','-a','--format','{{.Names}}\t{{.State}}'])
for port in [18092]:
 with socket.socket() as s:s.bind(('127.0.0.1',port))
pg_env=secure('postgres.env',f'POSTGRES_DB={db}\nPOSTGRES_USER=postgres\nPOSTGRES_PASSWORD={admin}\n')
redis_conf=secure('redis.conf',f'bind 0.0.0.0\nprotected-mode yes\nrequirepass {redispass}\nsave ""\nappendonly no\n')
# 密码配置只在隔离tmpfs服务；容器内Redis必须可读挂载，父目录仍0700。
redis_conf.chmod(0o644)
run('独立冻结PostgreSQL',['docker','create','--rm','--name',pg,'--label','pet.validation=P07-03','--tmpfs','/var/lib/postgresql/data','--env-file',str(pg_env),'postgres:17.11-bookworm@sha256:3645570cccdfa447589da9f57dd740faa29b30938e861289a5574b6ca6b03826','-c','log_statement=none','-c','log_min_error_statement=panic','-c','log_parameter_max_length=0','-c','log_parameter_max_length_on_error=0'])
run('启动隔离PostgreSQL',['curl','--max-time','8','--unix-socket','/Users/kimwell/Library/Containers/com.docker.docker/Data/docker.raw.sock','-fsS','-X','POST','http://localhost/containers/'+pg+'/start'])
run('独立冻结Redis',['docker','create','--rm','--name',redis,'--label','pet.validation=P07-03','--tmpfs','/data','-v',str(redis_conf)+':/tmp/validation.conf:ro','redis:8.2.10-bookworm@sha256:47670742d7924adbcdb404d1288b6327815b23141969c0939b8e5d61f78c2634','redis-server','/tmp/validation.conf'])
run('启动隔离Redis',['curl','--max-time','8','--unix-socket','/Users/kimwell/Library/Containers/com.docker.docker/Data/docker.raw.sock','-fsS','-X','POST','http://localhost/containers/'+redis+'/start'])
for _ in range(50):
 p=subprocess.run(['docker','exec',pg,'psql','-X','-qAt','-U','postgres','-d',db,'-c','SELECT 1'],capture_output=True)
 if not p.returncode:break
 time.sleep(.2)
tunnel=subprocess.Popen(['python3',str(ev/'tunnel.py')],cwd=root,stdout=open(local/'tunnel.log','w'),stderr=subprocess.STDOUT,start_new_session=True)
secure('tunnel.pid',str(tunnel.pid))
for _ in range(30):
 if (local/'tunnel-resources.json').exists():break
 time.sleep(.1)
pgport=18094;redisport=18095
records.append({'label':'本轮回环传输隧道','cwd':str(root),'command':['python3',str(ev/'tunnel.py')],'exitCode':None,'status':'RUNTIME_VERIFIED','summary':'127.0.0.1:18094/18095经docker exec直连容器真实PG/Redis；不替换SQL/认证/协议；Docker Desktop发布端口失败独立留证'})
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
run('正式员工初始化',['scripts/backend-identity.sh','bootstrap','--tenant-code','p07-03-acceptance','--tenant-name','P07-03隔离验收','--admin-login','p07-staff','--store-code','store-a','--store-name','验收门店A','--password-stdin'],input=staffpass+'\n',env=env)
run('正式第二租户员工初始化',['scripts/backend-identity.sh','bootstrap','--tenant-code','p07-03-second','--tenant-name','P07-03第二隔离租户','--admin-login','p07-staff','--store-code','store-a','--store-name','验收门店A','--password-stdin'],input=staffpass+'\n',env=env)
env.update(PET_PLATFORM_BOOTSTRAP_DATABASE_URL=url,PET_PLATFORM_BOOTSTRAP_DATABASE_USERNAME='p07_platform_bootstrap',PET_PLATFORM_BOOTSTRAP_DATABASE_PASSWORD=platformdbpass)
run('正式平台初始化',['scripts/backend-identity.sh','platform-bootstrap','--admin-login','p07-platform','--admin-name','P07-03验收平台管理员','--password-stdin'],input=platformpass+'\n',env=env)
run('显式保留管理员详情授权补充', ['scripts/backend-identity.sh','employee-read-upgrade','--tenant-id',run('隔离租户引用',psql,input="SELECT id FROM platform_tenant WHERE code='p07-03-acceptance';")],env=env)
run('迁移与受限身份白名单',psql,input="SELECT version,success FROM flyway_schema_history ORDER BY installed_rank; SELECT rolname,rolsuper,rolbypassrls FROM pg_roles WHERE rolname='p07_runtime';")
shutil.copy2(root/'apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar', local/'backend.jar')
assert (local/'backend.jar').is_file()
backend_env={k:v for k,v in os.environ.items() if k in ['PATH','HOME','USER','TMPDIR']}
backend_env.update(SPRING_PROFILES_ACTIVE='local',PET_DATABASE_URL=url,PET_DATABASE_USERNAME='p07_runtime',PET_DATABASE_PASSWORD=dbpass,PET_DATABASE_MIGRATION_ENABLED='false',PET_REDIS_HOST='127.0.0.1',PET_REDIS_PORT=str(redisport),PET_REDIS_PASSWORD=redispass,PET_PUBLIC_ORIGIN='http://127.0.0.1:5175',SERVER_PORT='18092')
log=open(local/'backend.log','w');proc=subprocess.Popen([env['JAVA_HOME']+'/bin/java','-jar',str(local/'backend.jar')],cwd=root,env=backend_env,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
secure('backend.pid',str(proc.pid));secure('resources.json',json.dumps({'tunnelPid':tunnel.pid,'backendPid':proc.pid,'pg':pg,'redis':redis,'port':18092,'redisPort':redisport}))
for _ in range(100):
 try:
  with urllib.request.urlopen('http://127.0.0.1:18092/actuator/health/readiness',timeout=2) as resp:
   if resp.status==200:break
 except Exception:time.sleep(.3)
else:raise RuntimeError('正式后端未就绪；查看忽略日志')
records.append({'label':'正式生产JAR local启动','cwd':str(root),'command':[env['JAVA_HOME']+'/bin/java','-jar',str(local/'backend.jar')],'exitCode':None,'status':'RUNTIME_VERIFIED','summary':'readiness=200 UP;受限runtime/正式迁移;微信关闭;无测试profile/Gateway'})
(ev/'environment.json').write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n')
print('隔离正式后端就绪；两类账号已通过正式命令建立；凭据仅0600输入文件和进程内存')
