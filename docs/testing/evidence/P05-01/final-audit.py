from pathlib import Path
import hashlib,json,re,subprocess,zipfile,xml.etree.ElementTree as ET,datetime
root=Path(__file__).resolve().parents[4];ev=Path(__file__).resolve().parent
reports=ev/'verify-final-reports'
def cases_at(directory):
 cases=[];stats={};suites=[]
 for group in ('surefire','failsafe'):
  total={k:0 for k in ('tests','failures','errors','skipped')}
  for p in sorted((directory/(group+'-reports')).glob('TEST-*.xml')):
   x=ET.parse(p).getroot();row={'name':x.attrib['name'],**{k:int(x.attrib[k]) for k in total}}
   suites.append(row)
   for k in total:total[k]+=row[k]
   cases += [t.attrib['classname']+'#'+t.attrib['name'] for t in x.findall('testcase')]
  stats[group]=total
 return cases,stats,suites
cases,stats,suites=cases_at(reports)
baseline,_,_=cases_at(root/'docs/testing/evidence/P04-03/verify-complete-reports')
missing=sorted(set(baseline)-set(cases));added=sorted(set(cases)-set(baseline))
results={'total':len(cases),'baselineCount':len(baseline),'addedCount':len(added),'missingBaselineCases':missing,'groups':stats,'suites':suites,'addedCases':added}
(ev/'test-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))
jar=root/'apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar'
test_classes={str(p.relative_to(root/'apps/backend/target/test-classes')) for p in (root/'apps/backend/target/test-classes').rglob('*.class')}
test_resources={str(p.relative_to(root/'apps/backend/src/test/resources')) for p in (root/'apps/backend/src/test/resources').rglob('*') if p.is_file()}
encoded=re.compile(rb'\$pbkdf2-sha256\$v1\$[0-9]{5,7}\$[A-Za-z0-9+/]{43}=\$[A-Za-z0-9+/]{43}=')
with zipfile.ZipFile(jar) as z:
 entries=z.namelist()
 violations=[name for name in entries if name.removeprefix('BOOT-INF/classes/') in test_classes|test_resources or 'com/pet/testing/' in name or 'testcontainers-' in name or '/security-migrations/' in name or '/persistence-migrations/' in name]
 secret_constants=[];fixture_strings=[]
 for name in entries:
  if not name.startswith('BOOT-INF/classes/') or name.endswith('/'):continue
  data=z.read(name)
  if encoded.search(data):secret_constants.append(name)
  if any(value in data for value in (b'safety_parent',b'security_probe_runtime',b'p05_probe_runtime',b'OnlyContainer',b'DO-NOT-OUTPUT',b'technical-redis.conf')):fixture_strings.append(name)
 sql=[n for n in entries if n.startswith('BOOT-INF/classes/db/migration/') and n.endswith('.sql')]
 entities=[n for n in entries if n.startswith('BOOT-INF/classes/') and n.endswith('.class') and b'Ljakarta/persistence/Entity;' in z.read(n)]
artifact={'path':str(jar),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'testIsolationViolations':violations,'encodedCredentialConstants':secret_constants,'fixtureStrings':fixture_strings,'productionSql':sql,'productionEntities':entities,'testClassesCompared':len(test_classes),'testResourcesCompared':len(test_resources)}
(ev/'artifact-audit.json').write_text(json.dumps(artifact,ensure_ascii=False,indent=2))
files=subprocess.check_output(['git','ls-files','--modified','--others','--exclude-standard'],cwd=root,text=True).splitlines()
source_files=sorted(set(name for name in files if not name.startswith('docs/testing/evidence/P05-01/')))
(ev/'changed-files.json').write_text(json.dumps(source_files,ensure_ascii=False,indent=2))
links=[]
for name in source_files:
 p=root/name
 if p.suffix!='.md':continue
 for target in re.findall(r'\]\(([^)]+)\)',p.read_text()):
  if re.match(r'^[a-z]+:',target) or target.startswith('#'):continue
  target=target.split('#')[0]
  if target and not (p.parent/target).exists():links.append({'file':name,'target':target})
protected=[name for name in subprocess.check_output(['git','ls-files'],cwd=root,text=True).splitlines() if name.startswith(('ui/','apps/admin-web/','apps/wechat-miniprogram/','packages/','.github/','apps/backend/src/test/resources/','docs/testing/evidence/P04-')) or name in ('AGENTS.md','pnpm-lock.yaml','package.json','apps/backend/pom.xml','docs/development/VERSION-MATRIX.md','docs/testing/P04-ACCEPTANCE.md','docs/testing/P04-03-VERIFICATION.md')]
preservation=[]
for name in protected:
 original=subprocess.check_output(['git','show','HEAD:'+name],cwd=root)
 if not (root/name).exists() or original!=(root/name).read_bytes():preservation.append(name)
observations=[json.loads(p.read_text()) for p in (reports/'p05-01-observations').glob('*.json')]
observation_errors=[o['test'] for o in observations if o['runtime']!='p05_probe_runtime' or o['outsideTransactionTenant']!='EMPTY']
log_secret_hits=[]
for p in ev.glob('*.log'):
 data=p.read_bytes()
 if encoded.search(data) or b'OnlyContainer' in data or b'DO-NOT-OUTPUT' in data:log_secret_hits.append(p.name)
diff=subprocess.run(['git','diff','--check'],cwd=root,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
verify=json.loads((ev/'verify-final.json').read_text())
summary={'cwd':str(root),'command':'python3 docs/testing/evidence/P05-01/final-audit.py','testCount':len(cases),'baselineCount':len(baseline),'addedCount':len(added),'groups':stats,'missingBaselineCases':missing,'identitySnapshots':len(observations),'identitySnapshotErrors':observation_errors,'artifact':artifact,'logSecretHits':log_secret_hits,'preservationChecked':len(protected),'preservationViolations':preservation,'missingDocumentLinks':links,'diffCheckExitCode':diff.returncode,'sourceFiles':source_files,'gitHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),'containers':subprocess.check_output(['docker','ps','--format','{{.ID}} {{.Image}} {{.Names}}'],text=True).splitlines(),'createdAtUTC':datetime.datetime.now(datetime.timezone.utc).isoformat()}
ok=verify['exitCode']==0 and len(baseline)==222 and len(cases)==258 and not missing and len(observations)==27 and not observation_errors and not violations and not secret_constants and not fixture_strings and len(sql)==1 and len(entities)==7 and not preservation and not links and diff.returncode==0 and not log_secret_hits and all(row[k]==0 for row in stats.values() for k in ('failures','errors','skipped'))
summary['exitCode']=0 if ok else 1
(ev/'final-audit.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2))
commands=[]
for p in sorted(ev.glob('*.json')):
 d=json.loads(p.read_text())
 if isinstance(d,dict) and 'exitCode' in d and ('argv' in d or 'command' in d):commands.append({'file':p.name,**{k:d[k] for k in ('cwd','argv','command','exitCode','startedAtUTC','finishedAtUTC','log') if k in d}})
(ev/'COMMAND-INDEX.json').write_text(json.dumps(commands,ensure_ascii=False,indent=2))
print(json.dumps({k:v for k,v in summary.items() if k not in ('sourceFiles','artifact')},ensure_ascii=False,indent=2))
raise SystemExit(summary['exitCode'])
