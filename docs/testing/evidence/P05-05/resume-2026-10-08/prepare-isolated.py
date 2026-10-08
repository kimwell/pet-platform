"""仅隔离准备：正式JAR/迁移/初始化，不获取code、不调用微信、不记录原始输出。"""
import datetime
import hashlib
import json
import os
from pathlib import Path
import secrets
import socket
import stat
import subprocess
import tempfile
import threading
import time
import urllib.error
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[5]
EVIDENCE = Path(__file__).resolve().parent
PRIVATE = ROOT / '.local-data/p05-05-wechat'
JAVA_HOME = Path('/var/folders/5y/tt1690cx7k1gcq12252yf5rh0000gn/T/pet-platform-p02-02-_qb1x3n7/tools/jdk/jdk-21.0.12.1+1/Contents/Home')
JAR = ROOT / 'apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar'
IMAGES = {
    'postgres': 'postgres:17.11-bookworm@sha256:3645570cccdfa447589da9f57dd740faa29b30938e861289a5574b6ca6b03826',
    'redis': 'redis:8.2.10-bookworm@sha256:47670742d7924adbcdb404d1288b6327815b23141969c0939b8e5d61f78c2634',
}
RESULT = {'startedAt': None, 'cwd': str(ROOT), 'commands': [], 'checks': {}, 'steps': {k: 'NOT_EXECUTED' for k in ['realCodeExchange', 'customerSession', 'currentIdentity', 'logoutInvalidation', 'repeatCustomerReuse']}, 'reason': '用户确认暂时无法提供AppSecret；本轮只完成安全准备。'}
CONTAINERS = []
BACKEND = None


def now():
    return datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat()


def run(label, argv, *, data=None, env=None, allowed_output=False, check=True):
    start = now()
    completed = subprocess.run(argv, cwd=ROOT, input=data, env=env, text=True, capture_output=True, timeout=120)
    item = {'label': label, 'cwd': str(ROOT), 'argv': [str(x) for x in argv], 'startedAt': start, 'finishedAt': now(), 'exitCode': completed.returncode, 'summary': '完成' if completed.returncode == 0 else '失败；原始输出不采集'}
    if allowed_output:
        item['summary'] = completed.stdout.strip()
    RESULT['commands'].append(item)
    if check and completed.returncode:
        raise RuntimeError(label)
    return completed


def sql(name, query, *, role='postgres', record=True):
    return run(name, ['docker', 'exec', '-i', CONTAINERS[0], 'psql', '-X', '-qAt', '-v', 'ON_ERROR_STOP=1', '-U', role, '-d', 'p05_05_wechat_acceptance'], data=query, allowed_output=record).stdout.strip()


def limited_env():
    env = {k: os.environ[k] for k in ['PATH', 'LANG', 'LC_CTYPE'] if k in os.environ}
    env['JAVA_HOME'] = str(JAVA_HOME)
    return env


def http(path):
    trace = secrets.token_hex(16)
    request = urllib.request.Request('http://127.0.0.1:' + str(PORT) + path, headers={'X-Trace-Id': trace})
    try:
        response = urllib.request.urlopen(request, timeout=3)
    except urllib.error.HTTPError as failure:
        response = failure
    with response:
        body = json.loads(response.read())
        # 只采集白名单字段；不保存整个响应或请求头。
        return {'path': path, 'httpStatus': response.status, 'traceId': body.get('traceId'), 'headerTraceMatches': response.headers.get('X-Trace-Id') == trace, 'errorCode': body.get('error', {}).get('code'), 'healthStatus': body.get('status')}, body


def check_secret():
    path = PRIVATE / 'secrets/PET_WECHAT_LOCAL_SECRET'
    ignored = run('秘密位置Git忽略检查', ['git', 'check-ignore', str(path)], allowed_output=True)
    return {'property': 'PET_WECHAT_LOCAL_SECRET', 'path': str(path), 'exists': path.exists(), 'readable': os.access(path, os.R_OK), 'nonempty': path.exists() and path.stat().st_size > 0, 'mode': oct(stat.S_IMODE(path.stat().st_mode)), 'directoryMode': oct(stat.S_IMODE(path.parent.stat().st_mode)), 'gitIgnored': ignored.returncode == 0, 'contentRead': False}


