from pathlib import Path
import hashlib,json,re,subprocess,zipfile,xml.etree.ElementTree as ET,datetime
root=Path(__file__).resolve().parents[4];ev=Path(__file__).resolve().parent
reports=ev/'verify-complete-reports'
cases=[];groups={}
for group in ('surefire','failsafe'):
 totals=dict(tests=0,failures=0,errors=0,skipped=0);suites=[]
 for p in sorted((reports/f'{group}-reports').glob('TEST-*.xml')):
  x=ET.parse(p).getroot();stats={k:int(x.attrib[k]) for k in totals};suites.append(dict(name=x.attrib['name'],**stats))
  for k,v in stats.items():totals[k]+=v
  cases += [f"{t.attrib['classname']}#{t.attrib['name']}" for t in x.findall('testcase')]
 groups[group]=dict(totals=totals,suites=suites)
baseline=json.loads((ev/'baseline-tests.json').read_text());missing=sorted(set(baseline)-set(cases))
result=dict(groups=groups,total=len(cases),baselineCases=len(baseline),missingBaselineCases=missing,addedCases=sorted(set(cases)-set(baseline)))
(ev/'test-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2))
jar=root/'apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar'
testroot=root/'apps/backend/target/test-classes';resources=root/'apps/backend/src/test/resources'
testclasses={str(p.relative_to(testroot)) for p in testroot.rglob('*.class')}
testres={str(p.relative_to(resources)) for p in resources.rglob('*') if p.is_file()}
with zipfile.ZipFile(jar) as z:
 entries=z.namelist()
 violations=[p for p in entries if p.removeprefix('BOOT-INF/classes/') in testclasses|testres or 'com/pet/testing/' in p or 'testcontainers-' in p or '/security-migrations/' in p]
 fixture_strings=[p for p in entries if p.startswith('BOOT-INF/classes/') and not p.endswith('/') and any(s in z.read(p) for s in (b'safety_parent',b'security_probe_runtime',b'technical-redis.conf'))]
 formal_sql=[p for p in entries if p.startswith('BOOT-INF/classes/db/migration/') and p.endswith('.sql')]
 production_entities=[p for p in entries if p.startswith('BOOT-INF/classes/') and p.endswith('.class') and b'Ljakarta/persistence/Entity;' in z.read(p)]
artifact=dict(path=str(jar),sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),violations=violations,fixtureStrings=fixture_strings,productionSql=formal_sql,productionEntityClasses=production_entities,testClassesCompared=len(testclasses),testResourcesCompared=len(testres))
(ev/'artifact-audit.json').write_text(json.dumps(artifact,ensure_ascii=False,indent=2))
protected={p:sha for p,sha in json.loads((ev/'baseline-files.json').read_text()).items() if p.startswith(('ui/','apps/admin-web/','apps/wechat-miniprogram/','packages/','.github/','infra/','docs/testing/evidence/')) or p in ('AGENTS.md','pnpm-lock.yaml','package.json') or p.startswith('apps/backend/src/main/resources/db/migration/') or p in ('docs/testing/P04-01-VERIFICATION.md','docs/testing/P04-02-VERIFICATION.md','apps/backend/src/test/resources/security-migrations/V1__tenant_security_fixtures.sql')}
preservation=[p for p,sha in protected.items() if not (root/p).is_file() or hashlib.sha256((root/p).read_bytes()).hexdigest()!=sha]
files=subprocess.check_output(['git','ls-files','--modified','--others','--exclude-standard'],cwd=root,text=True).splitlines()
files=sorted(set(files+['docs/testing/evidence/P04-03/final-audit.json','docs/testing/evidence/P04-03/changed-files.json','docs/testing/evidence/P04-03/COMMAND-INDEX.json']))
(ev/'changed-files.json').write_text(json.dumps(files,ensure_ascii=False,indent=2))
links=[]
for name in files:
 p=root/name
 if p.suffix!='.md' or not p.is_file():continue
 for target in re.findall(r'\]\(([^)]+)\)',p.read_text()):
  if re.match(r'^[a-z]+:',target) or target.startswith('#'):continue
  target=target.split('#')[0]
  if target and not (p.parent/target).exists():links.append(dict(file=name,target=target))
