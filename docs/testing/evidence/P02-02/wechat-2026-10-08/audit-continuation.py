"""复核续验的保全边界、文档链接和私有绑定，不代替微信编译。"""
from pathlib import Path
import datetime
import hashlib
import json
import plistlib
import re
import subprocess

root = Path.cwd()
evidence = Path(__file__).resolve().parent
before = json.loads((evidence / 'preservation-before.json').read_text())
changed = []
for relative, expected in before.items():
    path = root / relative
    if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
        changed.append(relative)
historical = evidence.parent / 'EVIDENCE-MANIFEST.json'
files = json.loads(historical.read_text())['files']
mismatches = []
for item in files:
    path = historical.parent / item['path']
    if not path.is_file() or path.stat().st_size != item['size'] or hashlib.sha256(path.read_bytes()).hexdigest() != item['sha256']:
        mismatches.append(item['path'])
documents = [
    'docs/testing/P02-02-VERIFICATION.md', 'docs/testing/ACCEPTANCE-MATRIX.md',
    'docs/development/LOCAL-DEVELOPMENT.md', 'docs/development/ROADMAP.md',
]
broken = []
for relative in documents:
    document = root / relative
    for target in re.findall(r'(?<!!)\[[^\]]*\]\(([^)]+)\)', document.read_text()):
        if '://' in target or target.startswith('#'):
            continue
        if not (document.parent / target.split('#')[0]).exists():
            broken.append({'file': relative, 'target': target})
with Path('/Applications/wechatwebdevtools.app/Contents/Info.plist').open('rb') as stream:
    old_tool = plistlib.load(stream)
private = 'apps/wechat-miniprogram/project.private.config.json'
old_preserved = old_tool['CFBundleShortVersionString'] == '2.01.2510260'
ignored = subprocess.run(['git', 'check-ignore', '-q', private]).returncode == 0
public_empty = json.loads((root / 'apps/wechat-miniprogram/project.config.json').read_text())['appid'] == ''
appid_matches = json.loads((root / private).read_text())['appid'] == 'wx9bcab67d52e2ee04'
report = {
    'checked_at': datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat(),
    'cwd': str(root), 'preserved_file_count': len(before), 'changed_preserved_files': changed,
    'historical_p02_02_manifest_file_count': len(files), 'historical_p02_02_manifest_mismatch': mismatches,
    'broken_local_links': broken, 'old_tool_version_unchanged': old_preserved,
    'public_appid_empty': public_empty, 'private_appid_matches': appid_matches,
    'private_config_ignored': ignored, 'updated_documents': documents,
    'p02_02': 'BLOCKED', 'p02': 'IN_PROGRESS', 'p03': 'NOT_STARTED',
}
(evidence / 'final-audit.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
print(json.dumps(report, ensure_ascii=False, indent=2))
assert not changed and not mismatches and not broken
assert old_preserved and ignored and public_empty and appid_matches
