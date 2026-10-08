"""当前改动、引用与资源保全审计；不替代三端或业务运行验收。"""
import hashlib
import json
import os
import pathlib
import re
import socket
import subprocess
import urllib.parse

ROOT=pathlib.Path(__file__).resolve().parents[4]
OUT=pathlib.Path(__file__).parent
before=json.loads((OUT/'inventory-before.json').read_text())
skip={'.git','node_modules','dist','target','coverage','.local-data','miniprogram_npm','.cache','.vite','__pycache__','.delivery-os'}
after={}
for base,dirs,names in os.walk(ROOT):
    dirs[:]=[d for d in dirs if d not in skip and not (pathlib.Path(base)/d).is_symlink()]
    for name in names:
        p=pathlib.Path(base)/name
        if p.is_symlink() or name=='.env' or (name.startswith('.env.') and not name.endswith('.example')) or name=='project.private.config.json':
            continue
        rel=p.relative_to(ROOT).as_posix()
        if rel.startswith('docs/testing/evidence/P02-02/'):
            continue
        after[rel]={'size':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
deleted=sorted(set(before)-set(after))
changed=sorted(p for p in before if p in after and before[p]!=after[p])
new=sorted(set(after)-set(before))
allowed={'README.md','package.json','.github/workflows/check.yml','infra/local/docker-compose.yml','infra/local/README.md',
    'apps/admin-web/package.json','apps/admin-web/vite.config.ts','apps/admin-web/src/app/providers/AppProviders.tsx',
    'apps/backend/src/main/java/com/pet/platform/Application.java',
    'apps/backend/src/main/java/com/pet/platform/shared/config/EnvironmentSettings.java',
    'apps/backend/src/main/resources/application.yml','apps/backend/src/main/resources/application-prod.yml',
    'apps/backend/src/test/java/com/pet/platform/ApplicationTest.java',
    'docs/development/LOCAL-DEVELOPMENT.md','docs/development/ROADMAP.md','docs/development/DECISION-LOG.md',
    'docs/testing/ACCEPTANCE-MATRIX.md','docs/architecture/CONFIGURATION.md','docs/architecture/PROJECT-STRUCTURE.md'}
assert not deleted,deleted
assert set(changed)<=allowed,changed
assert set(new)=={'scripts/local-infra.mjs','apps/admin-web/dev-config.ts','apps/admin-web/src/app/router/dev-config.test.ts','docs/testing/P02-02-VERIFICATION.md'},new
preserved={}
for label,predicate in {
    'ui':lambda p:p.startswith('ui/'),
    'P01':lambda p:p.startswith('docs/testing/evidence/P01-') or p.startswith('docs/testing/P01-'),
    'P02-01':lambda p:p.startswith('docs/testing/evidence/P02-01/') or p=='docs/testing/P02-01-VERIFICATION.md',
    'frozen':lambda p:p in ['pnpm-lock.yaml','docs/development/VERSION-MATRIX.md','.nvmrc','pnpm-workspace.yaml']
}.items():
    files=[p for p in before if predicate(p)]
    assert all(before[p]==after[p] for p in files),label
    preserved[label]=len(files)
links=[]
for p in [ROOT/'README.md',*ROOT.joinpath('docs').rglob('*.md'),ROOT/'infra/local/README.md']:
    for target in re.findall(r'\]\(([^)\n]+)\)',p.read_text()):
        target=target.strip('<>')
        if '://' in target or target.startswith('#'):continue
        path=urllib.parse.unquote(target.split('#')[0])
        if path and not (p.parent/path).exists():links.append({'file':str(p.relative_to(ROOT)),'target':target})
assert not links,links
vol_before={x.split()[0] for x in (OUT/'volumes-before.log').read_text().splitlines()}
vol_after=subprocess.check_output(['docker','volume','ls','--format','{{.Name}}'],text=True).splitlines()
assert vol_before==set(vol_after),'已有卷清单改变'
containers=subprocess.check_output(['docker','ps','-a','--format','{{.Names}} {{.State}}'],text=True).splitlines()
names={x.split()[0] for x in containers}
original={x.split()[1] for x in (OUT/'containers-before.log').read_text().splitlines()}
assert names==original,'容器名称或其他项目资源改变'
assert all(x.split()[1]=='exited' for x in containers),'本轮启动资源未恢复停止'
ports={}
for port in [8080,5173,4173,25432,26379,25672,25673]:
    with socket.socket() as sock:ports[str(port)]=sock.connect_ex(('127.0.0.1',port))==0
assert not any(ports.values()),ports
for path in ['infra/local/.env','apps/admin-web/.env.local','apps/wechat-miniprogram/project.private.config.json']:
    assert not (ROOT/path).exists(),'本轮不得新增日常秘密文件'
ignored=subprocess.run(['git','check-ignore','infra/local/.env','apps/admin-web/.env.local','apps/wechat-miniprogram/project.private.config.json'],capture_output=True,text=True)
assert ignored.returncode==0 and len(ignored.stdout.splitlines())==3
backend_files=list((ROOT/'apps/backend/src/main').rglob('*'))
assert not any(p.suffix=='.sql' or p.name.endswith('Controller.java') or p.name.endswith('Repository.java') for p in backend_files)
assert 'package com.pet.platform;' in (ROOT/'apps/backend/src/main/java/com/pet/platform/Application.java').read_text()
for name in ['root-check-final-r2','web-build-final','backend-verify-r3','scripts-lint','final-stop-chain','ci-local-config','mini-offline-npm','configuration-negatives','backend-profile-mix-after','backend-profile-mismatch-after']:
    record=json.loads((OUT/(name+'.json')).read_text());assert record['status']=='PASS',name
for name in ['web-dev-console.json','web-preview-console.json']:
    assert json.loads((OUT/name).read_text())==[]
legacy=json.loads((OUT/'rabbit-stop-r2-events.json').read_text())
events=[json.loads(line) for line in (OUT/'rabbit-stop-final-events.log').read_text().splitlines()]
assert not any(x['Action']=='oom' or x['Actor']['Attributes'].get('signal')=='9' for x in events)
rabbit=[x for x in events if x['Actor']['Attributes'].get('com.docker.compose.service')=='rabbitmq']
assert any(x['Action']=='die' and x['Actor']['Attributes'].get('exitCode')=='0' for x in rabbit)
node_before=(OUT/'rabbit-node-name-before.log').read_text().strip()
node_after=(OUT/'rabbit-node-name-after.log').read_text().strip()
assert node_before==node_after=='rabbit@rabbitmq'
assert (OUT/'rabbit-identity-before.log').read_text().split()[0]!=(OUT/'rabbit-identity-after.log').read_text().split()[0]
report={'cwd':str(ROOT),'command':['python3','docs/testing/evidence/P02-02/audit.py'],'exit_code':0,'status':'PASS',
    'deleted':deleted,'changed':changed,'new':new,'preserved_counts':preserved,'links_missing':links,
    'volumes_before_count':len(vol_before),'volumes_after_count':len(vol_after),'containers':containers,
    'ports_occupied':ports,'private_paths_ignored':True,'no_added_secret_files':True,
    'final_rabbit_no_sigkill_oom':True,'stable_rabbit_node':node_after,
    'scope':'工程配置与源码/证据保全；未实现业务/DB/认证/多租户；未执行微信工具/真机/远程CI/其他OS'}
(OUT/'final-audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({k:report[k] for k in ['status','preserved_counts','volumes_before_count','volumes_after_count','ports_occupied']},ensure_ascii=False))
