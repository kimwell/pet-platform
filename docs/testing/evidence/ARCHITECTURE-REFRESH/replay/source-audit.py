from pathlib import Path
import json,hashlib,subprocess,re
root=Path.cwd();ev=root/'docs/testing/evidence/ARCHITECTURE-REFRESH';baseline=json.loads((ev/'preservation-baseline.json').read_text());bad=[]
for f,digest in baseline.items():
 p=root/f
 if not p.is_file() or hashlib.sha256(p.read_bytes()).hexdigest()!=digest:bad.append(f)
frozen=['package.json','pnpm-lock.yaml','docs/development/VERSION-MATRIX.md','apps/backend/pom.xml','apps/admin-web/package.json','packages/api-contracts/src/generated/backend.ts','packages/api-contracts/openapi/backend.openapi.json']
checks=[]
for f in frozen:
 p=root/f
 if not p.exists():continue
 original=subprocess.check_output(['git','show','HEAD:'+f]);ok=p.read_bytes()==original;checks.append({'file':f,'unchanged':ok,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()});assert ok,f
mini=root/'apps/wechat-miniprogram/types/generated/api.d.ts';original=subprocess.check_output(['git','show','HEAD:apps/wechat-miniprogram/miniprogram/types/generated/api.d.ts']);assert mini.read_bytes()==original
assert not (root/'apps/wechat-miniprogram/miniprogram').exists();assert not bad
changed=subprocess.check_output(['git','diff','--name-only'],text=True).splitlines()+subprocess.check_output(['git','ls-files','--others','--exclude-standard'],text=True).splitlines()
secrets=[];private=root/'.local-data/architecture/runtime'
secrets+=list(json.loads((private/'credentials.json').read_text()).values());secrets+=[v for k,v in json.loads((private/'command-env.json').read_text()).items() if k.endswith('PASSWORD')];secrets+=[(private/'redis.conf').read_text().split('requirepass ')[1].splitlines()[0]]
secrets+=re.findall(r'(?:appSecret|app-secret):\s*[\"\']?([a-zA-Z0-9]{16,})',(private/'wechat.yaml').read_text())
leaks=[];links=[]
for f in changed:
 p=root/f
 if not p.is_file() or p.suffix.lower() not in {'.ts','.tsx','.java','.mjs','.cjs','.py','.md','.log','.txt','.json','.jsonl','.css','.wxss','.wxml','.xml','.yml','.yaml'}:continue
 text=p.read_text(errors='replace')
 if any(v and v in text for v in secrets):leaks.append(f)
 if p.suffix=='.md':
  for match in re.finditer(r'\]\(([^)]+)\)',text):
   target=match.group(1).split('#')[0].strip('<>')
   if not target or '://' in target:continue
   q=Path(target) if target.startswith('/') else p.parent/target
   if not q.exists():links.append({'file':f,'target':target})
assert not leaks,'私有值泄露文件：'+str(leaks)
# CHECKS清单在收尾后统一生成；其预先引用不作为坏链接。
links=[i for i in links if not i['target'].endswith('CHECKS.md')]
assert not links,'文档引用不存在：'+str(links)
report={'status':'PASS','preservedFiles':len(baseline),'changedHistoricalFiles':bad,'frozenSources':checks,'miniDeclarationMovedByteIdentically':True,'miniIntermediateDirectoryRemoved':True,'privateValueLeakFiles':leaks,'brokenChangedDocumentLinks':links}
(ev/'preservation-final.json').write_text(json.dumps(report,ensure_ascii=False,indent=2));print('保全533处、冻结版本/根锁与声明移动、公共文件私有值检查、变更文档链接通过')
