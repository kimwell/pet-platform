from pathlib import Path
import subprocess,os,json
root=Path.cwd();e=root/'docs/testing/evidence/P02-01';env=os.environ.copy();env.pop('PET_ENVIRONMENT',None);env.pop('PET_PUBLIC_ORIGIN',None);env.pop('SPRING_PROFILES_ACTIVE',None)
checks=[('backend-missing-environment',[str(Path(env['JAVA_HOME'])/'bin/java'),'-jar','target/pet-platform-backend-0.0.0-SNAPSHOT.jar','--server.port=18081'],root/'apps/backend'),('compose-empty-password',['docker','compose','--env-file','infra/local/.env.example','-f','infra/local/docker-compose.yml','config','--quiet'],root)]
for label,cmd,cwd in checks:
 p=subprocess.run(cmd,cwd=cwd,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=30);(e/(label+'.log')).write_text(p.stdout);record={'id':label,'cwd':str(cwd),'command':cmd,'exit_code':p.returncode,'expected_exit_code':1,'status':'PASS' if p.returncode==1 else 'FAIL','evidence':label+'.log'};(e/(label+'.json')).write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n');print(json.dumps(record));assert p.returncode==1
