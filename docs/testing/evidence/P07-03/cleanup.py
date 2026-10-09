from pathlib import Path
import subprocess,os,signal,json,time,socket,hashlib,shutil,datetime
root=Path.cwd();ev=root/'docs/testing/evidence/P07-03';local=root/'.local-data/p07-03';raw='unix:///Users/kimwell/Library/Containers/com.docker.docker/Data/docker.raw.sock';resources=json.loads((local/'resources.json').read_text());records=[]
def run(label,args,input=None):
 p=subprocess.run(args,cwd=root,input=input,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=15);records.append({'label':label,'cwd':str(root),'command':args,'exitCode':p.returncode,'summary':p.stdout[-2000:]})
 if p.returncode:raise RuntimeError(label)
 return p.stdout.strip()
docker=['docker','-H',raw]
q="""UPDATE identity_employee SET status=CASE WHEN right(login_name,2)::int%4=0 THEN 'DISABLED' ELSE 'ACTIVE' END WHERE tenant_id=(SELECT id FROM platform_tenant WHERE code='p07-03-acceptance') AND login_name ~ '^employee-[0-9]{2}$';
SELECT json_build_object('listGrantTenant',count(*) FILTER (WHERE permission_code='identity:user:list' AND scope_type='TENANT'),'detailGrantTenant',count(*) FILTER (WHERE permission_code='identity:user:detail' AND scope_type='TENANT'),'otherReadScopes',count(*) FILTER (WHERE permission_code IN ('identity:user:list','identity:user:detail') AND scope_type<>'TENANT')) FROM identity_role_permission WHERE role_id=(SELECT r.id FROM identity_role r JOIN platform_tenant t ON t.id=r.tenant_id WHERE t.code='p07-03-acceptance' AND r.code='tenant-admin');
SELECT count(*) FROM identity_employee_store s JOIN identity_employee e ON e.id=s.employee_id WHERE e.tenant_id=(SELECT id FROM platform_tenant WHERE code='p07-03-acceptance') AND e.login_name='p07-staff';
SELECT count(*) FROM identity_employee WHERE tenant_id=(SELECT id FROM platform_tenant WHERE code='p07-03-acceptance') AND status='DISABLED';"""
restored=run('仅本轮授权/关联/数据准备状态恢复核对',docker+['exec','-i',resources['pg'],'psql','-X','-qAt','-v','ON_ERROR_STOP=1','-U','postgres','-d','p07_03_acceptance'],q).splitlines()
assert json.loads(restored[0])=={'listGrantTenant':1,'detailGrantTenant':1,'otherReadScopes':0} and restored[1:]==['1','13']
jar=local/'backend.jar';sha=hashlib.sha256(jar.read_bytes()).hexdigest();target=root/'apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar';shutil.copy2(jar,target);assert hashlib.sha256(target.read_bytes()).hexdigest()==sha
# 只扫描本轮已知临时凭据的精确值；不加载全局环境/其他项目配置。
secrets=[]
for p in local.iterdir():
 if p.is_file() and 'password' in p.name:secrets.append(p.read_text())
secrets+=list(json.loads((local/'validation-secret.json').read_text()).values());secrets.append((local/'postgres.env').read_text().split('POSTGRES_PASSWORD=')[1].strip())
files=set(subprocess.check_output(['git','diff','--name-only'],text=True).splitlines()+subprocess.check_output(['git','ls-files','--others','--exclude-standard'],text=True).splitlines())
hits=[]
for f in files:
 p=root/f
 if p.is_file():
  value=p.read_bytes()
  if any(s and s.encode() in value for s in secrets):hits.append(f)
