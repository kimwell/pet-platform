from pathlib import Path
import datetime
import hashlib
import json
import re
import subprocess

root = Path(__file__).resolve().parents[4]
ev = root / 'docs/testing/evidence/P04-01'
baseline = json.loads((ev / 'baseline-files.json').read_text())
docs = ['README.md', 'docs/architecture/MULTI-TENANCY.md', 'docs/architecture/AUTHORIZATION.md',
        'docs/architecture/MODULE-BOUNDARIES.md', 'docs/contracts/IDENTITY.md', 'docs/conventions/BACKEND.md',
        'docs/conventions/PERSISTENCE.md', 'docs/development/ROADMAP.md', 'docs/development/DECISION-LOG.md',
        'docs/testing/ACCEPTANCE-MATRIX.md', 'docs/testing/P04-01-VERIFICATION.md']
source_roots = ['apps/backend/src/main/java/com/pet/platform/shared/security',
                'apps/backend/src/main/java/com/pet/platform/shared/tenancy',
                'apps/backend/src/test/java/com/pet/testing/tenancy']
owned = docs + ['apps/backend/src/main/java/com/pet/platform/shared/exception/TenantAccessDeniedException.java',
                'apps/backend/src/test/java/com/pet/testing/architecture/StructureRulesTest.java']
for directory in source_roots:
    owned += [str(p.relative_to(root)) for p in (root / directory).rglob('*.java')]
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
manifest = [{'path': name, 'change': 'modified' if name in baseline else 'added', 'sha256': sha(root / name)} for name in sorted(owned)]
protected = [name for name in baseline if name.startswith(('ui/', 'apps/admin-web/', 'apps/wechat-miniprogram/', 'packages/'))]
protected += ['apps/backend/pom.xml', 'package.json', 'pnpm-lock.yaml', 'AGENTS.md']
violations = [name for name in protected if not (root / name).is_file() or sha(root / name) != baseline[name]]
missing_links = []
for name in docs:
    p = root / name
    for target in re.findall(r'\]\(([^)]+)\)', p.read_text()):
        target = target.strip('<>').split('#', 1)[0]
        if not target or re.match(r'^[a-z][a-z0-9+.-]*:', target):
            continue
        if not (p.parent / target).resolve().exists():
            missing_links.append([name, target])
tests = json.loads((ev / 'test-results.json').read_text())
artifact = json.loads((ev / 'artifact-audit.json').read_text())
diff_check = subprocess.run(['git', 'diff', '--check'], cwd=root, capture_output=True, text=True)
artifact_unchanged = sha(Path(artifact['artifact'])) == artifact['sha256']
record = {'cwd': str(root), 'argv': ['python3', 'docs/testing/evidence/P04-01/final-audit.py'],
          'finishedAtUTC': datetime.datetime.now(datetime.timezone.utc).isoformat(),
          'ownedFiles': manifest, 'protectedFilesCount': len(protected), 'protectedFileViolations': violations,
          'missingLinks': missing_links, 'gitDiffCheckExitCode': diff_check.returncode,
          'testCount': tests['total'], 'baselineCases': tests['oldBaselineCases'], 'missingBaselineCases': tests['missingBaselineCases'],
          'testTotals': {g: v['totals'] for g, v in tests['groups'].items()},
          'artifactSHA256': artifact['sha256'], 'artifactUnchangedSinceVerify': artifact_unchanged,
          'productionArtifactViolations': artifact['violations'],
          'externalWorkPreserved': ['.github/workflows/check.yml', 'docs/development/VERSION-MATRIX.md', 'docs/testing/evidence/CI-JAVA-SETUP-2026-10-08/', 'docs/testing/CI-JAVA-SETUP-VERIFICATION.md']}
ok = not violations and not missing_links and diff_check.returncode == 0 and artifact_unchanged and not artifact['violations'] and not tests['missingBaselineCases']
record['exitCode'] = 0 if ok else 1
(ev / 'final-audit.json').write_text(json.dumps(record, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({k: record[k] for k in ['exitCode', 'testCount', 'protectedFilesCount', 'protectedFileViolations', 'missingLinks', 'artifactUnchangedSinceVerify']}, ensure_ascii=False))
raise SystemExit(record['exitCode'])
