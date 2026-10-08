from pathlib import Path
from datetime import datetime
import hashlib,json,re,sys,tarfile
root=Path(__file__).resolve().parents[4]
ev=root/'docs/testing/evidence/P01-01'
required=['AGENTS.md','README.md','docs/product/SCAFFOLD-SCOPE.md','docs/architecture/TECHNICAL-BASELINE.md','docs/architecture/PROJECT-STRUCTURE.md','docs/contracts/CORE-CONTRACTS.md','docs/development/VERSION-MATRIX.md','docs/development/ROADMAP.md','docs/development/DECISION-LOG.md','docs/testing/P01-01-VERIFICATION.md']
results=[]
def check(name,condition,details):
 row={'id':name,'result':'PASS' if condition else 'FAIL','details':details};results.append(row);print(json.dumps(row,ensure_ascii=False))
def write_json(name,data):
 (ev/name).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
def preserved(base,before):
 missing=[];changed=[]
 for item in before:
  path=base/item['path']
  if not path.is_file():missing.append(item['path'])
  elif path.stat().st_size!=item['bytes'] or hashlib.sha256(path.read_bytes()).hexdigest()!=item['sha256']:changed.append(item['path'])
 return {'root':str(base),'checked_existing_files':len(before),'same':not missing and not changed,'missing':missing,'changed':changed}
check('required-documents',all((root/name).is_file() for name in required),{'count':len(required)})
versions=json.loads((ev/'version-records.json').read_text())
check('exact-version-matrix',len(versions)==53 and all(all(token not in row['version'] for token in ['latest','^','~']) and row['source'].startswith('https://') and row['query_date']=='2026-10-07' for row in versions),{'records':len(versions)})
metadata=json.loads((ev/'selected-npm-metadata.json').read_text());cutoff=datetime.fromisoformat('2026-10-07T23:59:59+08:00')
future=[row['name'] for row in metadata if not row['published'] or datetime.fromisoformat(row['published'].replace('Z','+00:00'))>cutoff]
check('npm-publish-date',not future,{'future_or_unknown':future,'source':'前轮实际官方注册信息，未冒充本次新查询'})
before=json.loads((ev/'root-inventory-before.json').read_text());root_preservation=preserved(root,before);write_json('root-preservation.json',root_preservation)
check('existing-root-files-preserved',root_preservation['same'] and len(before)==273,root_preservation)
prior=Path('/Users/kimwell/work/enterprise-app-scaffold');prior_before=json.loads((ev/'prior-location-inventory-before.json').read_text());prior_preservation=preserved(prior,prior_before);write_json('prior-location-preservation.json',prior_preservation)
check('prior-location-unchanged',prior_preservation['same'],prior_preservation)
conflicts=json.loads((ev/'conflict-check.json').read_text())
check('pre-write-conflict-check',conflicts['conflicts']==[] and conflicts['root_entries']==['.DS_Store','ui'],conflicts)
check('current-root-only',str(root)=='/Users/kimwell/work/pet-platform' and not (root/'enterprise-app-scaffold').exists(),{'root':str(root),'nested_scaffold_exists':(root/'enterprise-app-scaffold').exists()})
check('no-formal-applications',not any((root/name).exists() for name in ['apps','packages','templates','scripts','infra','pom.xml','package.json']),{'actual_root':sorted(path.name for path in root.iterdir())})
check('no-framework-or-git-created',not (root/'.delivery-os').exists() and not (root/'.git').exists(),{'framework_created':False,'git_initialized':False})
baseline_names=['AGENTS.md','README.md','docs/architecture/PROJECT-STRUCTURE.md','docs/architecture/TECHNICAL-BASELINE.md','docs/testing/P01-01-VERIFICATION.md'];package_fields=['com.pet.platform','apps/backend/src/main/java/com/pet/platform/','com.pet.platform.Application']
wrong_docs=[name for name in baseline_names if not all(field in (root/name).read_text() for field in package_fields) or 'com.company.scaffold' in (root/name).read_text()]
modules=['shared','platform','identity','customeridentity','attachment','audit','notification','modules']
missing_modules=[module for module in modules if not all('com.pet.platform.'+module in (root/name).read_text() for name in ['AGENTS.md','docs/architecture/PROJECT-STRUCTURE.md'])]
check('fixed-backend-package-consistency',not wrong_docs and not missing_modules,{'fields':package_fields,'modules':modules,'wrong_docs':wrong_docs,'missing_modules':missing_modules,'startup_class_created':False})
copied=[];wrong_copies=[]
for path in (prior/'docs/testing/evidence/P01-01').iterdir():
 if not path.is_file() or path.name in ['audit-reproduction.py','PROBE-REPRODUCTION.md']:continue
 target_name={'document-audit.json':'document-audit-prior-location.json','EVIDENCE-MANIFEST.json':'EVIDENCE-MANIFEST-prior-location.json'}.get(path.name,path.name);target=ev/target_name;copied.append(target_name)
 if not target.is_file() or hashlib.sha256(path.read_bytes()).hexdigest()!=hashlib.sha256(target.read_bytes()).hexdigest():wrong_copies.append(target_name)
