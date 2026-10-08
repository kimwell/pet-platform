#!/usr/bin/env python3
"""只读复核真实续验结果/保全/安全字段；不请求微信、不重跑完整测试。"""
from pathlib import Path
from datetime import datetime
from zoneinfo import ZoneInfo
import ast
import hashlib
import json
import os
import re
import socket
import subprocess

ROOT = Path(__file__).resolve().parents[5]
BASE = Path(__file__).resolve().parent
FINAL = BASE / 'real-run-second'
NODE = '/var/folders/5y/tt1690cx7k1gcq12252yf5rh0000gn/T/enterprise-scaffold-p01-01-plwt53cm/tools/node-v24.21.0-darwin-arm64/bin/node'
DOCS = [ROOT / x for x in ('docs/testing/P05-05-VERIFICATION.md', 'docs/testing/P05-ACCEPTANCE.md', 'docs/testing/ACCEPTANCE-MATRIX.md', 'docs/development/LOCAL-DEVELOPMENT.md', 'docs/development/ROADMAP.md')]
JAR_SHA = '783dce595d9c45fe275db083093b625b9fdfe670debdfb56f030f6bf1ed0581f'

def now():
    return datetime.now(ZoneInfo('Asia/Shanghai')).isoformat()

def save(name, value):
    (FINAL / name).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')

def command(label, argv):
    start = now()
    result = subprocess.run(argv, cwd=ROOT, capture_output=True, text=True)
    audit['commands'].append({'label': label, 'cwd': str(ROOT), 'argv': argv, 'startedAt': start, 'finishedAt': now(), 'exitCode': result.returncode, 'summary': '通过' if result.returncode == 0 else '失败；不采集原文'})
    assert result.returncode == 0, label
    return result.stdout

