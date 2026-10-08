from pathlib import Path
import subprocess,json,urllib.request,base64
base=['docker','compose','--env-file','.local-data/p02-infra.env','-p','pet-platform-p02-validation','-f','infra/local/docker-compose.yml']
commands=[('health',base+['ps','--format','json']),('postgres-io',base+['exec','-T','postgres','sh','-ec','PGPASSWORD="$POSTGRES_PASSWORD" psql -h 127.0.0.1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -Atc "select version(); select 1;"']),('redis-auth',base+['exec','-T','redis','sh','-ec','REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli ping; redis-cli ping']),('rabbit-node',base+['exec','-T','rabbitmq','rabbitmq-diagnostics','-q','check_running'])]
for label,cmd in commands:
 r=subprocess.run(cmd,capture_output=True,text=True);print(json.dumps({'id':label,'cwd':str(Path.cwd()),'command':cmd,'exit_code':r.returncode,'output':r.stdout+r.stderr}));assert r.returncode==0
 if label=='redis-auth':assert 'PONG' in r.stdout and 'NOAUTH' in r.stdout
 if label=='health':assert r.stdout.count('"Health":"healthy"')==3
values=dict(line.split('=',1) for line in Path('.local-data/p02-infra.env').read_text().splitlines() if line and not line.startswith('#'))
req=urllib.request.Request('http://127.0.0.1:25673/api/overview');auth=values['RABBITMQ_DEFAULT_USER']+':'+values['RABBITMQ_DEFAULT_PASS'];req.add_header('Authorization','Basic '+base64.b64encode(auth.encode()).decode())
with urllib.request.urlopen(req,timeout=15) as r:data=json.load(r);assert r.status==200;print('RabbitMQ management authenticated HTTP 200, version='+data['rabbitmq_version'])
