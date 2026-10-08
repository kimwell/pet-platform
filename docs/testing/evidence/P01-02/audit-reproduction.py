#!/usr/bin/env python3
"""仅审计P01-02文档/证据与既有成果，不读取任何治理状态。"""
from pathlib import Path
import hashlib,json,re,sys
ROOT=Path(__file__).resolve().parents[4]
E=ROOT/'docs/testing/evidence/P01-02'
allowed={'AGENTS.md','README.md','docs/product/SCAFFOLD-SCOPE.md','docs/architecture/TECHNICAL-BASELINE.md','docs/architecture/PROJECT-STRUCTURE.md','docs/contracts/CORE-CONTRACTS.md','docs/development/VERSION-MATRIX.md','docs/development/ROADMAP.md','docs/development/DECISION-LOG.md'}
required=[
'docs/architecture/PROJECT-STRUCTURE.md','docs/architecture/MODULE-BOUNDARIES.md','docs/architecture/AUTHENTICATION.md','docs/architecture/AUTHORIZATION.md','docs/architecture/MULTI-TENANCY.md','docs/architecture/CONFIGURATION.md','docs/architecture/FILE-STORAGE.md','docs/architecture/ASYNC-EVENTS.md',
'docs/contracts/API.md','docs/contracts/PAGINATION.md','docs/contracts/DATA-TYPES.md','docs/contracts/IDENTITY.md','docs/contracts/ATTACHMENTS.md','docs/contracts/ASYNC-TASKS.md','docs/contracts/OPENAPI-GENERATION.md','docs/contracts/CORE-CONTRACTS.md',
'docs/conventions/BACKEND.md','docs/conventions/WEB-PAGES.md','docs/conventions/WEB-STATE.md','docs/conventions/MINIPROGRAM-PAGES.md',
'docs/testing/ACCEPTANCE-MATRIX.md','docs/testing/P01-02-VERIFICATION.md']
results=[]
def add(name,ok,detail):results.append({'id':name,'result':'PASS' if ok else 'FAIL','details':detail})
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
missing=[n for n in required if not (ROOT/n).is_file()]
add('required-documents',not missing,{'count':len(required),'missing':missing})
before=json.loads((E/'inventory-before.json').read_text());changed=[];lost=[]
for name,record in before.items():
 p=ROOT/name
 if not p.exists():lost.append(name)
 elif digest(p)!=record['sha256']:changed.append(name)
unauthorized=[n for n in changed if n not in allowed]
add('existing-files-preserved',not lost and not unauthorized,{'before_files':len(before),'modified_authorized':sorted(changed),'preserved_count':len(before)-len(changed)-len(lost),'missing':lost,'unauthorized_modified':unauthorized})
ui=[n for n in before if n.startswith('ui/')]
add('ui-preserved',all(n not in changed and n not in lost for n in ui),{'files':len(ui)})
# 只枚举根名，绝不打开或遍历治理状态目录。
top_names=sorted(p.name for p in ROOT.iterdir())
forbidden=['apps','packages','templates','scripts','infra','enterprise-app-scaffold','.delivery-os','package.json','pnpm-lock.yaml','pom.xml']
present=[n for n in forbidden if n in top_names]
add('no-formal-initialization',not present,{'forbidden_present':present,'top_level_names':top_names})
docs=[ROOT/n for n in sorted(set(required)|allowed) if (ROOT/n).is_file()]
local_count=0;broken=[]
for p in docs:
 text=p.read_text()
 for target in re.findall(r'\[[^\]]*\]\(([^)]+)\)',text):
  target=target.strip().split(' ')[0].strip('<>')
  if re.match(r'^[a-z]+://',target) or target.startswith('#'):continue
  path,_,anchor=target.partition('#');candidate=(p.parent/path).resolve()
  local_count+=1
  if not candidate.exists():broken.append({'file':str(p.relative_to(ROOT)),'target':target})
  elif anchor and candidate.suffix=='.md':
   anchors=[]
   for heading in re.findall(r'^#{1,6} (.+)$',candidate.read_text(),re.M):
    anchors.append(re.sub(r'[^\w\- ]','',heading.lower()).replace(' ','-'))
   if anchor not in anchors:broken.append({'file':str(p.relative_to(ROOT)),'target':target,'reason':'anchor'})
