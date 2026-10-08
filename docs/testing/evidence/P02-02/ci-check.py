"""CI 源配置检查与同命令本地配置验证；不代表远程 CI 运行。"""
import yaml
from runner import ROOT, run, save

workflow=yaml.safe_load((ROOT/'.github/workflows/check.yml').read_text())
frontend=workflow['jobs']['frontend']['steps']
backend=workflow['jobs']['backend']['steps']
commands=[item['run'] for item in frontend if 'run' in item]
assert all(command in commands for command in ['pnpm install --frozen-lockfile','pnpm check','pnpm build:web'])
infra=next(item for item in frontend if item.get('name')=='校验 Compose 配置')
assert infra['run']=='pnpm check:infra --env-file infra/local/.env.example'
assert backend[-1]['working-directory']=='apps/backend'
assert './mvnw --batch-mode clean verify' in backend[-1]['run']
assert run('ci-local-config-final',['pnpm','check:infra','--env-file','infra/local/.env.example'],overrides=infra['env']).returncode==0
save('ci-source-check-final',{'cwd':str(ROOT),'command':['python3','docs/testing/evidence/P02-02/ci-check.py'],
     'exit_code':0,'status':'PASS','summary':'YAML解析及根check/build/frozen install/infra配置/Wrapper工作目录一致',
     'remote_ci':'NOT_EXECUTED','windows_linux_runtime':'NOT_EXECUTED'})
