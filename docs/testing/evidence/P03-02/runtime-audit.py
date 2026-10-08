"""P03-02 独立容器/生产 JAR 运行探针；所有凭据都是明确的公开技术夹具。"""
import datetime, hashlib, json, os, re, signal, socket, subprocess, time, urllib.error, urllib.request, uuid, zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[4]
out = Path(__file__).resolve().parent
java_home = os.environ.get('P03_JAVA_HOME', '/private/var/folders/5y/tt1690cx7k1gcq12252yf5rh0000gn/T/pet-platform-p02-02-_qb1x3n7/tools/jdk/jdk-21.0.12.1+1/Contents/Home')
jar = root / 'apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar'
image = 'postgres:17.11-bookworm@sha256:3645570cccdfa447589da9f57dd740faa29b30938e861289a5574b6ca6b03826'
container = 'pet-platform-p03-02-runtime-' + uuid.uuid4().hex[:8]
fixture_password = 'p03_runtime_technical_fixture'
records = []
processes = []


def command(name, argv, expected=0):
    start = datetime.datetime.now(datetime.timezone.utc).isoformat()
    result = subprocess.run(argv, cwd=root, capture_output=True, text=True, timeout=45)
    text = result.stdout + result.stderr
    (out / (name + '.log')).write_text(text)
    record = dict(cwd=str(root), argv=argv, exitCode=result.returncode, expectedExitCode=expected,
                  startedAtUTC=start, finishedAtUTC=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                  summary=text[-2000:], log=name + '.log')
    records.append(record)
    assert result.returncode == expected, record
    return result.stdout


def environment(profile='local'):
    env = {key: value for key, value in os.environ.items()
           if not key.startswith(('SPRING_', 'PET_DATABASE_', 'PET_PUBLIC_ORIGIN', 'PET_ENVIRONMENT', 'SERVER_'))}
    env.update(SPRING_PROFILES_ACTIVE=profile, SERVER_ADDRESS='127.0.0.1', SERVER_PORT='0',
               PET_DATABASE_URL=database_url, PET_DATABASE_USERNAME='p03_runtime_fixture',
               PET_DATABASE_PASSWORD=fixture_password)
    if profile == 'prod':
        env['PET_PUBLIC_ORIGIN'] = 'https://example.invalid'
    return env


