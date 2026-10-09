import pathlib,subprocess,json,hashlib,re,datetime
root=pathlib.Path('/Users/kimwell/work/pet-platform');e=root/'docs/testing/evidence/P07-01'
before=json.loads((e/'before-manifest.json').read_text())
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
missing=[p for p in before if not (root/p).is_file()]
changed={p:sha(root/p) for p,old in before.items() if (root/p).is_file() and sha(root/p)!=old}
raw=subprocess.check_output(['git','ls-files','-z','--cached','--others','--exclude-standard'],cwd=root).decode().split('\0')
paths=sorted({p for p in raw if p and not p.startswith('docs/testing/evidence/P07-01/') and (root/p).is_file()})
added={p:sha(root/p) for p in paths if p not in before}
protected={
 'ui':lambda p:p.startswith('ui/'),
 'adminWebAll':lambda p:p.startswith('apps/admin-web/'),
 'historicalEvidence':lambda p:p.startswith('docs/testing/evidence/'),
 'P06Reports':lambda p:p in ('docs/testing/P06-ACCEPTANCE.md','docs/testing/P06-03-VERIFICATION.md'),
 'existingUserOtherDocs':lambda p:p in ('docs/conventions/WEB-PAGES.md','docs/conventions/WEB-STATE.md','docs/development/LOCAL-DEVELOPMENT.md'),
 'frozenDependencies':lambda p:p.endswith('package.json') or p in ('pnpm-lock.yaml','apps/backend/pom.xml','.nvmrc','docs/development/VERSION-MATRIX.md'),
 'oldMigrations':lambda p:p.startswith('apps/backend/src/main/resources/db/migration/V') and pathlib.Path(p).name[:2] in ('V1','V2','V3','V4')}
protection={}
for name,match in protected.items():
 items=[p for p in before if match(p)]
 bad=[p for p in items if p in changed or p in missing]
 protection[name]={'checked':len(items),'changedOrMissing':bad};assert not bad,(name,bad)
assert not missing,missing
allowed={'README.md','scripts/backend-identity.sh','docs/architecture/AUTHORIZATION.md','docs/architecture/MODULE-BOUNDARIES.md','docs/contracts/API.md','docs/contracts/IDENTITY.md','docs/conventions/PERSISTENCE.md','docs/development/ROADMAP.md','docs/development/DECISION-LOG.md','docs/testing/ACCEPTANCE-MATRIX.md','docs/contracts/EMPLOYEE-MANAGEMENT.md','docs/testing/P07-01-VERIFICATION.md','packages/api-contracts/openapi/backend.openapi.json','packages/api-contracts/src/generated/api.d.ts','apps/wechat-miniprogram/miniprogram/types/generated/api.d.ts'}
allow=lambda p:p in allowed or p.startswith('apps/backend/src/main/java/com/pet/platform/') or p.startswith('apps/backend/src/test/java/com/pet/testing/') or p=='apps/backend/src/main/resources/db/migration/V5__employee_read_contract.sql'
assert all(allow(p) for p in (*changed,*added)),[p for p in (*changed,*added) if not allow(p)]
manifest={'base':'before-manifest.json (包含进入任务前的未提交成果)','modified':changed,'added':added,'sourceFileCount':len(changed)+len(added),'excluded':'本轮evidence文件单独保留，不在源码变更清单；没有列入此前用户已修改但本轮未动文件'}
(e/'file-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
# 本轮新增/修改文档的全部相对本地链接；路径校验，不冒充外部网站可达性。
links=[];broken=[];deferred=[]
for path in sorted(p for p in (*changed,*added) if p.endswith('.md')):
 for target in re.findall(r'\]\(([^)]+)\)',(root/path).read_text()):
  target=target.split()[0].strip('<>')
  if target.startswith(('http:','https:','codex:','app:','#','mailto:')):continue
  base=target.split('#',1)[0]
  if not base:continue
  resolved=(root/path).parent/base
  links.append({'source':path,'target':target})
  if not resolved.exists():
   item={'source':path,'target':target}
   if resolved.absolute() in ((e/'command-index.json').absolute(),(e/'final-audit.json').absolute(),(e/'audit-complete.json').absolute()):deferred.append(resolved)
   else:broken.append(item)
assert not broken,broken
check=subprocess.run(['git','diff','--check'],cwd=root,capture_output=True,text=True);assert check.returncode==0,check.stdout+check.stderr
assert json.loads((e/'test-summary.json').read_text())['originalCasesRetained']
required=('verify-final','contracts-generate','contracts-check','api-types','web-types','mini-types','contracts-tests','repo-check','toolchain-final','snapshot-final','package-final','package-check','artifact-final')
for name in required: assert json.loads((e/(name+'.json')).read_text())['exitCode']==0,name
# 环境/结构/生产验证的实际边界，未检查或复制忽略的秘密私有配置。
result={'date':datetime.datetime.now(datetime.timezone.utc).isoformat(),'cwd':str(root),'sourceModified':len(changed),'sourceAdded':len(added),'missingInitialFiles':missing,'protectedGroups':protection,'localLinksChecked':len(links),'brokenLocalLinks':broken,'gitDiffCheck':{'command':['git','diff','--check'],'exitCode':check.returncode,'output':check.stdout+check.stderr},'requiredCommandsAllZero':True,'formalTests':455,'historicalBaselineRetained':421,'newTests':34,'privateConfiguration':'未读取/改写忽略的秘密配置；不声称对它们完成哈希比较','productionDeploy':'NOT_EXECUTED','nextTask':'P07-02 NOT_STARTED','P07-01':'COMPLETE','P07':'IN_PROGRESS'}
(e/'final-audit.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
records=[]
for path in sorted(e.glob('*.json')):
 d=json.loads(path.read_text())
 if 'command' in d and 'exitCode' in d:
  records.append({'name':path.stem,**d})
(e/'command-index.json').write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n')
assert all(path.exists() for path in deferred if path.name!='audit-complete.json')
(e/'after-status.txt').write_bytes(subprocess.check_output(['git','status','--short'],cwd=root))
print(json.dumps({'modified':len(changed),'added':len(added),'missing':len(missing),'protected':{n:d['checked'] for n,d in protection.items()},'links':len(links),'diffExit':check.returncode,'result':'PASS'},ensure_ascii=False))
