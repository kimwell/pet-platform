"""配置反例只使用临时进程变量与公开输入，不写用户本地配置。"""
from runner import COMPOSE, ROOT, run

jar=str(ROOT/'apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar')
cases=[
    ('backend-missing-profile-r2',[],['pet.environment']),
    ('backend-prod-missing-origin',['--spring.profiles.active=prod'],['pet.public-origin']),
    ('backend-prod-http',['--spring.profiles.active=prod','--pet.public-origin=http://localhost'],['必须使用 HTTPS']),
    ('backend-invalid-origin',['--spring.profiles.active=local','--pet.public-origin=http://localhost:65536'],['端口须为']),
    ('backend-invalid-port',['--spring.profiles.active=local','--server.port=not-a-port'],['server.port']),
    ('backend-port-occupied',['--spring.profiles.active=local'],['8080','already in use'])]
for name,options,terms in cases:
    result=run(name,['java','-jar',jar]+options,expected=1)
    assert result.returncode == 1 and all(term in result.stdout+result.stderr for term in terms),name
for name,value in [('web-empty-origin',''),('web-invalid-origin','backend'),('web-credential-origin','http://user:technical-fixture@localhost')]:
    result=run(name,['pnpm','dev:web'],expected=1,overrides={'DEV_BACKEND_ORIGIN':value})
    assert 'DEV_BACKEND_ORIGIN 必须' in result.stdout+result.stderr
for name,args,term in [
    ('web-port-occupied',['pnpm','dev:web'],'Port 5173 is already in use'),
    ('preview-port-occupied',['pnpm','preview:web'],'Port 4173 is already in use'),
    ('infra-no-env-file',['pnpm','check:infra'],'缺少本地基础设施环境文件')]:
    result=run(name,args,expected=1)
    assert term in result.stdout+result.stderr
for key in ['POSTGRES_DB','POSTGRES_USER','POSTGRES_PASSWORD','REDIS_PASSWORD','RABBITMQ_DEFAULT_USER','RABBITMQ_DEFAULT_PASS']:
    result=run('infra-missing-'+key.lower(),COMPOSE+['config','--quiet'],expected=1,overrides={key:''})
    assert key in result.stdout+result.stderr
for key in ['POSTGRES_PORT','REDIS_PORT','RABBITMQ_PORT','RABBITMQ_MANAGEMENT_PORT']:
    result=run('infra-invalid-'+key.lower(),COMPOSE+['config','--quiet'],expected=15,overrides={key:'not-a-port'})
    assert result.returncode != 0 and 'not-a-port' in result.stdout+result.stderr
