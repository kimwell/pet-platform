import os,subprocess,json,datetime,sys
from pathlib import Path
root=Path(__file__).resolve().parents[4]
evidence=Path(__file__).resolve().parent
label=sys.argv[1];args=sys.argv[2:]
env=os.environ.copy()
java_home=json.loads((root/'docs/testing/evidence/P04-03/toolchain.json').read_text())['JAVA_HOME']
if not Path(java_home,'bin/java').exists(): raise SystemExit('冻结JDK路径不可用')
env['JAVA_HOME']=java_home;env['PATH']=java_home+'/bin:'+env['PATH']
started=datetime.datetime.now(datetime.timezone.utc).isoformat()
with (evidence/(label+'.log')).open('w') as output:
 result=subprocess.run(args,cwd=root/'apps/backend',env=env,stdout=output,stderr=subprocess.STDOUT)
log=(evidence/(label+'.log')).read_text()
record={'cwd':str(root/'apps/backend'),'argv':args,'JAVA_HOME':java_home,'exitCode':result.returncode,'startedAtUTC':started,'finishedAtUTC':datetime.datetime.now(datetime.timezone.utc).isoformat(),'log':label+'.log','summary':log[-3500:]}
(evidence/(label+'.json')).write_text(json.dumps(record,ensure_ascii=False,indent=2))
print(json.dumps(record,ensure_ascii=False,indent=2))
raise SystemExit(result.returncode)
