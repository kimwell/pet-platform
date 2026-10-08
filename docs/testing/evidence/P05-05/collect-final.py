"""只汇总本轮测试、产物与受保护文件；不读取环境或秘密配置。"""
import hashlib,json,pathlib,shutil,subprocess,xml.etree.ElementTree as ET,zipfile
root=pathlib.Path(__file__).resolve().parents[4]
e=root/'docs/testing/evidence/P05-05';target=root/'apps/backend/target';archive=e/'verify-final-reports'
for name in ['surefire-reports','failsafe-reports','p05-05-observations']:
    shutil.copytree(target/name,archive/name,dirs_exist_ok=True)
def suites(directory):
    total={k:0 for k in ['tests','failures','errors','skipped']};cases=set();detail=[]
    for file in sorted(directory.rglob('TEST-*.xml')):
        suite=ET.parse(file).getroot()
        for key in total:total[key]+=int(suite.attrib.get(key,0))
        for case in suite.findall('testcase'):cases.add((case.attrib['classname'],case.attrib['name']))
        detail.append({'suite':suite.attrib['name'],**{k:int(suite.attrib.get(k,0)) for k in total}})
    return total,cases,detail
unit,uc,ud=suites(archive/'surefire-reports');it,ic,idd=suites(archive/'failsafe-reports');current=uc|ic
baseline={}
for stage,folder in [('P05-04','verify-second-reports'),('P05-03','verify-final-reports')]:
    counts,cases,_=suites(root/'docs/testing/evidence'/stage/folder)
    baseline[stage]={'counts':counts,'retained':len(cases&current),'missing':[list(x) for x in sorted(cases-current)]}
summary={'unit':unit,'integration':it,'total':{k:unit[k]+it[k] for k in unit},'baseline':baseline,'newTests':len(current)-366,'suites':ud+idd,'scope':'真实PostgreSQL/Redis/Sa会话；微信Gateway测试边界替身，不是真实微信认证'}
(e/'test-summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
def sha(data):return hashlib.sha256(data).hexdigest()
jar=target/'pet-platform-backend-0.0.0-SNAPSHOT.jar'
shutil.copy2(jar,'/tmp/p05-05-final.jar')
test_classes={p.relative_to(target/'test-classes').as_posix() for p in (target/'test-classes').rglob('*.class')}
test_resources={p.relative_to(root/'apps/backend/src/test/resources').as_posix() for p in (root/'apps/backend/src/test/resources').rglob('*') if p.is_file()}
with zipfile.ZipFile(jar) as z:
    names=z.namelist();app=[n for n in names if n.startswith('BOOT-INF/classes/') and not n.endswith('/')]
    excluded=[n for n in app if n.removeprefix('BOOT-INF/classes/') in test_classes|test_resources]
    fixtures=['OnlyTestSecretInput','OnlyTestCode','OnlyTestOpenId','OnlyTestSessionKey','__customer-test','testWechatGateway','RESPONSES','wx9bcab67d52e2ee04']
    leaked=[{'entry':n,'marker':s} for n in app for s in fixtures if s.encode() in z.read(n)]
    probes=[n for n in names if any(s in n.lower() for s in ['probe','project.private.config','devtools','customerauthenticationit','wechatconfigtest'])]
    migrations={p.name:{'sourceSHA256':sha(p.read_bytes()),'jarMatches':p.read_bytes()==z.read('BOOT-INF/classes/db/migration/'+p.name)} for p in (root/'apps/backend/src/main/resources/db/migration').glob('V*.sql')}
protected={}
for path in ['pnpm-lock.yaml','AGENTS.md']+[str(p.relative_to(root)) for p in (root/'apps/backend/src/main/resources/db/migration').glob('V[123]__*.sql')]:
    before=subprocess.check_output(['git','show','HEAD:'+path],cwd=root);now=(root/path).read_bytes()
    protected[path]={'matchesHEAD':before==now,'SHA256':sha(now)}
ui_changes=subprocess.check_output(['git','status','--porcelain','--','ui'],cwd=root,text=True).splitlines()
audit={'jar':str(jar),'SHA256':sha(jar.read_bytes()),'testClassesOrResourcesPackaged':excluded,'technicalCredentialsOrLocalAppIdInOwnProductionResources':leaked,'probeEntries':probes,'migrations':migrations,'protectedFiles':protected,'uiChanges':ui_changes,'scope':'检查应用自有生产class/resource；WxJava依赖必须含协议字段，不将字段名等同于秘密值。实际AppSecret未配置，未读取秘密。'}
(e/'artifact-audit.json').write_text(json.dumps(audit,ensure_ascii=False,indent=2)+'\n')
assert summary['total']=={'tests':421,'failures':0,'errors':0,'skipped':0}
assert all(not b['missing'] for b in baseline.values()) and baseline['P05-04']['retained']==366 and baseline['P05-03']['retained']==328
assert not excluded and not leaked and not probes and not ui_changes
assert all(p['matchesHEAD'] for p in protected.values()) and all(p['jarMatches'] for p in migrations.values())
print(json.dumps({'tests':summary['total'],'baseline':baseline,'artifactSHA256':audit['SHA256'],'checks':'PASS'},ensure_ascii=False,indent=2))
