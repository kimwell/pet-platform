import os,sys,json,pathlib,subprocess,datetime
root=pathlib.Path('/Users/kimwell/work/pet-platform'); e=root/'docs/testing/evidence/P05-04';name=sys.argv[1];cwd=root/sys.argv[2];args=sys.argv[3:];env=os.environ.copy()
env['PATH']='/private/var/folders/5y/tt1690cx7k1gcq12252yf5rh0000gn/T/enterprise-scaffold-p01-01-plwt53cm/tools/node-v24.21.0-darwin-arm64/bin:/private/var/folders/5y/tt1690cx7k1gcq12252yf5rh0000gn/T/pet-platform-p02-02-_qb1x3n7/bin:'+env['PATH']
env['JAVA_HOME']=json.loads((root/'docs/testing/evidence/P05-01/toolchain.json').read_text())['JAVA_HOME']
start=datetime.datetime.now(datetime.timezone.utc).isoformat()
with (e/(name+'.log')).open('w') as f:r=subprocess.run(args,cwd=cwd,env=env,stdout=f,stderr=subprocess.STDOUT)
log=(e/(name+'.log')).read_text();meta=dict(cwd=str(cwd),argv=args,exitCode=r.returncode,startedAtUTC=start,finishedAtUTC=datetime.datetime.now(datetime.timezone.utc).isoformat(),log=name+'.log',summary=log[-3500:]);(e/(name+'.json')).write_text(json.dumps(meta,ensure_ascii=False,indent=2));print(json.dumps(meta,ensure_ascii=False))
if name=='final-audit-command':
 commands=[]
 for path in sorted(e.glob('*.json')):
  data=json.loads(path.read_text())
  if isinstance(data,dict) and 'exitCode' in data and ('argv' in data or 'command' in data):
   commands.append({'file':path.name,**{k:data[k] for k in ('cwd','argv','command','exitCode','startedAtUTC','finishedAtUTC','log') if k in data}})
 (e/'COMMAND-INDEX.json').write_text(json.dumps(commands,ensure_ascii=False,indent=2))
sys.exit(r.returncode)
