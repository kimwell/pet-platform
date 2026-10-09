from pathlib import Path
import os,json,subprocess,signal,time,shutil,hashlib,urllib.request
root=Path.cwd();local=root/'.local-data/architecture/runtime';ev=root/'docs/testing/evidence/ARCHITECTURE-REFRESH'
r=json.loads((local/'resources.json').read_text());pid=r['backendPid']
observed=subprocess.check_output(['ps','-p',str(pid),'-o','command='],text=True)
if str(local/'backend.jar') not in observed:raise RuntimeError('进程归属不一致')
os.killpg(pid,signal.SIGTERM)
for _ in range(50):
 try:os.kill(pid,0)
 except ProcessLookupError:break
 time.sleep(.1)
source=root/'apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar';shutil.copy2(source,local/'backend.jar')
c=json.loads((local/'command-env.json').read_text());password=(local/'redis.conf').read_text().split('requirepass ')[1].splitlines()[0]
env={k:v for k,v in os.environ.items() if k in ['PATH','HOME','USER','TMPDIR']};env.update(SPRING_PROFILES_ACTIVE='local',SPRING_CONFIG_ADDITIONAL_LOCATION='file:'+str(local/'wechat.yaml'),PET_DATABASE_URL=c['PET_MIGRATION_DATABASE_URL'],PET_DATABASE_USERNAME='arch_runtime',PET_DATABASE_PASSWORD=c['PET_MIGRATION_DATABASE_PASSWORD'],PET_DATABASE_MIGRATION_ENABLED='false',PET_REDIS_HOST='127.0.0.1',PET_REDIS_PORT=str(r['redisPort']),PET_REDIS_PASSWORD=password,PET_PUBLIC_ORIGIN='http://127.0.0.1:18102',SERVER_PORT='18101')
jdk='/Users/kimwell/.vscode/extensions/redhat.java-1.56.0-darwin-arm64/jre/21.0.12.1-macosx-aarch64';command=[jdk+'/bin/java','-jar',str(local/'backend.jar')]
p=subprocess.Popen(command,cwd=root,env=env,stdout=open(local/'backend-final.log','w'),stderr=subprocess.STDOUT,start_new_session=True);r['backendPid']=p.pid;(local/'resources.json').write_text(json.dumps(r));(local/'resources.json').chmod(0o600)
for _ in range(100):
 try:
  if urllib.request.urlopen('http://127.0.0.1:18101/actuator/health/readiness',timeout=2).status==200:break
 except Exception:time.sleep(.3)
else:raise RuntimeError('最终JAR未就绪')
(ev/'final-runtime-artifact.json').write_text(json.dumps({'cwd':str(root),'command':command,'previousPid':pid,'pid':p.pid,'status':'RUNTIME_VERIFIED','readiness':200,'jarSha256':hashlib.sha256(source.read_bytes()).hexdigest(),'metadata':'最终Tag注解与兼容契约；正式local配置和原PG/Redis，会话保留'},ensure_ascii=False,indent=2));print('最终生产JAR就绪200，凭据未输出')
