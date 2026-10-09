from pathlib import Path
import os,signal,time,subprocess,json,re,socket,shutil
root=Path('/Users/kimwell/work/pet-platform');local=root/'.local-data/b02';ev=root/'docs/testing/evidence/B02';res=json.loads((local/'resources.json').read_text());out={'dedicatedProcessIds':[],'containers':[],'ports':[],'existingVolumesTouched':False,'devtoolsServicePortChanged':False,'dedicatedProbeLaunched':False}
for key in ['backendPid','webPid']:
 pid=res[key]
 try:os.killpg(pid,signal.SIGTERM);status='SIGTERM_SENT'
 except ProcessLookupError:status='ALREADY_STOPPED'
 out['dedicatedProcessIds'].append({'pid':pid,'status':status})
for name in [res['pg'],res['redis']]:
 exists=subprocess.run(['docker','inspect',name],capture_output=True,text=True).returncode==0
 if exists:
  r=subprocess.run(['docker','stop',name],capture_output=True,text=True);assert r.returncode==0;out['containers'].append({'name':name,'stopExitCode':r.returncode,'status':'REMOVED'})
 else:out['containers'].append({'name':name,'stopExitCode':None,'status':'ALREADY_REMOVED_BY_FIRST_ATTEMPT'})
for port in [res['backendPort'],res['webPort'],res['pgPort'],res['redisPort'],9421,9422]:
 stopped=False
 for _ in range(80):
  with socket.socket() as s:
   s.settimeout(.1);stopped=s.connect_ex(('127.0.0.1',port))!=0
  if stopped:break
  time.sleep(.1)
 out['ports'].append({'port':port,'noListener':stopped});assert stopped
active=subprocess.check_output(['docker','ps','-a','--no-trunc','--format','{{.ID}}'],text=True).splitlines();ids=set()
for file in ev.glob('*.log'):
 for val in re.findall(r'Container [^\n]+ is starting: ([0-9a-f]{64})',file.read_text()):ids.add(val)
for file in [ev/'runtime-setup.json',ev/'runtime-setup-before-initialization-fix.json']:
 for item in json.loads(file.read_text()):
  summary=item.get('summary','').strip()
  if re.fullmatch('[0-9a-f]{64}',summary):ids.add(summary)
out['loggedContainerCount']=len(ids);out['loggedContainersStillPresent']=sorted(ids.intersection(active));assert not out['loggedContainersStillPresent']
current=subprocess.check_output(['docker','ps','-a','--format','{{.Names}}\t{{.State}}'],text=True).splitlines();before=json.loads((ev/'runtime-setup-before-initialization-fix.json').read_text())[0]['summary'].strip().splitlines();states={x.split('\t')[0]:x.split('\t')[1] for x in current if '\t' in x}
out['existingNamedInfrastructurePreserved']=all(states.get(x.split('\t')[0])==x.split('\t')[1] for x in before if '\t' in x and x.startswith('pet-platform-'));assert out['existingNamedInfrastructurePreserved']
out['baselineTemporaryContainerChanges']=[{'name':x.split('\t')[0],'before':x.split('\t')[1],'after':states.get(x.split('\t')[0]),'directCleanupCommand':False} for x in before if '\t' in x and states.get(x.split('\t')[0])!=x.split('\t')[1]]
out['baselineChangeNote']='初始快照处于Maven容器运行期间；4个临时容器后来消失。仅本轮两个命名容器执行stop，不把全快照不变写PASS。' 
# 只删除本轮忽略目录内的凭据、状态、探针及工具依赖；复核源已脱敏保存。
privateCount=len([p for p in local.rglob('*') if p.is_file()]);shutil.rmtree(local);out['privateTaskDirectoryRemoved']=not local.exists();out['removedPrivateFiles']=privateCount;out['status']='PASS';(ev/'cleanup.json').write_text(json.dumps(out,ensure_ascii=False,indent=2));print({'status':'PASS','portsClosed':len(out['ports']),'loggedContainersRemoved':len(ids),'privateTaskDirectoryRemoved':True})
