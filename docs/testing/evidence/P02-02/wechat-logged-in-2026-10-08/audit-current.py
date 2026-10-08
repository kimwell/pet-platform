"""核对续验保全、配置和证据一致性；不替代微信编译或运行验证。"""
import hashlib
import json
import re
import subprocess
from pathlib import Path

evidence = Path(__file__).resolve().parent
root = evidence.parents[4]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load(path):
    return json.loads(path.read_text())


before = load(evidence / 'preservation-before.json')
changed = [name for name, digest in before.items()
           if not (root / name).is_file() or sha(root / name) != digest]
assert not changed, f'保全清单出现改动：{changed}'

project = root / 'apps/wechat-miniprogram'
public = load(project / 'project.config.json')
private = load(project / 'project.private.config.json')
private_before = load(evidence / 'private-before.json')
assert public['appid'] == ''
assert private['appid'] == 'wx9bcab67d52e2ee04'
assert private['projectname'] == private_before['projectname']
for key, value in private_before['setting'].items():
    assert private['setting'][key] == value, f'私有既有设置被覆盖：{key}'
assert public['miniprogramRoot'] == 'miniprogram/'
assert public['libVersion'] == private['libVersion'] == '3.17.2'
assert public['setting']['urlCheck'] is True
assert 'typescript' in public['setting']['useCompilerPlugins']
assert load(project / 'tsconfig.json')['compilerOptions']['strict'] is True
assert load(project / 'package.json')['dependencies']['tdesign-miniprogram'] == '1.17.0'
assert not (project / 'miniprogram/app.js').exists()
assert load(project / 'miniprogram/app.json')['pages'] == ['pages/system/entry/index']

ignored = subprocess.run(
    ['git', 'check-ignore', '--', 'apps/wechat-miniprogram/project.private.config.json',
     'apps/wechat-miniprogram/miniprogram/miniprogram_npm',
     '.local-data/p02-02-offline-npm-before-tool'],
    cwd=root, capture_output=True, text=True,
)
assert ignored.returncode == 0
assert len(ignored.stdout.splitlines()) == 3

manifest = load(evidence / 'npm-tool-output-manifest.json')
npm_root = Path(manifest['root'])
files = [p for p in npm_root.rglob('*') if p.is_file()]
assert len(files) == manifest['file_count'] == 1177
for item in manifest['files']:
    path = npm_root / item['path']
    assert path.is_file() and sha(path) == item['sha256'], item['path']
for extension in ['js', 'json', 'wxml', 'wxss']:
    assert (npm_root / f'tdesign-miniprogram/button/button.{extension}').is_file()
assert (root / '.local-data/p02-02-offline-npm-before-tool').is_dir()

results = load(evidence / 'wechat-results.json')
assert results['gates']['p02_02'] == results['gates']['p02'] == 'COMPLETE'
assert results['gates']['p03'] == 'NOT_STARTED'
assert results['source_compile']['completion_observed'] == '【idle-compile】 all done'
assert load(evidence / 'config-after-tool.json')['exit_code'] == 0
assert load(evidence / 'cli-current-service.json')['exit_code'] == 246
assert load(evidence / 'device-user-feedback.json')['user_reply'] == '已打开且按钮正常'
for group in ['npm_build', 'source_compile', 'simulator', 'preview']:
    for name in results[group]['evidence']:
        assert (evidence / name).is_file(), name

# 检查本轮维护文档的本地链接，外部官方资料使用已有抓取证据。
docs = [root / name for name in [
    'README.md', 'docs/testing/P02-02-VERIFICATION.md',
    'docs/testing/ACCEPTANCE-MATRIX.md', 'docs/development/LOCAL-DEVELOPMENT.md',
    'docs/development/ROADMAP.md',
]]
links_checked = 0
for doc in docs:
    content = doc.read_text()
    assert 'P02-02 COMPLETE' in content or 'P02-02/P02 COMPLETE' in content
    for target in re.findall(r'\]\(([^)]+)\)', content):
        if '://' in target or target.startswith('#'):
            continue
        target = target.split('#')[0]
        if (doc.parent / target).resolve() == evidence / 'final-audit.json':
            # 记录器在本进程退出后写入本次实际退出码。
            continue
        assert (doc.parent / target).exists(), f'{doc.relative_to(root)}: {target}'
        links_checked += 1

print(json.dumps({
    'status': 'PASS', 'scope': '保全与文档/配置/产物一致性；不替代真实编译和运行',
    'preserved_files': len(before), 'changed_preserved_files': changed,
    'public_config_sha256': sha(project / 'project.config.json'),
    'public_appid': public['appid'], 'private_appid': private['appid'],
    'existing_private_settings_preserved': True,
    'ignored_paths': ignored.stdout.splitlines(),
    'npm_output_files_sha_verified': len(files), 'local_document_links_checked': links_checked,
    'business_sources': '本轮未修改后端/Web/infra；此SHA保全清单不覆盖其全部源码',
}, ensure_ascii=False, indent=2))