def drain(stream):
    # 所有原始后端输出丢弃；仅固定事件及计数进入证据。
    for line in stream:
        if 'Started Application in ' in line:
            RESULT['checks']['applicationStartedMessageObserved'] = True
        if ' ERROR ' in line:
            RESULT['checks']['backendErrorLines'] = RESULT['checks'].get('backendErrorLines', 0) + 1


def prepare():
    global BACKEND, PORT
    RESULT['startedAt'] = now()
    RESULT['checks']['secret'] = check_secret()
    assert not RESULT['checks']['secret']['nonempty'], '本准备脚本只用于缺少AppSecret的准备，不执行真实联调'
    RESULT['checks']['initialContainers'] = run('初始容器状态', ['docker', 'ps', '-a', '--format', '{{.ID}}\t{{.Names}}\t{{.Status}}'], allowed_output=True).stdout.splitlines()
    run('冻结Java版本', [str(JAVA_HOME / 'bin/java'), '-version'])
    RESULT['checks']['javaVersion'] = 'Temurin 21.0.12.1+1-LTS'
    RESULT['checks']['productionJarSHA256'] = hashlib.sha256(JAR.read_bytes()).hexdigest()
    assert RESULT['checks']['productionJarSHA256'] == '783dce595d9c45fe275db083093b625b9fdfe670debdfb56f030f6bf1ed0581f'
    with zipfile.ZipFile(JAR) as package:
        RESULT['checks']['packagedMigrationMatches'] = {p.name: p.read_bytes() == package.read('BOOT-INF/classes/db/migration/' + p.name) for p in (ROOT / 'apps/backend/src/main/resources/db/migration').glob('V*.sql')}
        RESULT['checks']['productionGatewayClassPresent'] = 'BOOT-INF/classes/com/pet/platform/customeridentity/infrastructure/WxJavaMiniProgramGateway.class' in package.namelist()
        RESULT['checks']['testGatewayPackaged'] = any('CustomerAuthenticationIT' in name or 'testWechatGateway' in name for name in package.namelist())
    assert all(RESULT['checks']['packagedMigrationMatches'].values()) and not RESULT['checks']['testGatewayPackaged']
    # 专用容器技术凭据只在本进程内存和600临时输入文件；不是验收微信客户凭据。
    db_password = secrets.token_hex(24)
    redis_password = secrets.token_hex(24)
    tag = str(os.getpid())
    with tempfile.TemporaryDirectory(prefix='p05-05-isolated-input-', dir=PRIVATE) as temp:
        temp = Path(temp)
        pg_env = temp / 'postgres.env'
        pg_env.write_text('POSTGRES_DB=p05_05_wechat_acceptance\nPOSTGRES_PASSWORD=' + db_password + '\n')
        pg_env.chmod(0o600)
        pg_name = 'pet-p05-05-wechat-pg-' + tag
        run('启动独立PostgreSQL', ['docker', 'run', '-d', '--rm', '--name', pg_name, '--label', 'pet.validation=P05-05-resume', '--tmpfs', '/var/lib/postgresql/data', '--env-file', str(pg_env), '-p', '127.0.0.1::5432', IMAGES['postgres'], '-c', 'log_statement=none', '-c', 'log_min_error_statement=panic', '-c', 'log_parameter_max_length=0', '-c', 'log_parameter_max_length_on_error=0'])
        CONTAINERS.append(pg_name)
        redis_conf = temp / 'redis.conf'
        redis_conf.write_text('bind 0.0.0.0\nprotected-mode yes\nsave ""\nappendonly no\nrequirepass ' + redis_password + '\n')
        redis_conf.chmod(0o600)
        redis_name = 'pet-p05-05-wechat-redis-' + tag
        run('启动独立Redis', ['docker', 'run', '-d', '--rm', '--name', redis_name, '--label', 'pet.validation=P05-05-resume', '--tmpfs', '/data', '-v', str(redis_conf) + ':/tmp/validation.conf:ro', '-p', '127.0.0.1::6379', IMAGES['redis'], 'redis-server', '/tmp/validation.conf'])
        CONTAINERS.append(redis_name)
        deadline = time.monotonic() + 45
        while time.monotonic() < deadline:
            ready = subprocess.run(['docker', 'exec', pg_name, 'pg_isready', '-U', 'postgres', '-d', 'p05_05_wechat_acceptance'], capture_output=True).returncode
            if ready == 0:
                break
            time.sleep(1)
        else:
            raise RuntimeError('PostgreSQL未就绪')
        pg_port = run('PostgreSQL回环端口', ['docker', 'port', pg_name, '5432'], allowed_output=True).stdout.strip().rsplit(':', 1)[1]
        redis_port = run('Redis回环端口', ['docker', 'port', redis_name, '6379'], allowed_output=True).stdout.strip().rsplit(':', 1)[1]
        sql('PostgreSQL版本', 'select version();')
        run('Redis版本', ['docker', 'exec', redis_name, 'redis-server', '--version'], allowed_output=True)
        for script in ['provision-identity-roles.sql', 'provision-platform-roles.sql', 'provision-customer-roles.sql']:
            sql('正式角色预配置:' + script, (ROOT / 'infra/database' / script).read_text(), record=False)
        roles = ''
        for role, capability, inheritance, membership in [
            ('p05_probe_runtime', 'pet_runtime', 'INHERIT', 'WITH INHERIT TRUE, SET FALSE'),
            ('p05_probe_migration', 'pet_migrator', 'INHERIT', 'WITH INHERIT TRUE, SET TRUE'),
            ('p05_probe_bootstrap', 'pet_bootstrap', 'NOINHERIT', 'WITH INHERIT FALSE, SET TRUE'),
        ]:
            roles += 'CREATE ROLE ' + role + ' LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEROLE NOCREATEDB ' + inheritance + " PASSWORD '" + db_password + "';\nGRANT " + capability + ' TO ' + role + ' ' + membership + ';\n'
        sql('临时独立登录身份及受限成员关系', roles, record=False)
        url = 'jdbc:postgresql://127.0.0.1:' + pg_port + '/p05_05_wechat_acceptance'
        migration_env = limited_env() | {'PET_MIGRATION_DATABASE_URL': url, 'PET_MIGRATION_DATABASE_USERNAME': 'p05_probe_migration', 'PET_MIGRATION_DATABASE_PASSWORD': db_password}
        run('正式生产JAR独立迁移', [str(ROOT / 'scripts/backend-identity.sh'), 'migrate'], env=migration_env)
        RESULT['checks']['migrations'] = sql('正式迁移history白名单', 'select version||\':\'||success from public.flyway_schema_history order by installed_rank;').splitlines()
        bootstrap_env = limited_env() | {'PET_BOOTSTRAP_DATABASE_URL': url, 'PET_BOOTSTRAP_DATABASE_USERNAME': 'p05_probe_bootstrap', 'PET_BOOTSTRAP_DATABASE_PASSWORD': db_password}
        run('正式初始化验收租户', [str(ROOT / 'scripts/backend-identity.sh'), 'bootstrap', '--tenant-code', 'p05-05-wechat-acceptance', '--tenant-name', 'P05-05微信隔离验收', '--admin-login', 'p05-acceptance-admin', '--password-stdin'], data=secrets.token_hex(24) + '\n', env=bootstrap_env)
        RESULT['checks']['tenant'] = sql('验收租户非敏感投影', "select id||':'||code||':'||status from public.platform_tenant where code='p05-05-wechat-acceptance';")
        RESULT['checks']['customerBaselineCounts'] = sql('客户及绑定基线数量', 'select (select count(*) from public.customer_subject)||\':\'||(select count(*) from public.customer_wechat_binding);')
        RESULT['checks']['runtimeSecurity'] = sql('受限运行角色/RLS/秘密列权限', "select current_user||':'||rolsuper||':'||rolbypassrls||':'||has_column_privilege(current_user,'public.customer_wechat_binding','open_id','SELECT') from pg_roles where rolname=current_user; select count(*) from public.customer_subject;", role='p05_probe_runtime').splitlines()
        RESULT['checks']['redisPing'] = run('真实Redis认证PING', ['docker', 'exec', '-i', redis_name, 'sh', '-c', 'read -r task_password; REDISCLI_AUTH="$task_password" redis-cli ping'], data=redis_password + '\n', allowed_output=True).stdout.strip()
        with socket.socket() as server:
            server.bind(('127.0.0.1', 0))
            PORT = server.getsockname()[1]
        backend_env = limited_env() | {'SPRING_PROFILES_ACTIVE': 'local', 'SPRING_CONFIG_ADDITIONAL_LOCATION': 'file:' + str(PRIVATE / 'application-local.yaml'), 'PET_DATABASE_URL': url, 'PET_DATABASE_USERNAME': 'p05_probe_runtime', 'PET_DATABASE_PASSWORD': db_password, 'PET_DATABASE_MIGRATION_ENABLED': 'false', 'PET_REDIS_HOST': '127.0.0.1', 'PET_REDIS_PORT': redis_port, 'PET_REDIS_PASSWORD': redis_password, 'SERVER_PORT': str(PORT)}
        started = now()
        BACKEND = subprocess.Popen([str(JAVA_HOME / 'bin/java'), '-jar', str(JAR)], cwd=ROOT, env=backend_env, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        threading.Thread(target=drain, args=(BACKEND.stdout,), daemon=True).start()
        deadline = time.monotonic() + 50
        while time.monotonic() < deadline:
            if BACKEND.poll() is not None:
                raise RuntimeError('正式后端启动失败')
            try:
                health, body = http('/actuator/health/readiness')
                if health['httpStatus'] == 200 and body.get('status') == 'UP':
                    break
            except (OSError, ValueError):
                pass
            time.sleep(1)
        else:
            raise RuntimeError('正式后端健康未就绪')
        RESULT['commands'].append({'label': '正式后端启动/健康', 'cwd': str(ROOT), 'argv': [str(JAVA_HOME / 'bin/java'), '-jar', str(JAR)], 'startedAt': started, 'finishedAt': now(), 'exitCode': None, 'summary': '独立local生产装配；HTTP readiness=200 UP；结束状态在cleanup记录'})
        RESULT['checks']['readiness'] = health
        RESULT['checks']['unauthenticatedMe'] = http('/api/customer/auth/me')[0]
        assert RESULT['checks']['unauthenticatedMe']['httpStatus'] == 401
        RESULT['checks']['productionGateway'] = {'class': 'com.pet.platform.customeridentity.infrastructure.WxJavaMiniProgramGateway', 'wxJavaVersion': '4.8.0', 'profile': 'local', 'testOverrides': False, 'enabledEntry': 'p05-05-acceptance', 'application': 'local-mini', 'appIdMatchesDesignated': True, 'upstreamExchange': 'NOT_EXECUTED'}
        RESULT['checks']['customerCountsAfterPreparation'] = sql('准备后客户及绑定未创建', 'select (select count(*) from public.customer_subject)||\':\'||(select count(*) from public.customer_wechat_binding);')
        RESULT['checks']['temporaryInputsRemovedWhenContextEnds'] = True
        RESULT['preparationStatus'] = 'RUNTIME_VERIFIED'


try:
    prepare()
except Exception as failure:
    # 不输出异常原文（可能含子进程配置/响应）；只报告固定准备失败状态。
    RESULT['preparationStatus'] = 'FAILED'
    RESULT['failureCategory'] = type(failure).__name__
finally:
    cleanup = {}
    if BACKEND is not None:
        BACKEND.terminate()
        try:
            cleanup['backendExitCode'] = BACKEND.wait(timeout=20)
        except subprocess.TimeoutExpired:
            BACKEND.kill()
            cleanup['backendExitCode'] = BACKEND.wait(timeout=5)
            cleanup['forcedBackendStop'] = True
    cleanup['containers'] = []
    for name in reversed(CONTAINERS):
        stopped = run('停止本轮专用容器', ['docker', 'stop', '--time', '15', name], check=False)
        cleanup['containers'].append({'name': name, 'stopExitCode': stopped.returncode})
    cleanup['finalContainers'] = run('结束容器状态', ['docker', 'ps', '-a', '--format', '{{.ID}}\t{{.Names}}\t{{.Status}}'], allowed_output=True).stdout.splitlines()
    cleanup['existingContainersStatePreserved'] = cleanup['finalContainers'] == RESULT['checks'].get('initialContainers')
    cleanup['userProcessesStopped'] = False
    cleanup['existingVolumesDeleted'] = False
    RESULT['cleanup'] = cleanup
    RESULT['finishedAt'] = now()
    RESULT['G12'] = 'BLOCKED'
    RESULT['P05-05'] = 'BLOCKED'
    RESULT['P05'] = 'IN_PROGRESS'
    (EVIDENCE / 'isolation-preparation.json').write_text(json.dumps(RESULT, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'preparation': RESULT.get('preparationStatus'), 'realWechat': 'NOT_EXECUTED', 'G12': 'BLOCKED', 'resourcesRestored': cleanup['existingContainersStatePreserved'], 'evidence': str(EVIDENCE / 'isolation-preparation.json')}, ensure_ascii=False))
    if RESULT.get('preparationStatus') != 'RUNTIME_VERIFIED' or not cleanup['existingContainersStatePreserved']:
        raise SystemExit(1)
