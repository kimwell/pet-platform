"""文档/范围/产物/资源事实核对；不代替运行验收。"""
import hashlib,json,re,subprocess,urllib.parse
from pathlib import Path
root=Path(__file__).resolve().parents[4]
out=Path(__file__).resolve().parent
(out/'final-audit.json').write_text(json.dumps({'status':'IN_PROGRESS'})+'\n')
baseline=json.loads((out/'baseline-files.json').read_text())
allowed_docs={
 'README.md','infra/local/README.md','docs/architecture/TECHNICAL-BASELINE.md','docs/architecture/PROJECT-STRUCTURE.md',
 'docs/architecture/CONFIGURATION.md','docs/contracts/PAGINATION.md','docs/conventions/BACKEND.md',
 'docs/conventions/DATABASE-MIGRATION.md','docs/conventions/PERSISTENCE.md','docs/development/LOCAL-DEVELOPMENT.md',
 'docs/development/VERSION-MATRIX.md','docs/development/DECISION-LOG.md','docs/development/ROADMAP.md',
 'docs/testing/ACCEPTANCE-MATRIX.md','docs/testing/P03-02-VERIFICATION.md'}
changed=[];deleted=[];unexpected=[]
for rel,sha in baseline.items():
 p=root/rel
 if not p.exists():deleted.append(rel);continue
 if hashlib.sha256(p.read_bytes()).hexdigest()!=sha:
  changed.append(rel)
  if not (rel in allowed_docs or rel.startswith('apps/backend/') or rel=='.github/workflows/check.yml'):
   unexpected.append(rel)
new=[]
for p in root.rglob('*'):
 if not p.is_file() or any(x in p.parts for x in ['.git','node_modules','target','miniprogram_npm','.local-data']):continue
 if p.name in ['.env','project.private.config.json']:continue
 rel=str(p.relative_to(root))
 if rel not in baseline:
  if p.suffix=='.log' and not rel.startswith('docs/testing/evidence/P03-02/'):continue
  new.append(rel)
  if not (rel in allowed_docs or rel.startswith(('apps/backend/','docs/testing/evidence/P03-02/'))):unexpected.append(rel)
resource_baseline=json.loads((out/'resource-baseline.json').read_text())
volumes=subprocess.check_output(['docker','volume','ls','-q'],text=True).splitlines()
missing_volumes=sorted(set(resource_baseline['volumes'])-set(volumes))
extra_volumes=sorted(set(volumes)-set(resource_baseline['volumes']))
containers=[]
for line in resource_baseline['containers']:
 cid,name,*status=line.split()
 result=subprocess.run(['docker','inspect','--format','{{.State.Status}} {{.State.ExitCode}} {{.State.OOMKilled}}',cid],capture_output=True,text=True)
 assert result.returncode==0
 assert result.stdout.strip()=='exited 0 false',result.stdout
 containers.append(dict(id=cid,name=name,state=result.stdout.strip()))
active=subprocess.check_output(['docker','ps','--format','{{.Names}}'],text=True).splitlines()
assert not any('pet-platform-p03-02' in n or 'testcontainers' in n for n in active)
for record in json.loads((out/'local-runtime.json').read_text()):
 if 'pid' in record:
  result=subprocess.run(['ps','-p',str(record['pid']),'-o','pid='],capture_output=True,text=True)
  assert not result.stdout.strip(),record['pid']
artifact=json.loads((out/'artifact-audit.json').read_text())
assert hashlib.sha256(Path(artifact['jar']).read_bytes()).hexdigest()==artifact['sha256']
assert artifact['testArtifactsAbsent']
assert json.loads((out/'verify-final.json').read_text())['exitCode']==0
assert json.loads((out/'runtime-third.json').read_text())['exitCode']==0
assert json.loads((out/'test-results.json').read_text())['total']==dict(tests=62,failures=0,errors=0,skipped=0)
links=[]
for rel in sorted(allowed_docs):
 p=root/rel
 if not p.exists():links.append(dict(file=rel,target='文件缺失'));continue
 for link in re.findall(r'\[[^\]]*\]\(([^)]+)\)',p.read_text()):
  path=link.split('#')[0]
  if not path or ':' in path:continue
  target=(p.parent/urllib.parse.unquote(path)).resolve()
  if not target.exists():links.append(dict(file=rel,target=link))
result=dict(changedFiles=sorted(changed),newFiles=sorted(new),deletedFiles=deleted,unexpectedChanges=sorted(unexpected),
 protectedPaths=['apps/admin-web','apps/wechat-miniprogram','ui','pnpm-lock.yaml','packages','AGENTS.md','P01/P02/P03-01历史文档与证据'],
 missingBaselineVolumes=missing_volumes,extraVolumes=extra_volumes,baselineContainers=containers,taskResourcesCleaned=True,
 missingLinks=links,artifactSha256=artifact['sha256'],remoteCI='NOT_EXECUTED',multiOS='NOT_EXECUTED')
(out/'final-audit.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
assert not deleted and not unexpected and not links and not missing_volumes and not extra_volumes,result
print(json.dumps({k:v for k,v in result.items() if k not in ['newFiles','changedFiles']},ensure_ascii=False,indent=2))
