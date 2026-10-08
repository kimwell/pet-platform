"""本轮只读产物/证据审计，不触碰运行库或删除容器。"""
from pathlib import Path
import datetime, hashlib, json, re, subprocess, xml.etree.ElementTree as ET, zipfile
root=Path(__file__).resolve().parents[4];ev=Path(__file__).resolve().parent;target=root/'apps/backend/target'
def write(name,data): (ev/name).write_text(json.dumps(data,ensure_ascii=False,indent=2))
def cases_at(directory):
    cases=set();stats={};suites=[]
    for group in ('surefire','failsafe'):
        total={key:0 for key in ('tests','failures','errors','skipped')}
        for path in sorted((directory/(group+'-reports')).glob('TEST-*.xml')):
            suite=ET.parse(path).getroot();row={'name':suite.attrib['name'],**{k:int(suite.attrib[k]) for k in total}};suites.append(row)
            for k in total:total[k]+=row[k]
            cases.update((c.attrib['classname'],c.attrib['name']) for c in suite.findall('testcase'))
        stats[group]=total
    return cases,stats,suites
cases,stats,suites=cases_at(ev/'verify-final-reports');baseline,baseline_stats,_=cases_at(root/'docs/testing/evidence/P05-02/verify-final-reports')
# P05-02禁止真实身份异步改为本轮执行前重验；只有此项语义替换。
replacement=('com.pet.testing.authentication.StaffAuthenticationIT','realUserAsyncSnapshotsAreRevalidatedBeforeExecution')
old=[c for c in baseline-cases if c[0]==replacement[0] and 'Async' in c[1]]
missing=sorted(baseline-cases);unexplained=[c for c in missing if c not in old];added=sorted(cases-baseline)
write('test-summary.json',{'cases':len(cases),'baseline':len(baseline),'groups':stats,'suites':suites,'addedCases':added,'semanticReplacements':[{'before':c,'after':replacement} for c in old],'unexplainedMissing':unexplained})
jar=target/'pet-platform-backend-0.0.0-SNAPSHOT.jar'
test_classes={str(p.relative_to(target/'test-classes')) for p in (target/'test-classes').rglob('*.class')}
test_resources={str(p.relative_to(root/'apps/backend/src/test/resources')) for p in (root/'apps/backend/src/test/resources').rglob('*') if p.is_file()}
encoded=re.compile(rb'\$pbkdf2-sha256\$v1\$[0-9]{5,7}\$[A-Za-z0-9+/]{43}=\$[A-Za-z0-9+/]{43}=')
raw_token=re.compile(rb'(?:Bearer |(?:__Secure-pet_staff_sid|pet_dev_staff_sid)=)[A-Za-z0-9_-]{16,256}')
with zipfile.ZipFile(jar) as a:
    entries=a.namelist();violations=[n for n in entries if n.removeprefix('BOOT-INF/classes/') in test_classes|test_resources or 'com/pet/testing/' in n or 'testcontainers-' in n or '/security-migrations/' in n or '/persistence-migrations/' in n]
    credentials=[];fixtures=[];unresolved=[]
    for n in entries:
        if not n.startswith('BOOT-INF/classes/') or n.endswith('/'):continue
        content=a.read(n)
        if encoded.search(content):credentials.append(n)
        if any(v in content for v in (b'/__p05-browser',b'/__authentication-test',b'/__protocol-test',b'/__credential-test',b'P05BrowserHarness',b'TEST_PASSWORD',b'OnlyContainer',b'DO-NOT-OUTPUT',b'p05_probe_runtime',b'StaffCredentialSecurityIT',b'cleanupFailures',b'beforeFinal',b'afterCommit')):fixtures.append(n)
        if b'Unresolved compilation problem' in content:unresolved.append(n)
    sql=[n for n in entries if n.startswith('BOOT-INF/classes/db/migration/') and n.endswith('.sql')]
    matches={Path(n).name:a.read(n)==(root/'apps/backend/src/main/resources/db/migration'/Path(n).name).read_bytes() for n in sql}
    entities=[n for n in entries if n.startswith('BOOT-INF/classes/') and n.endswith('.class') and b'Ljakarta/persistence/Entity;' in a.read(n)]
    provider='BOOT-INF/classes/com/pet/platform/identity/infrastructure/session/StaffAuthenticationFilter.class' in entries
    modules=[n for n in entries if n.startswith('BOOT-INF/lib/sa-token')]
