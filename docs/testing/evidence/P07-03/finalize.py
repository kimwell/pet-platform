"""汇总本轮公开证据；不连接服务、不读取秘密、不改历史文件。"""
import datetime
import hashlib
import json
import pathlib
import re
import subprocess
from urllib.parse import unquote

root = pathlib.Path('/Users/kimwell/work/pet-platform')
evidence = root / 'docs/testing/evidence/P07-03'
started = datetime.datetime.now(datetime.timezone.utc).isoformat()
own_outputs = {'command-index.json', 'file-manifest.json', 'final-audit.json', 'finalize.json'}


def git(*args):
    return subprocess.check_output(['git', *args], cwd=root).decode()


def save(name, value):
    (evidence / name).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')


baseline = json.loads((evidence / 'baseline.json').read_text())
modified = git('diff', '--name-only').splitlines()
added = git('ls-files', '--others', '--exclude-standard').splitlines()
all_paths = sorted(set(modified + added + ['docs/testing/evidence/P07-03/' + name for name in own_outputs]))
changed_baseline = [path for path, sha in baseline['files'].items()
                    if not (root / path).is_file() or hashlib.sha256((root / path).read_bytes()).hexdigest() != sha]
normalization_only = []
for path in set(changed_baseline) - set(modified):
    # 起始clean基线来自HEAD原始内容；既有CRLF工作树经Git text属性规范化后比较。
    head_content = subprocess.check_output(['git', 'show', 'HEAD:' + path], cwd=root)
    actual_blob = git('hash-object', path).strip()
    expected_blob = git('rev-parse', 'HEAD:' + path).strip()
    assert hashlib.sha256(head_content).hexdigest() == baseline['files'][path]
    assert actual_blob == expected_blob, path
    normalization_only.append({'file': path, 'headRawSHA256': baseline['files'][path],
                               'workingSHA256': hashlib.sha256((root / path).read_bytes()).hexdigest(),
                               'normalizedBlob': actual_blob, 'status': 'UNCHANGED_GIT_CONTENT'})
changed_baseline = [path for path in changed_baseline if path in modified]
assert sorted(changed_baseline) == sorted(modified), '基线变更必须与Git清单一致'
assert git('rev-parse', 'HEAD').strip() == baseline['head'], '不得自动提交'
assert all(path.startswith('apps/admin-web/src/') or path.startswith('docs/') or path == 'README.md' for path in modified)
assert all(path.startswith('apps/admin-web/src/features/identity/users/') or path.startswith('docs/') for path in added)

preserved = {}
groups = {
    'ui': lambda p: p.startswith('ui/'),
    'backendIncludingFrozenMigrations': lambda p: p.startswith('apps/backend/'),
    'generatedAndSharedContracts': lambda p: p.startswith('packages/api-contracts/') or p.startswith('docs/contracts/generated/'),
    'miniprogram': lambda p: p.startswith('apps/wechat-miniprogram/'),
    'dependenciesAndVersions': lambda p: pathlib.PurePosixPath(p).name in ['pnpm-lock.yaml', 'package.json', 'pom.xml', 'VERSION-MATRIX.md'],
    'priorVerification': lambda p: p.startswith('docs/testing/evidence/') or (p.startswith('docs/testing/P') and p.endswith('.md')),
}
for group, predicate in groups.items():
    files = [p for p in baseline['files'] if predicate(p)]
    affected = [p for p in changed_baseline if predicate(p)]
    assert not affected, group
    preserved[group] = {'baselineFiles': len(files), 'changedFiles': affected, 'status': 'PASS'}

# 本轮Markdown本地引用逐个存在性检查；不声称网络链接已验证。
links = []
missing = []
for path in all_paths:
    file = root / path
    if not file.is_file() or file.suffix != '.md':
        continue
    for raw in re.findall(r'\]\(([^)]+)\)', file.read_text()):
        target = raw.strip('<>').split('#')[0]
        if not target or re.match(r'[a-zA-Z]+:', target):
            continue
        resolved = (file.parent / unquote(target)).resolve()
        exists = resolved.exists() or (resolved.parent == evidence and resolved.name in own_outputs)
        links.append({'file': path, 'target': target, 'exists': exists})
        if not exists:
            missing.append(links[-1])
assert not missing, missing

# 原始命令JSON包括失败与长驻服务，按来源保存，不折叠历史失败。
commands = []
for file in sorted(evidence.glob('*.json')):
    if file.name in own_outputs:
        continue
    data = json.loads(file.read_text())
    candidates = data if isinstance(data, list) else [data]
    if isinstance(data, dict) and isinstance(data.get('records'), list):
        candidates += data['records']
    for number, item in enumerate(candidates):
        if not isinstance(item, dict) or 'command' not in item:
            continue
        row = {key: value for key, value in item.items() if key in
               ['label', 'cwd', 'command', 'exitCode', 'status', 'startedAt', 'completedAt', 'log', 'reason', 'summary', 'toolchain']}
        row['cwd'] = item.get('cwd', str(root))
        row['source'] = file.name
        row['record'] = number
        if 'exitCode' not in row:
            row['exitCode'] = None
        row['outcome'] = 'PASS' if row['exitCode'] == 0 else 'FAIL' if row['exitCode'] is not None else row.get('status', 'NOT_RECORDED')
        commands.append(row)
