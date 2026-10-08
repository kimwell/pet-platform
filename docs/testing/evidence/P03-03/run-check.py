import os, sys, subprocess, json, datetime
from pathlib import Path
root=Path(__file__).resolve().parents[4]
evidence=Path(__file__).resolve().parent
name,cwd,*argv=sys.argv[1:]
env=os.environ.copy()
env['JAVA_HOME']=os.environ.get('P03_JAVA_HOME','/private/var/folders/5y/tt1690cx7k1gcq12252yf5rh0000gn/T/pet-platform-p02-02-_qb1x3n7/tools/jdk/jdk-21.0.12.1+1/Contents/Home')
env['PATH']='/private/var/folders/5y/tt1690cx7k1gcq12252yf5rh0000gn/T/pet-platform-p02-02-_qb1x3n7/bin:/private/var/folders/5y/tt1690cx7k1gcq12252yf5rh0000gn/T/enterprise-scaffold-p01-01-plwt53cm/tools/node-v24.21.0-darwin-arm64/bin:'+env['JAVA_HOME']+'/bin:'+env['PATH']
start=datetime.datetime.now(datetime.timezone.utc).isoformat()
with (evidence/(name+'.log')).open('w') as f:
 result=subprocess.run(argv,cwd=root/cwd,env=env,stdout=f,stderr=subprocess.STDOUT)
output=(evidence/(name+'.log')).read_text()
record={'cwd':str(root/cwd),'argv':argv,'exitCode':result.returncode,'startedAtUTC':start,'finishedAtUTC':datetime.datetime.now(datetime.timezone.utc).isoformat(),'toolchain':{'JAVA_HOME':env['JAVA_HOME']},'log':name+'.log','summary':output[-4500:]}
(evidence/(name+'.json')).write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(record,ensure_ascii=False,indent=2))
sys.exit(result.returncode)