diff=subprocess.run(['git','diff','--check'],cwd=root,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
observations=[json.loads(p.read_text()) for p in (reports/'p04-03-database-observations').glob('*.json')]
observation_errors=[o['test'] for o in observations if o['outsideTransactionTenantGuc']!='EMPTY' or o['runtimeRole']!='security_probe_runtime' or any(o['role'].values())]
previous=[json.loads(p.read_text()) for p in (reports/'p04-02-database-observations').glob('*.json')]
previous_errors=[o['test'] for o in previous if o['outsideTransactionTenantGuc']!='EMPTY' or o['runtimeRole']!='security_probe_runtime']
resource_baseline=json.loads((ev/'resources-before-final.json').read_text())
containers=subprocess.check_output(['docker','ps','--format','{{.ID}} {{.Image}} {{.Names}}'],text=True).splitlines()
volumes=subprocess.check_output(['docker','volume','ls','--format','{{.Name}}'],text=True).splitlines()
resource=dict(cwd=str(root),commands=resource_baseline['commands'],containers=containers,volumes=volumes,lostBaselineVolumes=sorted(set(resource_baseline['volumes'])-set(volumes)),exitCode=0,createdAtUTC=datetime.datetime.now(datetime.timezone.utc).isoformat())
(ev/'resources-final.json').write_text(json.dumps(resource,ensure_ascii=False,indent=2))
head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()
verify=json.loads((ev/'verify-complete.json').read_text())
# VERSION-MATRIX只追加接入证据，原冻结正文逐字保留。
original_matrix=subprocess.check_output(['git','show','HEAD:docs/development/VERSION-MATRIX.md'],cwd=root,text=True)
matrix_preserved=(root/'docs/development/VERSION-MATRIX.md').read_text().startswith(original_matrix)
tree=(ev/'backend-dependency-tree.txt').read_text()
deps_ok=all(x in tree for x in ('spring-data-redis:jar:4.0.7:compile','lettuce-core:jar:6.8.2.RELEASE:compile','spring-boot-starter-data-redis:jar:4.0.8:compile'))
index=[]
for p in sorted(ev.glob('*.json')):
 d=json.loads(p.read_text())
 if isinstance(d,dict) and 'exitCode' in d and ('argv' in d or 'commands' in d or 'command' in d): index.append(dict(file=p.name,**{k:d[k] for k in ('cwd','argv','commands','command','exitCode','startedAtUTC','finishedAtUTC','log') if k in d}))
(ev/'COMMAND-INDEX.json').write_text(json.dumps(index,ensure_ascii=False,indent=2))
summary=dict(cwd=str(root),command='python3 docs/testing/evidence/P04-03/final-audit.py',testCount=len(cases),baselineCount=len(baseline),missingBaselineCases=missing,groups={g:x['totals'] for g,x in groups.items()},redisIsolationTests=sum('RedisIsolationIT#' in c for c in cases),asyncUnitTests=sum('TenantTaskExecutorTest#' in c for c in cases),asyncPostgresTests=sum('AsyncTenantPersistenceIT#' in c for c in cases),postgresSnapshots=len(observations),postgresSnapshotErrors=observation_errors,previousSecuritySnapshots=len(previous),previousSecuritySnapshotErrors=previous_errors,artifact=artifact,preservationChecked=len(protected),preservationViolations=preservation,frozenMatrixOriginalBodyPreserved=matrix_preserved,dependencyVersionsMatch=deps_ok,missingDocumentLinks=links,diffCheckExitCode=diff.returncode,diffCheckOutput=diff.stdout,gitHead=head,gitHeadUnchanged=head==(ev/'baseline-git.txt').read_text().strip().splitlines()[-1],resources=resource,sourceFiles=[p for p in files if not p.startswith('docs/testing/evidence/P04-03/')])
ok=verify['exitCode']==0 and len(cases)==222 and len(baseline)==167 and not missing and len(observations)==9 and len(previous)==48 and not observation_errors and not previous_errors and not violations and not fixture_strings and not formal_sql and not production_entities and not preservation and matrix_preserved and deps_ok and not links and diff.returncode==0 and summary['gitHeadUnchanged'] and not containers and not resource['lostBaselineVolumes'] and all(g['totals'][k]==0 for g in groups.values() for k in ('failures','errors','skipped'))
summary['exitCode']=0 if ok else 1
(ev/'final-audit.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2))
print(json.dumps({k:v for k,v in summary.items() if k not in ('sourceFiles','artifact','resources')},ensure_ascii=False,indent=2))
raise SystemExit(summary['exitCode'])
