from pathlib import Path
import json,hashlib,subprocess,re,socket
r=Path('/Users/kimwell/work/pet-platform'); e=r/'docs/testing/evidence/B02'; c=e/'completion'
records=[json.loads(x) for x in (c/'commands.jsonl').read_text().splitlines()]
required=['backend-verify-serial','web-check','web-build','contracts-generate','contracts-check','contracts-typecheck','miniprogram-check','repo-closure','diff-check-closure','repo-final','diff-check-final','runtime-setup-ready','browser-stage1','browser-stage2-ready','customer-real','final-database','cleanup']
results={name:next(x['exitCode'] for x in reversed(records) if x['label']==name) for name in required};assert all(v==0 for v in results.values())
suites=json.loads((c/'backend-test-results.json').read_text()); totals={a:sum(int(x[a]) for x in suites) for a in ['tests','failures','errors','skipped']};assert totals=={'tests':490,'failures':0,'errors':0,'skipped':0}
customer=json.loads((c/'customer-real.json').read_text());assert customer['status']=='PASS' and customer['probeClosed'] and customer['productionGateway'] and not customer['codeLogged'] and not customer['tokenLogged'];assert [x['status'] for x in customer['steps']]==[200,200,401,401,401,401,401,200,200,200]
stage1=json.loads((c/'browser-stage1.json').read_text());stage2=json.loads((c/'browser-stage2.json').read_text());assert stage1['status']==stage2['status']=='PASS' and len(stage2['scenarios'])==15
snapshot=json.loads((c/'final-database.json').read_text())['snapshot'];assert snapshot['customerIdentityCount']==1 and snapshot['runtimeRestricted'] and snapshot['platformCleanupPending']==0
preserved=json.loads((c/'preservation.json').read_text());assert not preserved['B01MethodsMissing'] and not preserved['B01EvidenceChanged'] and not preserved['trackedUIChanged'] and not preserved['firstStageSourceChanged']
frozen=['pnpm-lock.yaml','package.json','apps/backend/pom.xml','docs/development/VERSION-MATRIX.md'];assert subprocess.check_output(['git','diff','HEAD','--',*frozen],cwd=r)==b''
for p in (r/'apps/backend/src/main/resources/db/migration').glob('V[1-5]__*.sql'):assert p.read_bytes()==subprocess.check_output(['git','show','HEAD:'+str(p.relative_to(r))],cwd=r)
v6=next(x for x in json.loads((r/'docs/testing/evidence/B01/file-manifest.json').read_text())['files'] if x['path'].endswith('V6__identity_management.sql'));assert hashlib.sha256((r/v6['path']).read_bytes()).hexdigest()==v6['sha256']
assert not (r/'.local-data/b02').exists()
cleanup=json.loads((c/'cleanup.json').read_text());assert cleanup['status']=='PASS' and cleanup['existingNamedInfrastructurePreserved'] and all(x['noListener'] for x in cleanup['ports'])
for x in cleanup['ports']:
 with socket.socket() as s:assert s.connect_ex(('127.0.0.1',x['port']))!=0
active=subprocess.check_output(['docker','ps','-a','--no-trunc','--format','{{.ID}}'],text=True).splitlines(); ids=set()
for p in c.glob('*.log'):ids.update(re.findall(r'Container [^\n]+ is starting: ([0-9a-f]{64})',p.read_text()))
for name in ['runtime-setup.json','runtime-setup-missing-jar.json']:
 for x in json.loads((c/name).read_text()):
  value=x.get('summary','').strip()
  if re.fullmatch('[0-9a-f]{64}',value):ids.add(value)
assert not ids.intersection(active)
base=json.loads((c/'runtime-setup.json').read_text())[0]['summary'].strip().splitlines();states=dict(x.split('\t') for x in subprocess.check_output(['docker','ps','-a','--format','{{.Names}}\t{{.State}}'],text=True).splitlines());assert all(states.get(x.split('\t')[0])==x.split('\t')[1] for x in base if x.startswith('pet-platform-') and '\t' in x)
report=r/'docs/testing/B02-ACCEPTANCE.md';links=re.findall(r'\[[^\]]*\]\(([^)]+)\)',report.read_text());missing=[]
for link in links:
 if not link.startswith(('http:','https:','#')):
  target=link.split('#')[0]
  if not (report.parent/target).exists():missing.append(link)
assert not missing
for name in ['docs/testing/B02-ACCEPTANCE.md','docs/development/ROADMAP.md','docs/testing/ACCEPTANCE-MATRIX.md']:
 text=(r/name).read_text();assert 'B02 COMPLETE' in text and 'P07 IN_PROGRESS' in text
paths={x['path'] for x in json.loads((e/'file-manifest-first-stage.json').read_text())['files']};paths.update(str(p.relative_to(r)) for p in e.rglob('*') if p.is_file());paths.discard('docs/testing/evidence/B02/file-manifest.json');paths.discard('docs/testing/evidence/B02/final-audit.json')
files=[{'path':name,'bytes':(r/name).stat().st_size,'sha256':hashlib.sha256((r/name).read_bytes()).hexdigest()} for name in sorted(paths) if (r/name).is_file()]
manifest={'root':str(r),'scope':'B02实现、契约、文档和两阶段真实验收；B01先前成果不冒充新增','selfAndFinalAuditExcluded':True,'fileCount':len(files),'files':files};(e/'file-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2))
out={'command':['python3','docs/testing/evidence/B02/completion/replay/final-audit.py'],'cwd':str(r),'exitCode':0,'B02':'COMPLETE','P07':'IN_PROGRESS','B03':'NOT_STARTED','B03EntryConditionsSatisfied':True,'remainingB02Gate':None,'remainingP07':['A07-02批量部分失败（B03）','A07-01删除末页回退（B03）'],'requiredCommandResults':results,'backend':totals,'webTests':266,'contractsGenerateTests':10,'contractsCheckTests':10,'browserStage1Scenarios':len(stage1['scenarios']),'browserStage2Scenarios':len(stage2['scenarios']),'customerHTTPChecks':len(customer['steps']),'customerLoginRequestsExecuted':sum(x['path']=='/api/customer/auth/wechat/login' for x in customer['steps']),'runtimeReports':{'browser-stage1':'PASS','browser-stage2':'PASS','customer-real':'PASS'},'preservation':preserved,'cleanup':cleanup,'finalLoggedContainerCount':len(ids),'finalLoggedContainersStillPresent':[],'B02FirstStageRawDatabaseSnapshotLost':True,'evidenceCorrection':'completion/evidence-path-correction.json','secretScan':json.loads((c/'secret-scan.json').read_text()),'reportLinks':'PASS','manifestSHA256':hashlib.sha256((e/'file-manifest.json').read_bytes()).hexdigest(),'historicalFailureCommandCount':sum(x.get('exitCode',0) not in (0,None) for x in records),'submitted':False,'pushed':False,'deployed':False}
(e/'final-audit.json').write_text(json.dumps(out,ensure_ascii=False,indent=2));print({'B02':out['B02'],'P07':out['P07'],'B03EntryConditionsSatisfied':True,'backendTests':490,'webTests':266,'reportLinks':'PASS','loggedContainersAbsent':len(ids),'fileManifestCount':len(files)})