def launch(name, env):
    log = (out / (name + '.log')).open('w')
    argv = [java_home + '/bin/java', '-jar', str(jar)]
    process = subprocess.Popen(argv, cwd=root / 'apps/backend', env=env, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
    processes.append((process, log, name))
    return process


def port_when_ready(process, name):
    deadline = time.monotonic() + 40
    while time.monotonic() < deadline:
        assert process.poll() is None, (out / (name + '.log')).read_text()[-3000:]
        text = (out / (name + '.log')).read_text()
        match = re.search(r'Tomcat started on port (\d+)', text)
        if match and 'Started Application' in text:
            return int(match.group(1))
        time.sleep(0.2)
    raise AssertionError('应用启动超时')


def http(port, path):
    url = 'http://127.0.0.1:' + str(port) + path
    try:
        response = urllib.request.urlopen(url, timeout=15)
    except urllib.error.HTTPError as error:
        response = error
    body = response.read().decode('utf-8')
    item = dict(url=url, status=response.code, body=json.loads(body))
    assert not any(secret in body for secret in [fixture_password, 'jdbc:', 'password', 'username', 'components', 'details'])
    records.append(item)
    return item


def stop(process, name, port=None):
    if process.poll() is None:
        os.killpg(process.pid, signal.SIGTERM)
    code = process.wait(timeout=25)
    text = (out / (name + '.log')).read_text()
    assert 'Shutdown completed' in text
    if port:
        with socket.socket() as check:
            assert check.connect_ex(('127.0.0.1', port)) != 0
    records.append(dict(process=name, pid=process.pid, exitCode=code, gracefulShutdown=True, portClosed=True if port else None))


try:
    with zipfile.ZipFile(jar) as package:
        entries = package.namelist()
        classes = [p for p in entries if p.startswith('BOOT-INF/classes/') and p.endswith('.class')]
        forbidden = [p for p in entries if any(s in p for s in ['com/pet/testing', 'persistence-migrations', 'ProtocolFixtures', 'ApiProtocolTest', 'ApiProtocolIT', 'ProbeRepository', 'PersistenceProbe', 'testcontainers'])]
        migrations = [p for p in entries if p.startswith('BOOT-INF/classes/db/migration/')]
        assert not forbidden, forbidden
        assert not any(p.endswith('.sql') for p in migrations), migrations
        assert all(b'/__protocol' not in package.read(p) for p in classes)
        (out / 'artifact-audit.json').write_text(json.dumps(dict(jar=str(jar), sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),
            projectClasses=classes, productionMigrations=migrations, forbiddenEntries=forbidden, testArtifactsAbsent=True), ensure_ascii=False, indent=2) + '\n')
    command('runtime-postgres-create', ['docker', 'run', '-d', '--name', container, '--label', 'com.pet.platform.validation=P03-02',
            '-p', '127.0.0.1::5432', '-e', 'POSTGRES_DB=p03_runtime_fixture', '-e', 'POSTGRES_USER=p03_runtime_fixture',
            '-e', 'POSTGRES_PASSWORD=' + fixture_password, image])
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        ready = subprocess.run(['docker', 'exec', container, 'pg_isready', '-U', 'p03_runtime_fixture', '-d', 'p03_runtime_fixture'], capture_output=True)
        if ready.returncode == 0:
            break
        time.sleep(0.3)
    assert ready.returncode == 0
    mapping = json.loads(command('runtime-postgres-port', ['docker', 'inspect', '--format', '{{json .NetworkSettings.Ports}}', container]))
    database_port = int(mapping['5432/tcp'][0]['HostPort'])
    database_url = 'jdbc:postgresql://127.0.0.1:' + str(database_port) + '/p03_runtime_fixture'
    records.append(dict(databaseReady=True, fixture='公开技术容器凭据，不是业务或生产账号', databasePort=database_port))

    app = launch('local-startup', environment())
    port = port_when_ready(app, 'local-startup')
    for path in ['/actuator/health', '/actuator/health/liveness', '/actuator/health/readiness']:
        result = http(port, path)
        assert result['status'] == 200 and result['body'].get('status') == 'UP'
        assert set(result['body']).issubset({'status', 'groups'})
    for path in ['/', '/__protocol/success', '/actuator/env', '/actuator/beans']:
        assert http(port, path)['status'] == 404
    startup = (out / 'local-startup.log').read_text()
    assert startup.index('Successfully validated') < startup.index('Initialized JPA EntityManagerFactory')
    tables = command('runtime-production-tables', ['docker', 'exec', container, 'psql', '-U', 'p03_runtime_fixture', '-d', 'p03_runtime_fixture', '-Atc', "select tablename from pg_tables where schemaname='public' order by tablename"])
    assert tables.strip().splitlines() == ['flyway_schema_history']
    command('runtime-postgres-stop', ['docker', 'stop', '--time', '10', container])
    assert http(port, '/actuator/health/readiness')['status'] == 503
    live = http(port, '/actuator/health/liveness')
    assert live['status'] == 200 and live['body'] == {'status': 'UP'}
    assert http(port, '/actuator/health')['status'] == 503
    stop(app, 'local-startup', port)
    command('runtime-postgres-restart', ['docker', 'start', container])
    # 等待同一技术数据库恢复；不重建或删除其迁移历史。
    deadline = time.monotonic() + 25
    while time.monotonic() < deadline:
        ready = subprocess.run(['docker', 'exec', container, 'pg_isready', '-U', 'p03_runtime_fixture', '-d', 'p03_runtime_fixture'], capture_output=True)
        if ready.returncode == 0:
            break
        time.sleep(0.2)
    assert ready.returncode == 0
    mapping = json.loads(command('runtime-postgres-restarted-port', ['docker', 'inspect', '--format', '{{json .NetworkSettings.Ports}}', container]))
    database_port = int(mapping['5432/tcp'][0]['HostPort'])
    database_url = 'jdbc:postgresql://127.0.0.1:' + str(database_port) + '/p03_runtime_fixture'
    app = launch('local-restart', environment())
    port = port_when_ready(app, 'local-restart')
    assert http(port, '/actuator/health/readiness')['status'] == 200
    stop(app, 'local-restart', port)
    prod = launch('prod-startup', environment('prod'))
    port = port_when_ready(prod, 'prod-startup')
    assert http(port, '/actuator/health/readiness')['status'] == 200
    assert 'FlywayExecutor' not in (out / 'prod-startup.log').read_text()
    stop(prod, 'prod-startup', port)
    cases = [
        ('prod-missing-url', 'prod', {'PET_DATABASE_URL': ''}, '缺少必要数据库配置：spring.datasource.url'),
        ('prod-missing-user', 'prod', {'PET_DATABASE_USERNAME': ''}, '缺少必要数据库配置：spring.datasource.username'),
        ('prod-missing-password', 'prod', {'PET_DATABASE_PASSWORD': ''}, '缺少必要数据库配置：spring.datasource.password'),
        ('prod-migration-refused', 'prod', {'SPRING_FLYWAY_ENABLED': 'true'}, 'spring.flyway.enabled 必须设置为 false'),
        ('wrong-password', 'local', {'PET_DATABASE_PASSWORD': 'P03_TECHNICAL_WRONG_PASSWORD'}, 'password authentication failed'),
        ('unavailable-database', 'local', {'PET_DATABASE_URL': 'jdbc:postgresql://127.0.0.1:1/p03_runtime_fixture'}, 'Connection to 127.0.0.1:1 refused'),
    ]
    for name, profile, overrides, expected in cases:
        env = environment(profile)
        env.update(overrides)
        process = launch(name, env)
        code = process.wait(timeout=40)
        text = (out / (name + '.log')).read_text()
        assert code != 0 and expected in text and 'Started Application' not in text, (name, code, text[-2500:])
        assert fixture_password not in text and 'P03_TECHNICAL_WRONG_PASSWORD' not in text
        records.append(dict(process=name, exitCode=code, expectedFailureObserved=True, expectedMessage=expected, log=name + '.log', noPasswordLeak=True))
finally:
    for process, log, name in processes:
        if process.poll() is None:
            os.killpg(process.pid, signal.SIGTERM)
            process.wait(timeout=30)
        log.close()
    present = subprocess.run(['docker', 'inspect', '--format', '{{.State.Status}}', container], capture_output=True, text=True)
    if present.returncode == 0:
        command('runtime-postgres-final-stop', ['docker', 'stop', '--time', '10', container])
        # 只移除本脚本新建的临时容器/匿名测试卷；基线中已有卷不在操作目标中。
        command('runtime-postgres-cleanup', ['docker', 'rm', '-v', container])
    (out / 'local-runtime.json').write_text(json.dumps(records, ensure_ascii=False, indent=2) + '\n')
print('生产JAR、数据库健康/故障、配置反例、正常停止及临时资源清理通过')