assert not hits
(ev/'secret-value-scan.json').write_text(json.dumps({'cwd':str(root),'status':'PASS','method':'本轮已知临时凭据精确值扫描新增/修改文件；不输出值；截图另目视核对','filesChecked':len(files),'matchingFiles':hits},ensure_ascii=False,indent=2))
for name,pid in [('后端',resources['backendPid']),('Web',int((local/'web.pid').read_text())),('字节隧道',resources['tunnelPid'])]:
 check=subprocess.run(['ps','-p',str(pid),'-o','args='],text=True,capture_output=True)
 if check.returncode==1:continue
 args=check.stdout
 assert ('p07-03/backend.jar' in args if name=='后端' else '--filter @pet/admin-web dev --port 5175' in args if name=='Web' else 'P07-03/tunnel.py' in args)
 assert os.getpgid(pid)==pid
 os.killpg(pid,signal.SIGTERM);records.append({'label':'停止本轮'+name,'cwd':str(root),'command':['kill','-TERM','process-group',pid],'exitCode':0})
# 该名字来自本轮契约测试日志中的唯一session；不处理其他Testcontainers资源。
ryuk='testcontainers-ryuk-ce4215fe-2b71-42d3-a0e7-04b605452c90'
names=run('收尾前容器名',docker+['ps','-a','--format','{{.Names}}']).splitlines()
if ryuk in names:
 labels=json.loads(run('验证本轮Ryuk标签',docker+['inspect','--format','{{json .Config.Labels}}',ryuk]))
 assert labels.get('org.testcontainers')=='true' and labels.get('org.testcontainers.sessionId')=='ce4215fe-2b71-42d3-a0e7-04b605452c90'
 run('停止本轮契约辅助容器',docker+['stop',ryuk])
run('停止本轮专用tmpfs容器',docker+['stop',resources['redis'],resources['pg']])
removed=[]
for p in local.iterdir():
 if p.is_file() and ('password' in p.name or p.name in ['postgres.env','redis.conf','validation-secret.json']):p.unlink();removed.append(p.name)
for _ in range(40):
 closed={}
 for port in [18092,5175,18094,18095]:
  s=socket.socket();s.settimeout(.2);closed[str(port)]=s.connect_ex(('127.0.0.1',port))!=0;s.close()
 if all(closed.values()):break
 time.sleep(.1)
assert all(closed.values())
containers=run('既有容器状态与本轮清理核对',docker+['ps','-a','--format','{{.Names}}\t{{.State}}']).splitlines();env=json.loads((ev/'environment-first.json').read_text());original=next(x['summary'] for x in env if x['label']=='已有容器保全基线').splitlines();assert set(original).issubset(containers);assert not any(x.startswith('pet-p07-03-') or x.startswith(ryuk) for x in containers)
volumes=run('卷保全核对',docker+['volume','ls','--format','{{.Name}}']).splitlines();before=next(x['summary'] for x in env if x['label']=='已有卷保全基线').splitlines();assert set(before).issubset(volumes)
# 最初只读默认Docker客户端仍阻塞；仅结束该已知本轮命令。
probe=subprocess.run(['ps','-p','73181','-o','args='],capture_output=True,text=True)
if probe.returncode==0 and 'docker ps -a --format' in probe.stdout:os.kill(73181,signal.SIGTERM);records.append({'label':'结束本轮阻塞只读Docker客户端','command':['kill','-TERM',73181],'exitCode':0})
result={'cwd':str(root),'command':['python3','docs/testing/evidence/P07-03/cleanup.py'],'exitCode':0,'completedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'records':records,'closedPorts':closed,'existingContainersPreserved':True,'originalContainers':original,'originalVolumesPreserved':True,'namedVolumesDeleted':False,'restoredReadGrantsAndStoreAndFixtureStatus':True,'restoredOriginalArtifactSHA256':sha,'removedSecretInputs':removed,'remainingSecretInputs':[],'summary':'只清本轮进程/字节隧道/tmpfs容器/唯一Ryuk和临时凭据；既有运行服务、容器、卷保全；标准Docker故障未擅自重启'}
(ev/'cleanup.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k not in ('records','originalContainers')},ensure_ascii=False))
