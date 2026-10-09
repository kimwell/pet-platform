import subprocess, pathlib, sys, json, datetime, os
name,cwd,*args=sys.argv[1:]
root=pathlib.Path('/Users/kimwell/work/pet-platform/docs/testing/evidence/P07-03')
start=datetime.datetime.now(datetime.timezone.utc).isoformat()
env=os.environ.copy()
task_jdk='/Users/kimwell/.vscode/extensions/redhat.java-1.56.0-darwin-arm64/jre/21.0.12.1-macosx-aarch64'
env['JAVA_HOME']=task_jdk
if name.startswith('contracts-raw'):
 env['DOCKER_HOST']='unix:///Users/kimwell/Library/Containers/com.docker.docker/Data/docker.raw.sock'
 if 'socket' in name: env['TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE']='/var/run/docker.sock'
task_node='/var/folders/5y/tt1690cx7k1gcq12252yf5rh0000gn/T/enterprise-scaffold-p01-01-plwt53cm/tools/node-v24.21.0-darwin-arm64/bin'
task_pnpm='/Users/kimwell/work/pet-platform/.local-data/p07-03/tools/bin'
env['PATH']=':'.join((task_jdk+'/bin',task_pnpm,task_node,env.get('PATH','')))
with (root/(name+'.log')).open('w') as log:
 result=subprocess.run(args,cwd=cwd,stdout=log,stderr=subprocess.STDOUT,env=env)
metadata={'cwd':cwd,'command':args,'toolchain':{'JAVA_HOME':task_jdk,'nodeBin':task_node,'pnpmBin':task_pnpm},'exitCode':result.returncode,'startedAt':start,'completedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'log':name+'.log'}
(root/(name+'.json')).write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(metadata,ensure_ascii=False))
print('\n'.join((root/(name+'.log')).read_text().splitlines()[-25:]))
sys.exit(result.returncode)
