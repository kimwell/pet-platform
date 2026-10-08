"""P02-02 技术证据记录器；不读取或改写用户秘密配置。"""
import datetime
import json
import os
import pathlib
import subprocess
import sys
import time

ROOT = pathlib.Path(__file__).resolve().parents[4]
OUT = pathlib.Path(__file__).parent
TASK = pathlib.Path((ROOT / '.local-data/p02-02-path').read_text())
TOOLS = pathlib.Path('/var/folders/5y/tt1690cx7k1gcq12252yf5rh0000gn/T/enterprise-scaffold-p01-01-plwt53cm/tools')
ENV = os.environ.copy()
ENV['JAVA_HOME'] = str(TASK / 'tools/jdk/jdk-21.0.12.1+1/Contents/Home')
ENV['PATH'] = ':'.join([str(TASK / 'bin'), str(TOOLS / 'node-v24.21.0-darwin-arm64/bin'), str(pathlib.Path(ENV['JAVA_HOME']) / 'bin'), ENV['PATH']])
COMPOSE = ['docker', 'compose', '--env-file', str(ROOT / '.local-data/p02-infra.env'), '-p', 'pet-platform-p02-validation', '-f', str(ROOT / 'infra/local/docker-compose.yml')]

def save(name, value):
    (OUT / (name + '.json')).write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n')

def run(name, args, cwd=ROOT, expected=0, overrides=None, timeout=180):
    env = ENV.copy()
    env.update(overrides or {})
    start = time.monotonic()
    result = subprocess.run(args, cwd=cwd, env=env, capture_output=True, text=True, timeout=timeout)
    output = result.stdout + result.stderr
    # 本轮只复用公开夹具；仍按配置键遮盖凭据，不输出完整解析配置。
    fixture = ROOT / '.local-data/p02-infra.env'
    if fixture.exists():
        for line in fixture.read_text().splitlines():
            if '=' in line:
                key, value = line.split('=', 1)
                if any(word in key for word in ['PASSWORD', 'PASS', 'SECRET', 'TOKEN']) and value:
                    output = output.replace(value.strip("'\""), '<已脱敏>')
    (OUT / (name + '.log')).write_text(output)
    record = {'id': name, 'cwd': str(cwd), 'command': args, 'exit_code': result.returncode,
              'expected_exit_code': expected, 'status': 'PASS' if result.returncode == expected else 'FAIL',
              'started_at': datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat(),
              'duration_seconds': round(time.monotonic()-start, 3), 'evidence': name+'.log', 'summary': output[-2200:]}
    if overrides:
        record['environment_fixture'] = {key: '<公开反例输入>' for key in overrides}
    save(name, record)
    print(json.dumps({key: record[key] for key in ['id','exit_code','expected_exit_code','status','duration_seconds']}, ensure_ascii=False), flush=True)
    return result

if __name__ == '__main__':
    run(sys.argv[1], sys.argv[2:])
