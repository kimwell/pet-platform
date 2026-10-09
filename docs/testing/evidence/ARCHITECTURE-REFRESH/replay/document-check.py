"""收尾文档、证据链接和已保全文件检查。"""
import hashlib
import json
from pathlib import Path
import re
import subprocess

ROOT = Path('/Users/kimwell/work/pet-platform')
EVIDENCE = ROOT / 'docs/testing/evidence/ARCHITECTURE-REFRESH'
changed = subprocess.check_output(['git', 'diff', '--name-only'], cwd=ROOT, text=True).splitlines()
changed += subprocess.check_output(['git', 'ls-files', '--others', '--exclude-standard'],
                                   cwd=ROOT, text=True).splitlines()
broken = []
for name in changed:
    source = ROOT / name
    if not source.is_file() or source.suffix != '.md':
        continue
    for match in re.finditer(r'\]\(([^)]+)\)', source.read_text()):
        target = match.group(1).split('#')[0].strip('<>')
        if not target or '://' in target:
            continue
        resolved = Path(target) if target.startswith('/') else source.parent / target
        if not resolved.exists():
            broken.append({'file': name, 'target': target})
assert not broken, broken
records = [json.loads(line) for line in (EVIDENCE / 'commands.jsonl').read_text().splitlines()]
assert all((EVIDENCE / item['log']).is_file() for item in records)
baseline = json.loads((EVIDENCE / 'preservation-baseline.json').read_text())
assert all((ROOT / name).is_file() and hashlib.sha256((ROOT / name).read_bytes()).hexdigest() == digest
           for name, digest in baseline.items())
assert json.loads((EVIDENCE / 'cleanup.json').read_text())['status'] == 'PASS'
result = subprocess.run(['git', 'diff', '--check'], cwd=ROOT)
assert result.returncode == 0
report = {'status': 'PASS', 'evidenceLevel': 'DOCUMENTED',
          'preservedFiles': len(baseline), 'brokenLinks': broken,
          'recordedCommandLogs': len(records), 'whitespaceExitCode': result.returncode,
          'cleanupStatus': 'PASS'}
(EVIDENCE / 'document-final.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
print(json.dumps(report, ensure_ascii=False))
