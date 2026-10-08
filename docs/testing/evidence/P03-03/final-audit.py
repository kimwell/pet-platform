from pathlib import Path
import json,hashlib,re,subprocess
root=Path(__file__).resolve().parents[4];ev=Path(__file__).resolve().parent
baseline=json.loads((ev/'baseline-files.json').read_text())
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
modified=[];removed=[]
for name,digest in baseline.items():
 path=root/name
 if not path.exists():removed.append(name)
 elif sha(path)!=digest:modified.append(name)
assert removed==['apps/backend/src/main/resources/application-test.yml'],removed
assert sha(root/'apps/backend/src/test/resources/application-test.yml')==baseline[removed[0]]
assert all(not p.startswith('ui/') for p in modified)
assert 'AGENTS.md' not in modified
assert not any(p.startswith('docs/testing/evidence/P03-01/') or p.startswith('docs/testing/evidence/P03-02/') or re.match(r'docs/testing/P0[12]',p) for p in modified)
skip={'.git','node_modules','target','dist','miniprogram_npm','.cache','.local-data'}
new=[]
for path in root.rglob('*'):
 if path.is_file() and not any(part in skip for part in path.relative_to(root).parts):
  name=str(path.relative_to(root))
  if name not in baseline and not name.startswith('docs/testing/evidence/P03-03/'):new.append(name)
expected_commands={'verify-second':0,'generate-second':0,'check-first':0,'drift-negative':1,'api-types':0,'web-typecheck':0,'mini-typecheck':0,'generator-tests-final':0,'frontend-check':0,'web-build':0,'scripts-lint':0,'frozen-install':0}
for name,expected in expected_commands.items():
 record=json.loads((ev/(name+'.json')).read_text());assert record['exitCode']==expected,(name,record['exitCode'])
results=json.loads((ev/'test-results.json').read_text());assert results['total']=={'tests':79,'failures':0,'errors':0,'skipped':0}
proof=json.loads((ev/'drift-negative-proof.json').read_text());assert proof['snapshotNotOverwritten']
artifacts=['packages/api-contracts/openapi/backend.openapi.json','packages/api-contracts/openapi/test-contract.openapi.json','packages/api-contracts/src/generated/api.d.ts','packages/api-contracts/test/generated/test-contract.d.ts','apps/wechat-miniprogram/miniprogram/types/generated/api.d.ts']
for name in artifacts:
 text=(root/name).read_text()
 assert 'http://127.0.0.1:' not in text and str(root) not in text,name
 assert 'TECHNICAL_SECRET_RAW_INPUT' not in text,name
backend=json.loads((root/artifacts[0]).read_text());assert backend['paths']=={}
assert backend['components']['schemas']['PageResponseFieldErrorDetail']['properties']['total']['type']=='string'
assert 'ScalarInput' not in backend['components']['schemas']
assert (root/artifacts[-1]).read_text().splitlines()[0]=='// 自动生成：请勿手改；来源SHA256='+sha(root/artifacts[2])
assert len(list((root/'apps/wechat-miniprogram/miniprogram/types/generated').iterdir()))==1
for name in ('apps/admin-web/src/contracts.ts','apps/wechat-miniprogram/miniprogram/types/contracts.ts'):
 text=(root/name).read_text();assert 'import type' in text;assert not re.search(r'^(?:import|export) (?!type)',text,re.M)
missing=[]
for name in modified+new:
 if not name.endswith('.md'):continue
 path=root/name
 for target in re.findall(r'\]\(([^)]+)\)',path.read_text()):
  target=target.strip('<>');target=target.split('#')[0]
  if not target or '://' in target:continue
  if not (path.parent/target).exists():missing.append({'file':name,'target':target})
# 本结果链接以输出文件为目标，先写再验证自身引用。
missing=[p for p in missing if p['target']!='evidence/P03-03/final-audit.json']
assert not missing,missing
jar=root/'apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar';artifact=json.loads((ev/'artifact-audit.json').read_text());assert sha(jar)==artifact['sha256'];assert artifact['testEntries']==[]
git=subprocess.run(['git','status','--short'],cwd=root,capture_output=True,text=True)
(ev/'git-final.txt').write_text(git.stdout);assert git.returncode==0
result={'status':'PASS','modified':sorted(modified),'removed':removed,'added':sorted(new),'preserved':{'ui':True,'AGENTS':True,'P03-01Evidence':True,'P03-02Evidence':True,'priorUnmodifiedFiles':len(baseline)-len(modified)-len(removed)},'commands':expected_commands,'backendTests':results['total'],'artifacts':{p:sha(root/p) for p in artifacts},'jarSHA256':sha(jar),'missingLocalLinks':missing,'limits':'文档/范围/产物审计不替代HTTP、数据库或业务运行证据'}
(ev/'final-audit.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
records={}
for path in sorted(ev.glob('*.json')):
 try:
  record=json.loads(path.read_text())
  if 'argv' in record:records[path.name]={'cwd':record['cwd'],'argv':record['argv'],'exitCode':record['exitCode'],'log':record['log']}
 except json.JSONDecodeError:pass
(ev/'COMMAND-INDEX.json').write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'status':'PASS','modified':len(modified),'added':len(new),'tests':results['total'],'preservedUi':True,'missingLinks':missing},ensure_ascii=False,indent=2))
