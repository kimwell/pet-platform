from pathlib import Path
import json,subprocess,os,signal,time,socket,hashlib,datetime
root=Path.cwd();local=root/'.local-data/p07-02';ev=root/'docs/testing/evidence/P07-02';resources=json.loads((local/'resources.json').read_text());records=[]
def run(label,args,input=None):
 p=subprocess.run(args,cwd=root,input=input,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 records.append({'label':label,'cwd':str(root),'command':args,'exitCode':p.returncode,'summary':p.stdout.strip()})
 if p.returncode:raise RuntimeError(label)
 return p.stdout.strip()
q="""SELECT json_build_object('listGrantTenant',count(*) FILTER (WHERE p.permission_code='identity:user:list' AND p.scope_type='TENANT'),'otherListScopes',count(*) FILTER (WHERE p.permission_code='identity:user:list' AND p.scope_type<>'TENANT')) FROM identity_role_permission p JOIN identity_role r ON r.id=p.role_id JOIN platform_tenant t ON t.id=r.tenant_id WHERE t.code='p07-02-acceptance' AND r.code='tenant-admin';
SELECT count(*) FROM identity_employee_store s JOIN identity_employee e ON e.id=s.employee_id JOIN platform_tenant t ON t.id=e.tenant_id WHERE t.code='p07-02-acceptance' AND e.login_name='p07-staff';
SELECT count(*) FROM information_schema.column_privileges WHERE table_name='identity_employee' AND grantee='pet_runtime' AND privilege_type='SELECT' AND column_name IN ('id','login_name','display_name','status','created_at','updated_at');"""
restore=run('隔离授权与关联恢复核对',['docker','exec','-i',resources['pg'],'psql','-X','-qAt','-v','ON_ERROR_STOP=1','-U','postgres','-d','p07_02_acceptance'],q)
parts=restore.splitlines();assert json.loads(parts[0])=={'listGrantTenant':1,'otherListScopes':0} and parts[1:]==['1','6']
jar=local/'backend.jar';artifact=root/'apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar';jarhash=hashlib.sha256(jar.read_bytes()).hexdigest();assert jarhash==hashlib.sha256(artifact.read_bytes()).hexdigest()
pgPort=int(run('本轮PostgreSQL端口',['docker','port',resources['pg'],'5432']).split(':')[-1])
volumesBefore=run('收尾前卷清单',['docker','volume','ls','--format','{{.Name}}']).splitlines()
for label,pid in [('专用后端',resources['backendPid']),('专用Web',int((local/'web.pid').read_text()))]:
 check=subprocess.run(['ps','-p',str(pid),'-o','pid=,pgid=,comm='],text=True,capture_output=True)
 if check.returncode==1 and not check.stdout.strip():
  records.append({'label':label+'进程已不存在','cwd':str(root),'command':['ps','-p',str(pid),'-o','pid=,pgid=,comm='],'exitCode':1,'expectedExitCode':1,'status':'RUNTIME_VERIFIED','summary':'专用PID不存在，不再发送信号；对应端口关闭另行核对'})
  continue
 assert check.returncode==0
 group=os.getpgid(pid);assert group==pid
 command=subprocess.check_output(['ps','-p',str(pid),'-o','args='],text=True).strip()
 assert ('backend.jar' in command if label=='专用后端' else 'pnpm' in command and 'dev:web' in command)
 os.killpg(group,signal.SIGTERM)
 for _ in range(50):
  probe=subprocess.run(['ps','-p',str(pid),'-o','pid='],text=True,capture_output=True)
  if probe.returncode==1 and not probe.stdout.strip():break
  time.sleep(.1)
 else:
  assert os.getpgid(pid)==group
  os.killpg(group,signal.SIGKILL)
 records.append({'label':label,'cwd':str(root),'command':['kill','-TERM','process-group',str(group)],'exitCode':0,'summary':'验证专用PID/独立进程组后停止；未读取/输出进程环境'})
run('停止并移除本轮tmpfs容器',['docker','stop',resources['redis'],resources['pg']])
removed=[]
for p in local.iterdir():
 if p.is_file() and ('password' in p.name or p.name in ('postgres.env','redis.conf')):p.unlink();removed.append(p.name)
ports=[5173,18090,resources['redisPort'],pgPort]
closed={}
for port in ports:
 s=socket.socket();s.settimeout(.25);closed[str(port)]=s.connect_ex(('127.0.0.1',port))!=0;s.close()
assert all(closed.values())
containers=run('已有容器状态核对',['docker','ps','-a','--format','{{.Names}}\t{{.State}}']).splitlines()
expected=['pet-platform-p02-validation-rabbitmq-1\texited','pet-platform-p02-validation-postgres-1\texited','pet-platform-p02-validation-redis-1\texited']
assert set(expected).issubset(containers) and not any(x.startswith('pet-p07-02-') for x in containers)
volumesAfter=run('收尾后卷清单',['docker','volume','ls','--format','{{.Name}}']).splitlines();assert set(volumesBefore).issubset(volumesAfter)
result={'cwd':str(root),'command':['python3','.local-data/p07-02/cleanup.py'],'exitCode':0,'completedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'records':records,'restoredGrantAndStoreAndSelect':True,'originalJarSHA256':jarhash,'artifactMatchesOriginal':True,'removedSecretInputs':removed,'remainingSecretInputs':[],'closedPorts':closed,'existingContainersPreserved':True,'volumesBefore':volumesBefore,'volumesAfter':volumesAfter,'namedVolumesDeleted':False,'summary':'只停止本轮生产JAR/Web/两个tmpfs容器并移除秘密输入；未删除既有数据库卷'}
(ev/'cleanup.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k not in ('records','volumesBefore','volumesAfter')},ensure_ascii=False))