check('original-evidence-preserved',not wrong_copies,{'unchanged_files':len(copied),'mismatches':wrong_copies,'adapted_files':['audit-reproduction.py','PROBE-REPRODUCTION.md']})
env=json.loads((ev/'current-root-checks.json').read_text())
check('current-environment-rechecked',len(env)==11 and all(row['exit_code']==(128 if row['command'][0]=='git' else 0) for row in env),{'commands':len(env),'git_exit_128_expected':True})
secret_pattern=re.compile(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|\b(?:AKIA|ASIA)[0-9A-Z]{16}\b|\bsk-(?:proj-)?[A-Za-z0-9_-]{30,}')
scan_files=[root/name for name in required]+[path for path in ev.rglob('*') if path.is_file() and path.suffix in ['.md','.json','.xml','.txt','.log','.pom','.py']]
secret_hits=[str(path.relative_to(root)) for path in scan_files if secret_pattern.search(path.read_text(errors='ignore'))]
check('secret-marker-scan',not secret_hits,{'hits':secret_hits,'scope':'仅本轮文档和证据标准私钥/API秘密标识扫描，不宣称完整安全审计'})
with tarfile.open(ev/'temporary-probes.tar.gz') as archive:archive_names=archive.getnames()
check('probe-archive-boundary',all(not name.startswith('apps/') and 'node_modules/' not in name and '/target/' not in name for name in archive_names),{'entries':len(archive_names),'archive':'测试证据，不是正式应用'})
commands=json.loads((ev/'all-command-records.json').read_text());lookup={row['id']:row for row in commands}
necessary=['wrapper-with-checksum','backend-resolve','backend-effective-pom','backend-tests','frontend-install-fixed','frontend-lock-replay-r2','web-types-final','frontend-lint-final','web-build-final','web-runtime-final','mini-types-baseline','mini-npm-pack-api-final','openapi-types-r2','openapi-types-compile-r2']
check('prior-necessary-probe-results',all(lookup[name]['exit_code']==0 for name in necessary),{'checks':len(necessary),'execution':'前轮实际探针结果保留，本次未重新执行兼容构建'})
failed=[row['id'] for row in commands if row['exit_code'] not in [0,None]]
check('failed-attempts-preserved',bool(failed) and (ev/'web-types-r2.log').exists() and (ev/'mini-types-final.log').exists(),{'count':len(failed),'ids':failed})
mini_count=len(json.loads((ev/'mini-pack-output-manifest.json').read_text())['files'])
check('mini-prior-artifact-manifest',mini_count==1177,{'file_count':mini_count,'execution':'前轮离线打包产物清单'})
write_json('document-audit.json',{'phase':'IN_PROGRESS','exit_code':None,'reason':'先生成审计输出，再验证输出文件的文档引用'})
write_json('EVIDENCE-MANIFEST.json',{'phase':'IN_PROGRESS','reason':'最终哈希在文档检查完成后计算'})
docs=[root/name for name in required]+sorted(ev.rglob('*.md'));broken=[];link_count=0
for path in docs:
 for link in re.findall(r'\[[^\]]*\]\(([^)]+)\)',path.read_text()):
  if link.startswith(('https://','http://','#','mailto:')):continue
  target=link.split('#',1)[0].strip('<>');link_count+=1
  if not (path.parent/target).exists():broken.append({'file':str(path.relative_to(root)),'link':link})
check('local-document-links',not broken,{'checked':link_count,'broken':broken,'scope':'本轮10份文档与证据说明，不改既有ui文档'})
exit_code=0 if all(row['result']=='PASS' for row in results) else 1
write_json('document-audit.json',{'command':['python3',*sys.argv],'cwd':str(Path.cwd()),'root':str(root),'date':'2026-10-07','exit_code':exit_code,'results':results})
manifest_files=[root/name for name in required]+sorted(path for path in ev.rglob('*') if path.is_file() and path.name!='EVIDENCE-MANIFEST.json')
write_json('EVIDENCE-MANIFEST.json',{'root':str(root),'date':'2026-10-07','scope':'10份当前文档及本轮证据，排除清单自身；不含既有ui文件','files':[{'path':str(path.relative_to(root)),'bytes':path.stat().st_size,'sha256':hashlib.sha256(path.read_bytes()).hexdigest()} for path in manifest_files]})
print(json.dumps({'exit_code':exit_code,'checks':len(results),'local_links':link_count,'manifest_files':len(manifest_files)},ensure_ascii=False))
sys.exit(exit_code)
