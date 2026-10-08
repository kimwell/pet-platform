"""仅审计本轮产物/证据，不修改工程、运行库或容器。"""
from pathlib import Path
import datetime
import hashlib
import json
import re
import subprocess
import xml.etree.ElementTree as ET
import zipfile

root = Path(__file__).resolve().parents[4]
ev = Path(__file__).resolve().parent
target = root / 'apps/backend/target'


def write(name, data):
    (ev / name).write_text(json.dumps(data, ensure_ascii=False, indent=2))


def cases_at(directory):
    cases = set()
    stats = {}
    suites = []
    for group in ('surefire', 'failsafe'):
        total = {key: 0 for key in ('tests', 'failures', 'errors', 'skipped')}
        for path in sorted((directory / (group + '-reports')).glob('TEST-*.xml')):
            suite = ET.parse(path).getroot()
            row = {'name': suite.attrib['name'], **{key: int(suite.attrib[key]) for key in total}}
            suites.append(row)
            for key in total:
                total[key] += row[key]
            cases.update((case.attrib['classname'], case.attrib['name']) for case in suite.findall('testcase'))
        stats[group] = total
    return cases, stats, suites


cases, stats, suites = cases_at(ev / 'verify-final-reports')
baseline, baseline_stats, _ = cases_at(root / 'docs/testing/evidence/P05-01/verify-final-reports')
missing_cases = sorted(baseline - cases)
added_cases = sorted(cases - baseline)
jar = target / 'pet-platform-backend-0.0.0-SNAPSHOT.jar'
test_classes = {str(path.relative_to(target / 'test-classes')) for path in (target / 'test-classes').rglob('*.class')}
test_resources = {str(path.relative_to(root / 'apps/backend/src/test/resources')) for path in (root / 'apps/backend/src/test/resources').rglob('*') if path.is_file()}
encoded = re.compile(rb'\$pbkdf2-sha256\$v1\$[0-9]{5,7}\$[A-Za-z0-9+/]{43}=\$[A-Za-z0-9+/]{43}=')
raw_token = re.compile(rb'(?:Bearer |(?:__Secure-pet_staff_sid|pet_dev_staff_sid)=)[A-Za-z0-9_-]{16,256}')
with zipfile.ZipFile(jar) as archive:
    entries = archive.namelist()
    violations = [name for name in entries if name.removeprefix('BOOT-INF/classes/') in test_classes | test_resources
                  or 'com/pet/testing/' in name or 'testcontainers-' in name
                  or '/security-migrations/' in name or '/persistence-migrations/' in name]
    credential_constants = []
    fixture_strings = []
    for name in entries:
        if not name.startswith('BOOT-INF/classes/') or name.endswith('/'):
            continue
        content = archive.read(name)
        if encoded.search(content):
            credential_constants.append(name)
        if any(value in content for value in (b'/__p05-browser', b'/__authentication-test', b'/__protocol-test',
                b'P05BrowserHarness', b'TEST_PASSWORD', b'OnlyContainer', b'DO-NOT-OUTPUT', b'p05_probe_runtime')):
            fixture_strings.append(name)
    sql = [name for name in entries if name.startswith('BOOT-INF/classes/db/migration/') and name.endswith('.sql')]
    entities = [name for name in entries if name.startswith('BOOT-INF/classes/') and name.endswith('.class')
                and b'Ljakarta/persistence/Entity;' in archive.read(name)]
    provider = 'BOOT-INF/classes/com/pet/platform/identity/infrastructure/session/StaffAuthenticationFilter.class' in entries
    formal_v1 = 'BOOT-INF/classes/db/migration/V1__platform_identity_foundation.sql'
    migration_matches = formal_v1 in entries and archive.read(formal_v1) == (root / 'apps/backend/src/main/resources/db/migration/V1__platform_identity_foundation.sql').read_bytes()
    sa_modules = [name for name in entries if name.startswith('BOOT-INF/lib/sa-token')]
artifact = {'path': str(jar), 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
            'testIsolationViolations': violations, 'encodedCredentialConstants': credential_constants,
            'fixtureStrings': fixture_strings, 'productionSql': sql, 'productionEntities': entities,
            'testClassesCompared': len(test_classes), 'testResourcesCompared': len(test_resources),
            'expectedProductionProviderIncluded': provider, 'formalMigrationMatches': migration_matches,
            'saTokenModules': sa_modules}
write('artifact-audit.json', artifact)

files = sorted(set(subprocess.check_output(['git', 'ls-files', '--modified', '--others', '--exclude-standard'], cwd=root, text=True).splitlines()))
categories = {'production': [], 'tests': [], 'generated': [], 'documents': [], 'evidence': []}
for name in files:
    category = ('evidence' if name.startswith('docs/testing/evidence/P05-02/') else
                'generated' if 'generated/' in name or 'api-contracts/openapi/' in name else
                'tests' if 'apps/backend/src/test/' in name else
                'production' if name.startswith('apps/backend/') else 'documents')
    categories[category].append(name)
