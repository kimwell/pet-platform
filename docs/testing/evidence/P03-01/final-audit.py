"""一致性/保全审计，不代替HTTP或业务验收。"""
import hashlib, json, re, zipfile, xml.etree.ElementTree as ET
from pathlib import Path
root=Path(__file__).resolve().parents[4]
out=Path(__file__).resolve().parent
baseline=json.loads((out/'baseline-files.json').read_text())
changed=[];missing=[]
for name,digest in baseline.items():
 p=root/name
 if not p.is_file(): missing.append(name)
 elif hashlib.sha256(p.read_bytes()).hexdigest()!=digest: changed.append(name)
allowed={'apps/backend/pom.xml','apps/backend/src/test/java/com/pet/platform/ApplicationTest.java','README.md','docs/contracts/API.md','docs/contracts/PAGINATION.md','docs/conventions/BACKEND.md','docs/development/VERSION-MATRIX.md','docs/development/ROADMAP.md','docs/development/DECISION-LOG.md','docs/testing/ACCEPTANCE-MATRIX.md','docs/architecture/PROJECT-STRUCTURE.md'}
assert not missing,missing
assert set(changed)<=allowed,changed
new=[]
for p in root.rglob('*'):
 if not p.is_file() or any(x in p.parts for x in ('.git','node_modules','target','dist','.local-data','miniprogram_npm')): continue
 name=str(p.relative_to(root))
 if name not in baseline: new.append(name)
assert all(n.startswith(('apps/backend/src/','docs/testing/evidence/P03-01/')) or n=='docs/testing/P03-01-VERIFICATION.md' for n in new),new
broken=[]
for name in [n for n in changed+new if n.endswith('.md')]:
 p=root/name
 for target in re.findall(r'\[[^\]\n]+\]\(([^)\n]+)\)',p.read_text()):
  if re.match(r'[a-zA-Z][a-zA-Z0-9+.-]*:',target) or target.startswith('#'): continue
  target=target.split('#')[0]
  if (p.parent/target).resolve() == out/'final-audit.json': continue
  if not (p.parent/target).exists(): broken.append((name,target))
assert not broken,broken
expected={'com.pet.platform.ApplicationTest':2,'com.pet.platform.protocol.ApiProtocolTest':30,'com.pet.platform.protocol.ApiProtocolIT':2}
for name,count in expected.items():
 suite=ET.parse(out/('TEST-'+name+'.xml')).getroot()
 assert int(suite.attrib['tests'])==count
 assert all(suite.attrib[k]=='0' for k in ['failures','errors','skipped'])
assert json.loads((out/'verify-final.json').read_text())['exitCode']==0
jar=root/'apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar'
assert hashlib.sha256(jar.read_bytes()).hexdigest()==json.loads((out/'artifact-audit.json').read_text())['sha256']
production=root/'apps/backend/src/main/java/com/pet/platform'
main_files=list(production.rglob('*.java'))
assert len(main_files)==21
for p in main_files:
 source=p.read_text()
 assert 'package com.pet.platform' in source
 assert '__protocol' not in source and 'ResponseBodyAdvice' not in source
 assert not re.search(r'(?m)^@RestController\s*$',source) or p.name=='ProtocolErrorController.java'
logs=(out/'verify-final.log').read_text()+(out/'local-startup.log').read_text()
assert all(s not in logs for s in ['TECHNICAL_SECRET_RAW_INPUT','private_table','SELECT password'])
assert 'P03-01' in (root/'docs/testing/P03-01-VERIFICATION.md').read_text()
assert 'IN_PROGRESS' in (root/'docs/development/ROADMAP.md').read_text()
runtime=json.loads((out/'local-runtime.json').read_text())
assert runtime['startupObserved'] and runtime['portClosed']
record={'missing':missing,'changedExistingFiles':sorted(changed),'newFiles':sorted(new),'brokenLinks':broken,'unchangedBaselineFiles':len(baseline)-len(changed),'preservedUiAndFrontendAndHistory':not any(n.startswith(('ui/','apps/admin-web/','apps/wechat-miniprogram/','packages/','infra/','docs/testing/evidence/P01','docs/testing/evidence/P02')) for n in changed),'mainJavaFiles':len(main_files),'tests':34,'failures':0,'errors':0,'skipped':0,'productionJarSHA256':hashlib.sha256(jar.read_bytes()).hexdigest(),'result':'PASS','scope':'documented consistency, artifact and preservation audit; HTTP evidence remains separate'}
(out/'final-audit.json').write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n')
assert (out/'final-audit.json').exists()
manifest={str(p.relative_to(out)):hashlib.sha256(p.read_bytes()).hexdigest() for p in out.rglob('*') if p.is_file() and p.name not in ('EVIDENCE-MANIFEST.json','audit-final.log','audit-final.json')}
(out/'EVIDENCE-MANIFEST.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'result':'PASS','changedExistingFiles':len(changed),'newFiles':len(new),'preservedUiAndFrontendAndHistory':record['preservedUiAndFrontendAndHistory'],'tests':34,'brokenLinks':broken},ensure_ascii=False))
