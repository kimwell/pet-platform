import pathlib,shutil,json,xml.etree.ElementTree as ET,collections,zipfile,hashlib
root=pathlib.Path('/Users/kimwell/work/pet-platform')
evidence=root/'docs/testing/evidence/P07-01'
out=evidence/'verify-final-reports';out.mkdir(exist_ok=True)
for name in ('surefire-reports','failsafe-reports','p07-01-observations','p07-01-command-observations','openapi'):
 p=root/'apps/backend/target'/name
 if p.exists():shutil.copytree(p,out/name,dirs_exist_ok=True)
def report(base):
 counts=collections.Counter();cases=collections.Counter();suites=[]
 for path in sorted(base.glob('*reports/TEST-*.xml')):
  t=ET.parse(path).getroot()
  metrics={k:int(t.get(k,0)) for k in ('tests','failures','errors','skipped')};counts.update(metrics)
  suites.append({'suite':t.get('name'),**metrics})
  for case in t.findall('testcase'): cases[(case.get('classname'),case.get('name'))]+=1
 return counts,cases,suites
counts,cases,suites=report(out)
old,oldcases,_=report(root/'docs/testing/evidence/P05-05/verify-final-reports')
missing=oldcases-cases;added=cases-oldcases
result={'actual':dict(counts),'historicalBaseline':dict(old),'originalCasesRetained':not missing,'missingCases':[list(c)+[n] for c,n in missing.items()],'addedCases':[list(c)+[n] for c,n in added.items()],'suites':suites}
(evidence/'test-summary.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
assert counts['tests']==455 and all(counts[x]==0 for x in ('failures','errors','skipped'))
assert old['tests']==421 and not missing and sum(added.values())==34
jar=root/'apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar'
with zipfile.ZipFile(jar) as z:
 names=z.namelist();classes={n.removeprefix('BOOT-INF/classes/') for n in names if n.startswith('BOOT-INF/classes/') and n.endswith('.class')}
 testclasses={str(p.relative_to(root/'apps/backend/target/test-classes')) for p in (root/'apps/backend/target/test-classes').rglob('*.class')}
 overlap=classes & testclasses
 invalid=[n for n in names if n.startswith('BOOT-INF/classes/') and b'Unresolved compilation problems' in z.read(n)]
 assert not overlap and not invalid
 result={'jar':str(jar.relative_to(root)),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'size':jar.stat().st_size,'productionClassCount':len(classes),'testClassCount':len(testclasses),'testClassOverlap':sorted(overlap),'invalidCompilationMarkers':invalid,'migrations':[n for n in names if n.startswith('BOOT-INF/classes/db/migration/V')],'grade':'COMPILED / RUNTIME_VERIFIED via ProductionArtifactIT, PackagedDocumentPolicyIT, EmployeeReadUpgradeRuntimeIT'}
 (evidence/'production-artifact.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
 (evidence/'production-jar-entries.txt').write_text('\n'.join(names)+'\n')
print(json.dumps({'tests':dict(counts),'historical':dict(old),'missing':sum(missing.values()),'added':sum(added.values()),'jarTestOverlap':len(overlap)},ensure_ascii=False))
