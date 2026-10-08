"""只读取本次产物并启动该产物到随机回环端口，不操作其他服务。"""
import hashlib, json, os, re, socket, subprocess, time, urllib.request, urllib.error, zipfile
from pathlib import Path
root=Path(__file__).resolve().parents[4]
out=Path(__file__).resolve().parent
backend=root/'apps/backend'
jar=backend/'target/pet-platform-backend-0.0.0-SNAPSHOT.jar'
with zipfile.ZipFile(jar) as archive:
 names=archive.namelist()
 classes=[n for n in names if n.startswith('BOOT-INF/classes/') and n.endswith('.class')]
 libraries=[n for n in names if n.startswith('BOOT-INF/lib/')]
 assert not any('protocol/' in n or 'Test' in n or 'Fixture' in n for n in classes)
 assert not any(b'__protocol' in archive.read(n) for n in classes)
 forbidden=['hibernate-core','spring-data-jpa','flyway','postgresql','h2-','sa-token','spring-data-redis','spring-rabbit']
 assert not any(marker in n for n in libraries for marker in forbidden)
 audit={'jar':str(jar),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'productionClasses':classes,'libraries':libraries,'testFixturesAbsent':True,'databaseAndAuthenticationDependenciesAbsent':True}
 (out/'artifact-audit.json').write_text(json.dumps(audit,ensure_ascii=False,indent=2)+'\n')
java=Path(os.environ['JAVA_HOME'])/'bin/java'
args=[str(java),'-jar',str(jar),'--spring.profiles.active=local','--server.port=0']
log=out/'local-startup.log'
started=time.time()
records=[]
with log.open('w') as output:
 process=subprocess.Popen(args,cwd=backend,stdout=output,stderr=subprocess.STDOUT)
 try:
  port=None
  while time.time()-started < 30:
   if process.poll() is not None: raise AssertionError('后端提前退出')
   match=re.search(r'Tomcat started on port (\d+)',log.read_text())
   if match: port=int(match.group(1));break
   time.sleep(.1)
  assert port is not None,'未观察到启动完成'
  for path,accept in [('/', 'application/json'),('/__protocol/success','application/json'),('/error','text/html')]:
   request=urllib.request.Request(f'http://127.0.0.1:{port}'+path,headers={'Accept':accept,'X-Trace-Id':'0123456789abcdef0123456789abcdef'})
   try: response=urllib.request.urlopen(request,timeout=5)
   except urllib.error.HTTPError as error: response=error
   body=response.read().decode()
   parsed=json.loads(body)
   assert response.status==404
   assert parsed['traceId']==response.headers['X-Trace-Id']=='0123456789abcdef0123456789abcdef'
   assert parsed['error']['code']=='RESOURCE_NOT_FOUND'
   assert set(parsed)=={'success','error','traceId'}
   assert 'application/json' in response.headers['Content-Type']
   records.append({'path':path,'accept':accept,'status':response.status,'headers':dict(response.headers),'body':parsed})
 finally:
  process.terminate()
  exit_code=process.wait(timeout=15)
 closed=socket.socket()
 try: assert closed.connect_ex(('127.0.0.1',port))!=0
 finally: closed.close()
record={'cwd':str(backend),'argv':args,'startupObserved':True,'profile':'local','http':records,'processExitAfterSIGTERM':exit_code,'portClosed':True,'log':log.name}
(out/'local-runtime.json').write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'artifactSHA256':audit['sha256'],'productionClasses':len(classes),'localHTTP':[(x['path'],x['status']) for x in records],'processExitAfterSIGTERM':exit_code,'portClosed':True},ensure_ascii=False))