save('command-index.json', {
    'cwd': str(root), 'createdAt': started,
    'policy': '逐次命令与观察原件均保留；退出未记录不虚构0；长驻服务由cleanup确认停止；环境故障不计测试通过',
    'commands': commands,
    'tests': {'historicalBackend': {'tests': 455, 'status': 'NOT_EXECUTED', 'source': '../P07-01/'},
              'webBaseline': {'files': 6, 'tests': 188, 'passed': 188, 'source': 'test-baseline.json'},
              'webFinal': {'files': 7, 'tests': 244, 'passed': 244, 'source': 'test-final-244.json'},
              'componentFinal': {'scenarios': 41, 'passed': 41, 'evidenceType': '技术网络替身', 'source': 'browser-components-final.json'},
              'formalBrowserFinal': {'scenarios': 43, 'passed': 43, 'evidenceType': '正式后端/PG/Redis/STAFF', 'source': 'browser-real-second.json'},
              'contractsRawAttempt': {'registered': 10, 'passed': 0, 'failures': 0, 'errors': 10, 'skipped': 0,
                                     'status': 'ENVIRONMENT_INITIALIZATION_FAILED', 'source': 'contracts-raw-failed-reports/'},
              'contractsDefaultAndSocket': {'completedTestBodies': 0, 'status': 'INTERRUPTED_ENVIRONMENT_HANG',
                                           'sources': ['contracts-check.json', 'contracts-raw-socket.json']}},
})

source_paths = [p for p in all_paths if p.startswith('apps/admin-web/src/') and not p.endswith('.test.ts')]
forbidden = ['dangerouslySetInnerHTML', 'P07-03验收', 'p07_03_acceptance', 'employee55', 'P07-03超长']
production_matches = [p for p in source_paths if any(word in (root / p).read_text() for word in forbidden)]
dist_files = [p for p in (root / 'apps/admin-web/dist').rglob('*') if p.is_file()]
dist_matches = [str(p.relative_to(root)) for p in dist_files
                if any(word.encode() in p.read_bytes() for word in forbidden[1:])]
assert not production_matches and not dist_matches, '验收夹具不得进入生产页面或产物'
cleanup = json.loads((evidence / 'cleanup.json').read_text())
assert cleanup['exitCode'] == 0 and all(cleanup['closedPorts'].values())
assert cleanup['existingContainersPreserved'] and cleanup['originalVolumesPreserved'] and not cleanup['remainingSecretInputs']
assert not (root / 'enterprise-app-scaffold').exists() and not (root / '.delivery-os').exists()
for name in ['typecheck-final-accepted', 'lint-final-accepted', 'test-final-244', 'build-final-accepted', 'check-repo-final', 'diff-check-final']:
    assert json.loads((evidence / (name + '.json')).read_text())['exitCode'] == 0, name
contracts = [json.loads((evidence / (name + '.json')).read_text())['exitCode'] for name in
             ['contracts-check', 'contracts-raw-attempt', 'contracts-raw-socket']]
assert contracts == [1, 1, 1]
save('final-audit.json', {
    'cwd': str(root), 'createdAt': started, 'head': baseline['head'], 'headUnchanged': True,
    'baselineFiles': len(baseline['files']), 'changedBaselineFiles': changed_baseline,
    'normalizationOnly': normalization_only,
    'preserved': preserved, 'localMarkdownLinks': {'checked': len(links), 'missing': missing, 'status': 'PASS'},
    'productionFixtureAndUnescapedHtmlScan': {'files': len(source_paths), 'matchingFiles': production_matches,
                                            'distFiles': len(dist_files), 'matchingDistFiles': dist_matches, 'status': 'PASS'},
    'secretScan': {'source': 'secret-value-scan.json', 'matchingFiles': [], 'method': '清理前本轮已知随机凭据精确值；截图另目视'},
    'resources': {'source': 'cleanup.json', 'status': 'PASS'},
    'requiredChecks': {'webRepoDiff': 'PASS', 'contracts': 'FAIL', 'contractExitCodes': contracts},
    'gates': {f'G{n:02}': 'PARTIAL' if n == 13 else 'PASS' for n in range(1, 15)},
    'taskStatus': 'IN_PROGRESS', 'phaseStatus': 'IN_PROGRESS',
    'remaining': 'G13标准Docker/Testcontainers契约检查未完成；不以既有生成文件或历史10项代替本轮PASS',
    'noAutomaticCommitPushReleaseDeploy': True,
})

manifest = []
for path in all_paths:
    file = root / path
    row = {'path': path, 'change': 'MODIFIED' if path in modified else 'ADDED'}
    if file.parent == evidence and file.name in own_outputs:
        row.update({'digest': None, 'note': '索引/清单/审计自引用不递归计摘要'})
    else:
        assert file.is_file(), path
        content = file.read_bytes()
        row.update({'bytes': len(content), 'sha256': hashlib.sha256(content).hexdigest()})
    manifest.append(row)
save('file-manifest.json', {'cwd': str(root), 'createdAt': started, 'files': manifest,
                            'counts': {'modified': len(modified), 'added': len(all_paths) - len(modified),
                                       'webModified': sum(p.startswith('apps/admin-web/src/') for p in modified),
                                       'webAdded': sum(p.startswith('apps/admin-web/src/') for p in added)}})
metadata = {'cwd': str(root), 'command': ['python3', 'docs/testing/evidence/P07-03/finalize.py'],
            'exitCode': 0, 'startedAt': started, 'completedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(),
            'summary': f'{len(baseline["files"])}基线文件；{len(modified)}修改；{len(all_paths)-len(modified)}新增；本地链接{len(links)}存在；G13 PARTIAL，P07-03/P07 IN_PROGRESS'}
save('finalize.json', metadata)
print(json.dumps(metadata, ensure_ascii=False))
