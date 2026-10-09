import pathlib,json,zipfile,hashlib,shutil,xml.etree.ElementTree as ET
root=pathlib.Path('/Users/kimwell/work/pet-platform');e=root/'docs/testing/evidence/P07-01';target=root/'apps/backend/target'
jar=target/'pet-platform-backend-0.0.0-SNAPSHOT.jar'
with zipfile.ZipFile(jar) as z:
 names=z.namelist();classes={n.removeprefix('BOOT-INF/classes/') for n in names if n.startswith('BOOT-INF/classes/') and n.endswith('.class')}
 tests={str(p.relative_to(target/'test-classes')) for p in (target/'test-classes').rglob('*.class')}
 overlap=classes&tests;invalid=[n for n in names if n.startswith('BOOT-INF/classes/') and b'Unresolved compilation problems' in z.read(n)]
 assert not overlap and not invalid
 metrics={k:0 for k in ('tests','failures','errors','skipped')}
 for p in (target/'failsafe-reports').glob('TEST-*.xml'):
  t=ET.parse(p).getroot()
  for k in metrics:metrics[k]+=int(t.get(k,0))
 assert metrics=={'tests':3,'failures':0,'errors':0,'skipped':0},metrics
 result={'jar':str(jar),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'size':jar.stat().st_size,'productionClassCount':len(classes),'testClassCount':len(tests),'testClassOverlap':sorted(overlap),'invalidCompilationMarkers':invalid,'postGenerationChecks':metrics,'full455Evidence':'verify-final-reports/ (未覆盖)','compileCommand':'package-final.json; -DskipTests只补产物，不替代完整455项验证'}
 (e/'production-artifact-final.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
 (e/'production-jar-entries-final.txt').write_text('\n'.join(names)+'\n')
 out=e/'package-final-reports';out.mkdir(exist_ok=True)
 for name in ('failsafe-reports','p07-01-command-observations'):
  shutil.copytree(target/name,out/name,dirs_exist_ok=True)
print(json.dumps(result,ensure_ascii=False))
