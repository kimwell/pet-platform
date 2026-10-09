import subprocess,pathlib,sys,json,datetime,os,time
label,cwd,*args=sys.argv[1:]
root=pathlib.Path('/Users/kimwell/work/pet-platform/docs/testing/evidence/B02')
env=os.environ.copy()
jdk='/Users/kimwell/.vscode/extensions/redhat.java-1.56.0-darwin-arm64/jre/21.0.12.1-macosx-aarch64'
node='/var/folders/5y/tt1690cx7k1gcq12252yf5rh0000gn/T/enterprise-scaffold-p01-01-plwt53cm/tools/node-v24.21.0-darwin-arm64/bin'
env['JAVA_HOME']=jdk;env['PATH']=jdk+'/bin:'+node+':/Users/kimwell/work/pet-platform/.local-data/p07-02/tools/bin:'+env.get('PATH','')
env['DOCKER_HOST']='unix:///Users/kimwell/.docker/run/docker.sock';env['TESTCONTAINERS_HOST_OVERRIDE']='127.0.0.1'
start=time.time()
with (root/(label+'.log')).open('w') as f:r=subprocess.run(args,cwd=cwd,env=env,stdout=f,stderr=subprocess.STDOUT)
item={'label':label,'cwd':cwd,'command':args,'exitCode':r.returncode,'durationSeconds':round(time.time()-start,2),'log':label+'.log','startedAt':datetime.datetime.fromtimestamp(start,datetime.timezone.utc).isoformat()}
with (root/'commands.jsonl').open('a') as f:f.write(json.dumps(item,ensure_ascii=False)+'\n')
print(json.dumps(item,ensure_ascii=False));print('\n'.join((root/(label+'.log')).read_text().splitlines()[-25:]));sys.exit(r.returncode)