add('local-references',not broken,{'checked':local_count,'broken':broken})
mat=(ROOT/'docs/development/VERSION-MATRIX.md').read_text();rows=[s for s in mat.splitlines() if s.startswith('| ') and ' | 2026-10-07 | ' in s]
add('single-version-source',len(rows)==56 and 'P01-02 补充' in mat,{'matrix_rows':len(rows),'historical_dependency_rows':53,'new_protocol_tool_rows':3})
core=(ROOT/'docs/contracts/CORE-CONTRACTS.md').read_text()
add('index-no-competing-definitions','```json' not in core and len(re.findall(r'^\| ',core,re.M))>=21,{'authority_index':True})
matrix=(ROOT/'docs/testing/ACCEPTANCE-MATRIX.md').read_text();ids=re.findall(r'^\| (A\d{2}-\d{2}) ',matrix,re.M);phases={int(n[1:3]) for n in ids}
add('acceptance-P02-P12',phases==set(range(2,13)) and len(ids)==len(set(ids)),{'entries':len(ids),'phases':sorted(phases)})
# 秘密特征只报告文件与类型，不打印命中内容；此检查不能证明所有秘密不存在。
secret_hits=[]
scan=docs+[p for p in E.iterdir() if p.is_file() and p.suffix in {'.md','.json','.java','.ts','.log','.py'} and p.name not in {'document-audit.json','EVIDENCE-MANIFEST.json'}]
patterns={'private-key':r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----','github-token':r'gh[pousr]_[A-Za-z0-9]{24,}','aws-access':r'AKIA[0-9A-Z]{16}'}
for p in scan:
 text=p.read_text(errors='replace')
 for kind,pattern in patterns.items():
  if p==Path(__file__).resolve():continue
  if re.search(pattern,text):secret_hits.append({'file':str(p.relative_to(ROOT)),'type':kind})
add('secret-pattern-scan',not secret_hits,{'hits':secret_hits,'boundary':'特征扫描+人工检查；技术fixture非真实秘密，无Token输出'})
commands=json.loads((E/'probe-commands.json').read_text())+json.loads((E/'sa-token-probe-commands.json').read_text())
failed=[r['id'] for r in commands if r['exit_code']!=r.get('expected_exit_code',0)]
add('actual-probe-results',not failed,{'records':len(commands),'unexpected_failures':failed,'expected_drift_exit':1})
exit_code=0 if all(r['result']=='PASS' for r in results) else 1
output={'date':'2026-10-07','cwd':str(ROOT),'command':['python3','docs/testing/evidence/P01-02/audit-reproduction.py'],'exit_code':exit_code,'results':results,'boundary':'文档审计，不是业务运行验收'}
(E/'document-audit.json').write_text(json.dumps(output,ensure_ascii=False,indent=2)+'\n')
new_docs=[str(p.relative_to(ROOT)) for p in docs if str(p.relative_to(ROOT)) not in before]
(E/'change-summary.json').write_text(json.dumps({'new_specification_files':sorted(new_docs),'modified_existing':sorted(changed),'evidence_files':sorted(set(str(p.relative_to(ROOT)) for p in E.iterdir() if p.is_file())|{'docs/testing/evidence/P01-02/change-summary.json','docs/testing/evidence/P01-02/EVIDENCE-MANIFEST.json'}),'ui_preserved_files':len(ui)},ensure_ascii=False,indent=2)+'\n')
files=[p for p in docs+list(E.iterdir()) if p.is_file() and p.name!='EVIDENCE-MANIFEST.json']
manifest={str(p.relative_to(ROOT)):{'bytes':p.stat().st_size,'sha256':digest(p)} for p in sorted(set(files))}
(E/'EVIDENCE-MANIFEST.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'exit_code':exit_code,'checks':[{k:r[k] for k in ['id','result']} for r in results],'local_references':local_count,'new_specification_files':len(new_docs),'modified_existing':len(changed),'ui_preserved':len(ui)},ensure_ascii=False))
sys.exit(exit_code)
