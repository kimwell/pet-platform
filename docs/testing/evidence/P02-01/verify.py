import os,sys,json,subprocess,time,shlex,pathlib
root=pathlib.Path('/Users/kimwell/work/pet-platform')
evidence=root/'docs/testing/evidence/P02-01'
probe=pathlib.Path('/var/folders/5y/tt1690cx7k1gcq12252yf5rh0000gn/T/enterprise-scaffold-p01-01-plwt53cm')
env=os.environ.copy();env['JAVA_HOME']=str(probe/'tools/jdk-21.0.12.1+1/Contents/Home');env['PATH']=str(pathlib.Path(__file__).parent/'bin')+':'+str(probe/'tools/node-v24.21.0-darwin-arm64/bin')+':'+env['JAVA_HOME']+'/bin:'+env['PATH']
label,cwd,*command=sys.argv[1:]
if command[0]=='pnpm':command=[str(probe/'tools/node-v24.21.0-darwin-arm64/bin/node'),str(probe/'tools/pnpm/package/bin/pnpm.cjs'),*command[1:]]
start=time.time()
p=subprocess.run(command,cwd=root/cwd,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
output=p.stdout
for key in ['POSTGRES_PASSWORD','REDIS_PASSWORD','RABBITMQ_DEFAULT_PASS']:
 if env.get(key): output=output.replace(env[key],'<REDACTED>')
(evidence/(label+'.log')).write_text(output)
record={'id':label,'cwd':str(root/cwd),'command':command,'exit_code':p.returncode,'status':'PASS' if p.returncode==0 else 'FAIL','duration_seconds':round(time.time()-start,2),'evidence':label+'.log','summary':output[-1600:]}
(evidence/(label+'.json')).write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(record,ensure_ascii=False))
sys.exit(p.returncode)