write('file-manifest.json', categories)

# 命令包装器退出时写入完整结果；这里只建立本检查的待完成记录供链接存在性检查。
if not (ev / 'final-audit-command.json').exists():
    write('final-audit-command.json', {'state': 'RUNNING', 'cwd': str(root), 'argv': ['python3', 'docs/testing/evidence/P05-02/final-audit.py']})
write('final-audit.json', {'state': 'RUNNING'})
links = []
for name in files:
    path = root / name
    if path.suffix != '.md':
        continue
    for link in re.findall(r'\]\(([^)]+)\)', path.read_text()):
        if re.match(r'^[a-z]+:', link) or link.startswith('#'):
            continue
        local = link.split('#')[0].strip('<>')
        if local and not (path.parent / local).exists():
            links.append({'file': name, 'target': local})

protected = [name for name in subprocess.check_output(['git', 'ls-files'], cwd=root, text=True).splitlines()
             if name.startswith(('ui/', 'docs/testing/evidence/P05-01/', 'docs/testing/evidence/P04-',
                                 'apps/admin-web/', 'apps/wechat-miniprogram/miniprogram/pages/', '.github/'))
             or name in ('AGENTS.md', 'pnpm-lock.yaml', 'package.json', 'docs/testing/P05-01-VERIFICATION.md')]
preservation = [name for name in protected if not (root / name).exists()
                or subprocess.check_output(['git', 'show', 'HEAD:' + name], cwd=root) != (root / name).read_bytes()]
log_hits = []
for path in list(ev.glob('*.log')) + list((ev / 'verify-final-reports').rglob('*.xml')) + list((ev / 'verify-final-reports').glob('*.log')):
    content = path.read_bytes()
    if encoded.search(content) or raw_token.search(content) or b'OnlyContainer' in content or b'DO-NOT-OUTPUT' in content:
        log_hits.append(str(path.relative_to(ev)))
diff = subprocess.run(['git', 'diff', '--check'], cwd=root, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
containers = subprocess.check_output(['docker', 'ps', '--filter', 'label=org.testcontainers=true', '--format', '{{.ID}} {{.Image}}'], text=True).splitlines()
required_commands = ['verify-final', 'contracts-generate', 'contracts-check', 'contracts-typecheck', 'web-typecheck', 'miniprogram-typecheck', 'repo-check', 'dependencies', 'browser-harness', 'container-cleanup-check', 'contracts-final-snapshot']
command_failures = [name for name in required_commands if json.loads((ev / (name + '.json')).read_text()).get('exitCode') != 0]
browser = json.loads((ev / 'browser-result.json').read_text())
ok = len(cases) == 292 and len(baseline) == 258 and not missing_cases and len(added_cases) == 34 \
    and not violations and not credential_constants and not fixture_strings and provider and migration_matches \
    and len(sql) == 1 and len(entities) == 7 and len(sa_modules) == 7 and all('1.46.0.jar' in name for name in sa_modules) \
    and not preservation and not links and not log_hits and not command_failures and not containers \
    and diff.returncode == 0 and browser['ok'] and (ev / 'browser-cookie-csrf.jpg').exists() \
    and all(row[key] == 0 for row in stats.values() for key in ('failures', 'errors', 'skipped'))
summary = {'cwd': str(root), 'argv': ['python3', 'docs/testing/evidence/P05-02/final-audit.py'],
           'exitCode': 0 if ok else 1, 'createdAtUTC': datetime.datetime.now(datetime.timezone.utc).isoformat(),
           'testCount': len(cases), 'baselineCount': len(baseline), 'baselineRetained': len(cases & baseline),
           'addedCount': len(added_cases), 'missingBaselineCases': missing_cases, 'groups': stats,
           'artifact': artifact, 'logCredentialHits': log_hits, 'preservationChecked': len(protected),
           'preservationViolations': preservation, 'missingDocumentLinks': links,
           'requiredCommandFailures': command_failures, 'diffCheckExitCode': diff.returncode,
           'runningTestcontainers': containers, 'browserResult': browser}
write('final-audit.json', summary)
commands = []
for path in sorted(ev.glob('*.json')):
    data = json.loads(path.read_text())
    if isinstance(data, dict) and 'exitCode' in data and ('argv' in data or 'command' in data):
        commands.append({'file': path.name, **{key: data[key] for key in ('cwd', 'argv', 'command', 'exitCode', 'startedAtUTC', 'finishedAtUTC', 'log') if key in data}})
write('COMMAND-INDEX.json', commands)
print(json.dumps({key: value for key, value in summary.items() if key != 'artifact'}, ensure_ascii=False, indent=2))
raise SystemExit(0 if ok else 1)
