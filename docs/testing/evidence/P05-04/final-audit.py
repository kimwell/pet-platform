"""仅审计本轮快照/产物/保护文件；不操作用户数据库或容器。"""
from pathlib import Path
import json,hashlib,re,subprocess,xml.etree.ElementTree as ET,zipfile,datetime
root=Path(__file__).resolve().parents[4];ev=Path(__file__).resolve().parent
write=lambda name,data:(ev/name).write_text(json.dumps(data,ensure_ascii=False,indent=2))
def cases_at(path):
 cases=set();stats={};suites=[]
 for group in ('surefire','failsafe'):
  total={k:0 for k in ('tests','failures','errors','skipped')}
  for p in sorted((path/(group+'-reports')).glob('TEST-*.xml')):
   r=ET.parse(p).getroot();row={'name':r.attrib['name'],**{k:int(r.attrib[k]) for k in total}};suites.append(row)
   for k in total:total[k]+=row[k]
   cases.update((c.attrib['classname'],c.attrib['name']) for c in r.findall('testcase'))
  stats[group]=total
 return cases,stats,suites
cases,stats,suites=cases_at(ev/'verify-final-reports');baseline,bstat,_=cases_at(root/'docs/testing/evidence/P05-03/verify-final-reports')
missing=sorted(baseline-cases);added=sorted(cases-baseline)
write('test-summary.json',dict(cases=len(cases),baseline=len(baseline),groups=stats,suites=suites,addedCases=added,missingBaselineCases=missing))
jar=Path('/tmp/pet-p05-04-final.jar');encoded=re.compile(rb'\$pbkdf2-sha256\$v1\$[0-9]{5,7}\$[A-Za-z0-9+/]{43}=\$[A-Za-z0-9+/]{43}=');token=re.compile(rb'(?:Bearer |(?:__Secure-pet_(?:staff|platform)_sid|pet_dev_(?:staff|platform)_sid)=)[A-Za-z0-9_-]{16,256}')
with zipfile.ZipFile(jar) as z:
 entries=z.namelist();violations=[n for n in entries if 'com/pet/testing/' in n or 'testcontainers-' in n or '/security-migrations/' in n or '/persistence-migrations/' in n]
 credentials=[];fixtures=[];unresolved=[]
 for n in entries:
  if not n.startswith('BOOT-INF/classes/') or n.endswith('/'):continue
  c=z.read(n)
  if encoded.search(c):credentials.append(n)
  if any(v in c for v in (b'/__p05-platform-browser',b'/__platform-test',b'P05PlatformBrowserHarness',b'TEST_PASSWORD',b'OnlyContainer',b'DO-NOT-OUTPUT',b'PlatformAuthenticationIT',b'PlatformBootstrapRuntimeIT')):fixtures.append(n)
  if b'Unresolved compilation problem' in c:unresolved.append(n)
 sql=[n for n in entries if n.startswith('BOOT-INF/classes/db/migration/') and n.endswith('.sql')]
 matches={Path(n).name:z.read(n)==(root/'apps/backend/src/main/resources/db/migration'/Path(n).name).read_bytes() for n in sql}
 entities=[n for n in entries if n.startswith('BOOT-INF/classes/') and n.endswith('.class') and b'Ljakarta/persistence/Entity;' in z.read(n)]
 modules=[n for n in entries if n.startswith('BOOT-INF/lib/sa-token')]
artifact=dict(path=str(jar),sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),testIsolationViolations=violations,credentialConstants=credentials,fixtureStrings=fixtures,unresolvedCompileStubs=unresolved,productionSql=sql,migrationMatches=matches,productionEntities=entities,saTokenModules=modules)
write('artifact-audit.json',artifact)
base=json.loads((ev/'baseline-files.json').read_text());changed=[];deleted=[]
for n,h in base.items():
 p=root/n
 if not p.exists():deleted.append(n)
 elif hashlib.sha256(p.read_bytes()).hexdigest()!=h:changed.append(n)
new=[]
for p in root.rglob('*'):
 n=str(p.relative_to(root))
 if set(p.relative_to(root).parts)&{'.git','node_modules','target'} or not p.is_file() or n in base:continue
 new.append(n)
files=sorted(set(changed+new));categories={k:[] for k in ('production','tests','generated','documents','evidence','typeOnlyConsumers')}
for n in files:
 c=('evidence' if n.startswith('docs/testing/evidence/P05-04/') else 'generated' if 'generated/' in n or 'api-contracts/openapi/' in n else 'tests' if '/src/test/' in n or 'api-contracts/test/' in n else 'typeOnlyConsumers' if n.endswith('contracts.ts') or n=='packages/api-contracts/src/index.ts' else 'production' if n.startswith('apps/backend/src/main/') or n.endswith('.sql') or n=='scripts/backend-identity.sh' else 'documents');categories[c].append(n)
