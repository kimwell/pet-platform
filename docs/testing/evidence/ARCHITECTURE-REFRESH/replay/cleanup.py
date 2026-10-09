"""只清理本轮独立验收资源，保留用户基础设施与开发者工具。"""
import datetime
import json
import os
from pathlib import Path
import signal
import socket
import subprocess
import time

ROOT = Path('/Users/kimwell/work/pet-platform')
EVIDENCE = ROOT / 'docs/testing/evidence/ARCHITECTURE-REFRESH'
RUNTIME = ROOT / '.local-data/architecture/runtime'
resources = json.loads((RUNTIME / 'resources.json').read_text())
commands = []


def run(argv):
    result = subprocess.run(argv, cwd=ROOT, capture_output=True, text=True)
    commands.append({'cwd': str(ROOT), 'command': argv, 'exitCode': result.returncode,
                     'summary': (result.stdout + result.stderr).strip()})
    (EVIDENCE / 'cleanup-command-progress.json').write_text(
        json.dumps(commands, ensure_ascii=False, indent=2) + '\n')
    return result


stopped_processes = []
for key, expected in [('backendPid', str(RUNTIME / 'backend.jar')),
                      ('webPid', '--filter @pet/admin-web dev --port 18102')]:
    pid = resources[key]
    observed = run(['ps', '-p', str(pid), '-o', 'pid=,pgid=,command='])
    if observed.returncode:
        stopped_processes.append({'pid': pid, 'result': '已退出'})
        continue
    assert expected in observed.stdout, '进程归属不匹配，拒绝终止'
    assert os.getpgid(pid) == pid, '非本轮独立进程组，拒绝终止'
    os.killpg(pid, signal.SIGTERM)
    commands.append({'cwd': str(ROOT), 'command': ['os.killpg', str(pid), 'SIGTERM'],
                     'exitCode': 0, 'summary': '归属校验后终止本轮进程组'})
    for _ in range(100):
        if subprocess.run(['ps', '-p', str(pid)], stdout=subprocess.DEVNULL,
                          stderr=subprocess.DEVNULL).returncode:
            break
        time.sleep(0.1)
    else:
        assert os.getpgid(pid) == pid
        os.killpg(pid, signal.SIGKILL)
        commands.append({'cwd': str(ROOT), 'command': ['os.killpg', str(pid), 'SIGKILL'],
                         'exitCode': 0, 'summary': '超时后仅终止已校验本轮进程组'})
    stopped_processes.append({'pid': pid, 'result': '本轮进程组已终止'})

removed_containers = []
for key in ['pg', 'redis']:
    name = resources[key]
    observed = run(['docker', 'inspect', '--format', '{{json .Config.Labels}}', name])
    if observed.returncode:
        assert ('no such object' in observed.stderr.lower() or 'no such container' in observed.stderr.lower()), \
            '容器查询失败，不能认定资源已移除'
        removed_containers.append({'name': name, 'result': '已不存在'})
        continue
    assert json.loads(observed.stdout).get('pet.validation') == '架构改造'
    assert name in ('pet-arch-pg', 'pet-arch-redis')
    assert run(['docker', 'stop', '--time', '10', name]).returncode == 0
    # 验收容器使用 --rm，停止后通常已由 Docker 自动移除。
    remaining = run(['docker', 'inspect', '--format', '{{json .Config.Labels}}', name])
    if remaining.returncode == 0:
        assert json.loads(remaining.stdout).get('pet.validation') == '架构改造'
        assert run(['docker', 'rm', name]).returncode == 0
    else:
        assert ('no such object' in remaining.stderr.lower() or 'no such container' in remaining.stderr.lower())
    removed_containers.append({'name': name, 'result': '本轮 tmpfs 验收容器已移除'})

ports = {}
for key in ('backendPort', 'webPort', 'pgPort', 'redisPort'):
    port = resources[key]
    with socket.socket() as probe:
        probe.settimeout(0.3)
        ports[str(port)] = probe.connect_ex(('127.0.0.1', port)) != 0
    assert ports[str(port)], '本轮回环端口仍被监听'

baseline = json.loads((EVIDENCE / 'runtime-setup.json').read_text())[0]['summary']
original = dict(line.split('\t', 1) for line in baseline.strip().splitlines())
current_result = run(['docker', 'ps', '-a', '--format', '{{.Names}}\t{{.State}}'])
assert current_result.returncode == 0
current = dict(line.split('\t', 1) for line in current_result.stdout.strip().splitlines())
preserved = {name: {'before': state, 'after': current.get(name),
                    'unchanged': current.get(name) == state}
             for name, state in original.items()}
assert all(item['unchanged'] for item in preserved.values()), '原有容器保全状态变化'

removed_files = []
for name in ['credentials.json', 'command-env.json', 'postgres.env', 'redis.conf',
             'wechat.yaml', 'worker-password', 'platform-reset-password',
             'root-staff-state.json', 'before-lifecycle-state.json', 'final-state.json',
             'browser-final-failure.png', 'browser-manage-failure.png',
             'browser-supplement-failure.png']:
    target = RUNTIME / name
    if target.exists():
        target.unlink()
        removed_files.append(name)

result = {'status': 'PASS', 'checkedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(),
          'evidenceLevel': 'RUNTIME_VERIFIED', 'cwd': str(ROOT),
          'processes': stopped_processes, 'containers': removed_containers,
          'portsReleased': ports, 'originalContainers': preserved,
          'removedPrivateCopies': removed_files,
          'preserved': ['既有本地服务', '原工程私有微信配置', 'ui/', '微信开发者工具'],
          'commands': commands}
(EVIDENCE / 'cleanup.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({key: value for key, value in result.items() if key != 'commands'},
                 ensure_ascii=False))
