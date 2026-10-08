import os,json,subprocess,datetime,pathlib,sys
root=pathlib.Path('/Users/kimwell/work/pet-platform');e=root/'docs/testing/evidence/P05-02';name=sys.argv[1];cwd=root/sys.argv[2];args=sys.argv[3:];env=os.environ.copy();env['PATH']='/private/var/folders/5y/tt1690cx7k1gcq12252yf5rh0000gn/T/enterprise-scaffold-p01-01-plwt53cm/tools/node-v24.21.0-darwin-arm64/bin:/private/var/folders/5y/tt1690cx7k1gcq12252yf5rh0000gn/T/pet-platform-p02-02-_qb1x3n7/bin:'+env['PATH'];env['JAVA_HOME']=json.loads((root/'docs/testing/evidence/P05-01/toolchain.json').read_text())['JAVA_HOME']
start=datetime.datetime.now(datetime.timezone.utc).isoformat()
with (e/(name+'.log')).open('w') as log:p=subprocess.run(args,cwd=cwd,env=env,stdout=log,stderr=subprocess.STDOUT)
(e/(name+'.json')).write_text(json.dumps({'cwd':str(cwd),'argv':args,'exitCode':p.returncode,'startedAtUTC':start,'finishedAtUTC':datetime.datetime.now(datetime.timezone.utc).isoformat(),'log':name+'.log'},indent=2))
print(name,'exitCode',p.returncode)
print('\n'.join((e/(name+'.log')).read_text().splitlines()[-32:]))
sys.exit(p.returncode)