write('file-manifest.json',dict(categories=categories,changedFromStartingWorkspace=changed,newFiles=new,deletedFiles=deleted))
protected=[]
for n in base:
 if n.startswith(('ui/','.local-data/','docs/testing/evidence/','apps/admin-web/','apps/wechat-miniprogram/')) and not n.startswith('docs/testing/evidence/P05-04/') and n not in ('apps/admin-web/src/contracts.ts','apps/wechat-miniprogram/miniprogram/types/contracts.ts','apps/wechat-miniprogram/miniprogram/types/generated/api.d.ts'):protected.append(n)
 if n in ('AGENTS.md','pnpm-lock.yaml','package.json','apps/backend/pom.xml','docs/development/VERSION-MATRIX.md','infra/database/provision-identity-roles.sql') or re.match(r'docs/testing/P0[1-5](?:-0[1-3])?-(?:VERIFICATION|ACCEPTANCE)\.md$',n) or re.match(r'apps/backend/src/main/resources/db/migration/V[12]__',n):protected.append(n)
preservation=sorted(set(protected)&set(changed+deleted))
links=[]
for n in files:
 p=root/n
 if p.suffix!='.md' or n.startswith('docs/testing/evidence/'):continue
 for link in re.findall(r'\]\(([^)]+)\)',p.read_text()):
  if re.match(r'^[a-z]+:',link) or link.startswith('#'):continue
  local=link.split('#')[0].strip('<>')
  if local and not (p.parent/local).exists():links.append(dict(file=n,target=local))
loghits=[]
for p in list(ev.glob('*.log'))+list((ev/'verify-final-reports').rglob('*.xml'))+list((ev/'verify-final-reports').rglob('*.json')):
 c=p.read_bytes()
 if encoded.search(c) or token.search(c) or b'OnlyContainer' in c or b'DO-NOT-OUTPUT' in c:loghits.append(str(p.relative_to(ev)))
required=['verify-third','encoded-path-fixed','contracts-generate-final','contracts-check-final','contracts-typecheck','web-typecheck','miniprogram-typecheck','repo-check','contracts-final-snapshot','java-version','node-version','pnpm-version']
failed=[n for n in required if not (ev/(n+'.json')).exists() or json.loads((ev/(n+'.json')).read_text()).get('exitCode')!=0]
diff=subprocess.run(['git','diff','--check'],cwd=root,capture_output=True,text=True)
containers=subprocess.check_output(['docker','ps','--filter','label=org.testcontainers=true','--format','{{.ID}} {{.Image}}'],text=True).splitlines()
schema=json.loads((root/'packages/api-contracts/openapi/backend.openapi.json').read_text());s=schema['components']['schemas'];p=s['PlatformCurrentIdentity'];staff=s['CurrentIdentity']
api={'paths':len(schema['paths']),'platform':p,'staffTenant':staff['properties']['tenantId'],'staffScope':staff['properties']['dataScope'],'cookies':schema['components']['securitySchemes']}
api_ok=len(schema['paths'])==15 and p['properties']['tenantId']['type']=='null' and p['properties']['dataScope']['type']=='null' and p['properties']['principalType']['enum']==['PLATFORM'] and staff['properties']['principalType']['enum']==['STAFF'] and staff['properties']['tenantId']['type']=='string' and 'tenantId' in staff['required']
write('openapi-audit.json',api)
browser=json.loads((ev/'browser-result.json').read_text());browser_ok=browser.get('ok') and json.loads((ev/'browser-harness.json').read_text())['exitCode']==0 and (ev/'browser-result.jpg').exists()
ok=len(cases)==366 and len(baseline)==328 and not missing and len(added)==38 and not violations and not credentials and not fixtures and not unresolved and len(sql)==3 and all(matches.values()) and len(entities)==8 and len(modules)==7 and all('1.46.0.jar' in n for n in modules) and not preservation and not deleted and not links and not loghits and not failed and not containers and diff.returncode==0 and api_ok and browser_ok and all(r[k]==0 for r in stats.values() for k in ('failures','errors','skipped'))
summary=dict(cwd=str(root),argv=['python3','docs/testing/evidence/P05-04/final-audit.py'],exitCode=0 if ok else 1,createdAtUTC=datetime.datetime.now(datetime.timezone.utc).isoformat(),testCount=len(cases),baselineCount=len(baseline),missingBaselineCases=missing,addedCases=len(added),groups=stats,logCredentialHits=loghits,preservationChecked=len(set(protected)),preservationViolations=preservation,deletedFiles=deleted,missingDocumentLinks=links,requiredCommandFailures=failed,diffCheckExitCode=diff.returncode,diffCheckSummary=diff.stdout[-1000:],runningTestcontainers=containers,openapiChecks=api_ok,browserChecks=browser_ok)
write('final-audit.json',summary);print(json.dumps(summary,ensure_ascii=False,indent=2));raise SystemExit(0 if ok else 1)
