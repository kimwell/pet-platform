"""记录本轮真实命令、退出码和日志；不收集完整进程环境。"""
from pathlib import Path
import datetime
import json
import subprocess
import sys
import time

evidence = Path(__file__).resolve().parent
name, *command = sys.argv[1:]
started = datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat()
begin = time.monotonic()
with (evidence / f'{name}.log').open('w') as log:
    try:
        result = subprocess.run(command, stdout=log, stderr=subprocess.STDOUT, timeout=300)
        code = result.returncode
    except subprocess.TimeoutExpired:
        code = None
        log.write('\n记录器等待300秒后终止命令；未取得工具完成退出码。\n')
record = {
    'cwd': str(Path.cwd()), 'command': command, 'started_at': started,
    'duration_seconds': round(time.monotonic() - begin, 3), 'exit_code': code,
    'log': f'{name}.log', 'summary': (evidence / f'{name}.log').read_text()[-3000:],
}
(evidence / f'{name}.json').write_text(json.dumps(record, ensure_ascii=False, indent=2) + '\n')
print(json.dumps(record, ensure_ascii=False, indent=2))
sys.exit(code if code is not None and code >= 0 else 1)