audit = {'cwd': str(ROOT), 'startedAt': now(), 'status': 'RUNNING', 'commands': [], 'checks': {}}
save('final-audit.json', audit)
save('stage-review.json', {'status': 'PENDING_REVIEW'})
try:
    real = json.loads((FINAL / 'real-wechat-result.json').read_text())
    isolation = json.loads((FINAL / 'isolation-preparation.json').read_text())
    restored = json.loads((FINAL / 'tool-restoration.json').read_text())
    steps = {x['name']: x for x in real['steps']}
    assert real['status'] == 'PASS' and real['fourSteps'] == 'RUNTIME_VERIFIED'
    for label in ('realWechatLogin', 'currentIdentity', 'currentLogout', 'repeatWechatLogin', 'repeatSessionCleanupLogout'):
        assert steps[label]['httpStatus'] == 200
    assert steps['oldTokenAfterLogout']['httpStatus'] == 401 and steps['oldTokenAfterLogout']['errorCode'] == 'SESSION_EXPIRED'
    assert real['oldSessionInvalidated'] and real['repeatCustomerReuse'] == 'RUNTIME_VERIFIED'
    assert real['customerIdentity']['principalType'] == 'CUSTOMER' and real['customerIdentity']['scope'] == 'SELF'
    assert not real['customerIdentity']['staffPermissions'] and not real['customerIdentity']['platformPermissions']
    for relation in (real['databaseRelation'], real['repeatDatabaseRelation']):
        assert all(relation[x] == 1 for x in ('customerCount', 'bindingCount', 'activeCustomerForTenant', 'correctActiveBinding'))
        assert not relation['openIdSelected'] and not relation['unionIdSelected']
    assert real['appIdMatches'] and real['libraryVersion'] == '3.17.2' and real['upstreamAttempts'] == 2
    assert isolation['checks']['productionGateway']['upstreamExchange'] == 'RUNTIME_VERIFIED'
    assert isolation['checks']['productionGateway']['testOverrides'] is False
    assert all(isolation['checks']['packagedMigrationMatches'].values())
    assert isolation['cleanup']['existingContainersStatePreserved']
    assert not restored['servicePortAfter'] and not restored['loginTicketAllowed'] and not restored['globalDefaultTrustProjects']
    assert real['probeClosed'] and restored['originalProjectWindowRestored']
    for port in (46904, 9420):
        with socket.socket() as check_socket:
            check_socket.settimeout(1)
            assert check_socket.connect_ex(('127.0.0.1', port)) != 0
    assert not list((ROOT / '.local-data/p05-05-wechat').glob('p05-05-isolated-input-*'))
    audit['checks']['realFourStepsAndRepeatReuse'] = 'PASS'
    audit['checks']['resourceAndToolRestoration'] = 'PASS'

    secret_file = ROOT / '.local-data/p05-05-wechat/secrets/PET_WECHAT_LOCAL_SECRET'
    assert secret_file.is_file() and os.access(secret_file, os.R_OK) and secret_file.stat().st_size > 0
    assert secret_file.stat().st_mode & 0o777 == 0o600
    assert secret_file.parent.stat().st_mode & 0o777 == 0o700
    command('秘密文件Git忽略', ['git', 'check-ignore', str(secret_file)])
    # 仅对本轮文档/证据作已知秘密值内存比较；只报告布尔结果，不输出值/摘要/匹配行。
    secret = secret_file.read_bytes().strip()
    scoped_files = DOCS + [x for x in BASE.rglob('*') if x.is_file()]
    assert not any(secret in x.read_bytes() for x in scoped_files)
    secret = None
    audit['checks']['secretExistsReadableNonemptyIgnored600'] = True
    audit['checks']['knownSecretAbsentFromNewEvidenceAndDocs'] = True

    forbidden = {'secret', 'appsecret', 'code', 'token', 'session_key', 'openid', 'unionid', 'cookie', 'authorization', 'x-customer-token'}
    def inspect_fields(value):
        if isinstance(value, dict):
            assert not any(key.lower() in forbidden for key in value)
            for child in value.values():
                inspect_fields(child)
        elif isinstance(value, list):
            for child in value:
                inspect_fields(child)
    inspect_fields(real)
    audit['checks']['realResultContainsOnlyNoncredentialFields'] = True

    changed = command('已跟踪变更边界', ['git', 'diff', '--name-only']).splitlines()
    allowed = {str(x.relative_to(ROOT)) for x in DOCS}
    assert set(changed) <= allowed
    audit['checks']['onlyFiveTrackedDocumentsChanged'] = True
    protected = command('原阶段与P05-05历史证据保全', ['git', 'diff', '--name-only', '--', 'docs/testing/evidence', 'apps', 'packages', 'infra', 'scripts', 'ui', 'AGENTS.md', 'pnpm-lock.yaml', 'package.json']).splitlines()
    assert not protected
    audit['checks']['backendContractsDependenciesUiPriorEvidenceUnchanged'] = True
    jar = ROOT / 'apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar'
    assert hashlib.sha256(jar.read_bytes()).hexdigest() == JAR_SHA
    audit['checks']['productionJarSHA256'] = JAR_SHA
    for source in BASE.rglob('*.py'):
        ast.parse(source.read_text(), filename=str(source))
    command('真实执行器Node语法', [NODE, '--check', str(BASE / 'safe-real-wechat.cjs')])
    command('仓库单锁固定版本及秘密特征', [NODE, 'scripts/check-repository.mjs'])
    command('正式小程序结构及配置', [NODE, 'scripts/check-miniprogram.mjs'])
    command('差异格式', ['git', 'diff', '--check'])
    audit['checks']['pythonSyntax'] = 'PASS'
    audit['checks']['missingLocalLinks'] = []
    for document in DOCS:
        for match in re.finditer(r'\]\(([^)]+)\)', document.read_text()):
            link = match.group(1).split('#')[0].split()[0]
            if link.startswith(('http:', 'https:', 'app:', 'codex:')):
                continue
            if link and not (document.parent / link).exists():
                audit['checks']['missingLocalLinks'].append({'document': str(document.relative_to(ROOT)), 'target': link})
    assert not audit['checks']['missingLocalLinks']
    old = json.loads((ROOT / 'docs/testing/evidence/P05-05/test-summary.json').read_text())
    assert old['total'] == {'tests': 421, 'failures': 0, 'errors': 0, 'skipped': 0}
    assert all(not x['missing'] for x in old['baseline'].values())
    audit['checks']['historical421PreservedNotRepeated'] = True
    audit['status'] = 'PASS'
    review = {'recordedAt': now(), 'status': 'PASS', 'G12': 'PASS', 'P05-05': 'COMPLETE', 'P05': 'COMPLETE', 'steps': {name: 'RUNTIME_VERIFIED' for name in ('realCodeExchange', 'customerSession', 'currentIdentity', 'logoutInvalidation', 'repeatCustomerReuse')}, 'executionEvidence': 'real-wechat-result.json', 'environmentEvidence': 'isolation-preparation.json', 'toolRestorationEvidence': 'tool-restoration.json', 'environmentTopLevelSteps': '原执行快照的启动前初始化字段未更新，当前步骤状态由真实执行结果复核得到；原快照不回写。', 'historical421Tests': old['total'], 'fullVerifyRepeated': False, 'backendCodeChanges': False, 'publicContractChanges': False, 'productionDependencyChanges': False, 'remainingP05RequiredWork': [], 'remainingIndependentLimits': ['P07完整员工/组织/角色权限管理API和页面仍必选未实现', 'P09完整小程序交互/隐私/真机', 'P10消息支付', '生产部署/TLS/代理/跨源/数据库及Redis配置', '远程CI及Windows/Linux'], 'humanActionRecommended': '聊天中提供过的凭据由用户在微信管理后台轮换，并自行更新既定安全文件/私有配置版本后重启；不在聊天提供新值。', 'nextSuggestedTask': 'P06-01 Web应用壳、请求与同源会话基础', 'nextTaskStarted': False, 'commitPushDeployPerformed': False}
    save('stage-review.json', review)
except Exception as failure:
    audit['status'] = 'FAIL'
    audit['failureCategory'] = type(failure).__name__
finally:
    audit['finishedAt'] = now()
    save('final-audit.json', audit)
    print(json.dumps({'status': audit['status'], 'evidence': str(FINAL / 'final-audit.json')}, ensure_ascii=False))
    if audit['status'] != 'PASS':
        raise SystemExit(1)