artifact={'path':str(jar),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'testIsolationViolations':violations,'encodedCredentialConstants':credentials,'fixtureStrings':fixtures,'unresolvedCompileStubs':unresolved,'productionSql':sql,'migrationMatches':matches,'productionEntities':entities,'testClassesCompared':len(test_classes),'testResourcesCompared':len(test_resources),'productionProviderIncluded':provider,'saTokenModules':modules}
write('artifact-audit.json',artifact)
files=sorted(set(subprocess.check_output(['git','ls-files','--modified','--others','--exclude-standard'],cwd=root,text=True).splitlines()))
categories={'production':[],'tests':[],'generated':[],'documents':[],'evidence':[],'typeOnlyConsumers':[]}
for n in files:
    c=('evidence' if n.startswith('docs/testing/evidence/P05-03/') else 'generated' if 'generated/' in n or 'api-contracts/openapi/' in n else 'tests' if 'apps/backend/src/test/' in n or 'api-contracts/test/' in n else 'production' if n.startswith('apps/backend/src/main/java/') or n.endswith('.sql') else 'typeOnlyConsumers' if n.endswith('contracts.ts') else 'documents');categories[c].append(n)
write('file-manifest.json',categories)
# 预创建本工具和结果的可链接入口，包装器稍后写实际退出元数据。
if not (ev/'final-audit-command.json').exists():write('final-audit-command.json',{'state':'RUNNING','cwd':str(root),'argv':['python3','docs/testing/evidence/P05-03/final-audit.py']})
write('final-audit.json',{'state':'RUNNING'})
links=[]
for n in files:
    p=root/n
    if p.suffix!='.md':continue
    for link in re.findall(r'\]\(([^)]+)\)',p.read_text()):
        if re.match(r'^[a-z]+:',link) or link.startswith('#'):continue
        local=link.split('#')[0].strip('<>')
        if local and not (p.parent/local).exists():links.append({'file':n,'target':local})
tracked=subprocess.check_output(['git','ls-files'],cwd=root,text=True).splitlines()
protected=[n for n in tracked if n.startswith(('ui/','infra/','.github/','docs/testing/evidence/P05-01/','docs/testing/evidence/P05-02/','docs/testing/evidence/P04-','apps/admin-web/','apps/wechat-miniprogram/miniprogram/pages/')) and n!='apps/admin-web/src/contracts.ts' or n in ('AGENTS.md','pnpm-lock.yaml','package.json','apps/backend/pom.xml','docs/development/VERSION-MATRIX.md','docs/testing/P05-01-VERIFICATION.md','docs/testing/P05-02-VERIFICATION.md','apps/backend/src/main/resources/db/migration/V1__platform_identity_foundation.sql')]
preservation=[n for n in protected if not (root/n).exists() or subprocess.check_output(['git','show','HEAD:'+n],cwd=root)!=(root/n).read_bytes()]
log_hits=[]
for p in list(ev.glob('*.log'))+list((ev/'verify-final-reports').rglob('*.xml'))+list((ev/'verify-final-reports').rglob('*.log'))+list((ev/'verify-final-reports').rglob('*.json')):
    content=p.read_bytes()
    if encoded.search(content) or raw_token.search(content) or b'OnlyContainer' in content or b'DO-NOT-OUTPUT' in content:log_hits.append(str(p.relative_to(ev)))
diff=subprocess.run(['git','diff','--check'],cwd=root,capture_output=True,text=True)
containers=subprocess.check_output(['docker','ps','--filter','label=org.testcontainers=true','--format','{{.ID}} {{.Image}}'],text=True).splitlines()
required=['verify-second','contracts-generate-second','contracts-check','contracts-typecheck','web-typecheck','miniprogram-typecheck','repo-check','contracts-final-snapshot','container-cleanup-check','java-version','node-version','pnpm-version']
command_failures=[n for n in required if json.loads((ev/(n+'.json')).read_text()).get('exitCode')!=0]
schema=json.loads((root/'packages/api-contracts/openapi/backend.openapi.json').read_text());components=schema['components']['schemas'];expected={'/api/admin/auth/password':'put','/api/admin/auth/logout-all':'post','/api/admin/identity/users/{employeeId}/password':'put','/api/admin/identity/users/{employeeId}/revoke-sessions':'post'}
api={'paths':len(schema['paths']),'sensitivePaths':{},'currentIdentityRequired':components['CurrentIdentity']['required'],'passwordChangeRequired':components['CurrentIdentity']['properties']['passwordChangeRequired']}
for path,method in expected.items():
    op=schema['paths'][path][method];s=components[op['requestBody']['content']['application/json']['schema']['$ref'].split('/')[-1]]
    api['sensitivePaths'][path]={'method':method,'requestRequired':s.get('required'),'passwordProperties':{k:v for k,v in s['properties'].items() if 'Password' in k},'security':op['security'],'cleanupHeader':op['responses']['200']['headers']['X-Session-Cleanup']['schema']}
write('openapi-audit.json',api)
api_ok=len(schema['paths'])==9 and 'passwordChangeRequired' in api['currentIdentityRequired'] and api['passwordChangeRequired']['type']=='boolean' and all(row['cleanupHeader']['type']=='string' and set(row['cleanupHeader']['enum'])=={'COMPLETE','PENDING'} and all(p.get('writeOnly') and p.get('format')=='password' for p in row['passwordProperties'].values()) for row in api['sensitivePaths'].values())
ok=len(cases)==328 and len(baseline)==292 and len(old)==1 and replacement in cases and not unexplained and len(added)==37 and not violations and not credentials and not fixtures and not unresolved and provider and len(sql)==2 and all(matches.values()) and len(entities)==7 and len(modules)==7 and all('1.46.0.jar' in n for n in modules) and not preservation and not links and not log_hits and not command_failures and not containers and diff.returncode==0 and api_ok and all(row[k]==0 for row in stats.values() for k in ('failures','errors','skipped'))
summary={'cwd':str(root),'argv':['python3','docs/testing/evidence/P05-03/final-audit.py'],'exitCode':0 if ok else 1,'createdAtUTC':datetime.datetime.now(datetime.timezone.utc).isoformat(),'testCount':len(cases),'baselineCount':len(baseline),'baselineRetainedUnchanged':len(cases&baseline),'semanticReplacements':[{'before':c,'after':replacement} for c in old],'unexplainedMissingBaselineCases':unexplained,'addedIncludingReplacement':len(added),'groups':stats,'logCredentialHits':log_hits,'preservationChecked':len(protected),'preservationViolations':preservation,'missingDocumentLinks':links,'requiredCommandFailures':command_failures,'diffCheckExitCode':diff.returncode,'diffCheckSummary':diff.stdout[-2000:],'runningTestcontainers':containers,'openapiChecks':api_ok,'artifact':artifact}
write('final-audit.json',summary)
commands=[]
for p in sorted(ev.glob('*.json')):
    data=json.loads(p.read_text())
    if isinstance(data,dict) and 'exitCode' in data and ('argv' in data or 'command' in data):commands.append({'file':p.name,**{k:data[k] for k in ('cwd','argv','command','exitCode','startedAtUTC','finishedAtUTC','log') if k in data}})
write('COMMAND-INDEX.json',commands)
print(json.dumps({k:v for k,v in summary.items() if k!='artifact'},ensure_ascii=False,indent=2));raise SystemExit(0 if ok else 1)
