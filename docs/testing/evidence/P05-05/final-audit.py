"""收尾审计：文件、文档链接、日志秘密标记和既有证据保全；不读取环境。"""
import json,pathlib,re,subprocess,xml.etree.ElementTree as ET
root=pathlib.Path(__file__).resolve().parents[4];e=root/'docs/testing/evidence/P05-05'
tracked=subprocess.check_output(['git','diff','--name-only'],cwd=root,text=True).splitlines()
untracked=subprocess.check_output(['git','ls-files','--others','--exclude-standard'],cwd=root,text=True).splitlines()
source=sorted(p for p in set(tracked+untracked) if not p.startswith('docs/testing/evidence/P05-05/'))
(e/'changed-files.json').write_text(json.dumps({'modified':sorted(tracked),'added':sorted(p for p in untracked if p in source),'evidenceDirectory':'docs/testing/evidence/P05-05','evidenceFiles':len([p for p in untracked if p.startswith('docs/testing/evidence/P05-05/')])},ensure_ascii=False,indent=2)+'\n')
commands={}
for file in sorted(e.glob('*.json')):
    d=json.loads(file.read_text())
    if isinstance(d,dict) and 'argv' in d:commands[file.stem]=d
(e/'COMMAND-INDEX.json').write_text(json.dumps(commands,ensure_ascii=False,indent=2)+'\n')
missing=[]
for path in source:
    p=root/path
    if p.suffix!='.md':continue
    for ref in re.findall(r'\]\(([^)]+)\)',p.read_text()):
        ref=ref.split('#')[0].strip('<>')
        if not ref or re.match(r'[a-z]+:',ref) or ref.startswith('/'):continue
        if not (p.parent/ref).exists():missing.append({'file':path,'reference':ref})
before=subprocess.check_output(['git','status','--porcelain','--','docs/testing/evidence/P05-01','docs/testing/evidence/P05-02','docs/testing/evidence/P05-03','docs/testing/evidence/P05-04'],cwd=root,text=True).splitlines()
cases=set()
for file in (e/'verify-final-reports').rglob('TEST-*.xml'):
    for case in ET.parse(file).getroot().findall('testcase'):cases.add(case.attrib['name'])
coverage={
 '01 正式迁移/RLS':['versionThreeUpgradesWithoutChangingEarlierMigrationsOrCreatingCustomers','runtimeRlsHasNoContextNoRowsAndCannotReadOpenIdOrElevate'],
 '02 首次登录':['firstLoginCreatesAtomicFormalIdentityAndRealSession'],
 '03 再次复用':['repeatLoginReusesCustomerWithIndependentDevice'],
 '04 并发首次':['concurrentFirstRegistrationHasOneBindingAndNoOrphan','concurrentHttpLoginsNeverLeaveAnOrphanAndFreshCodeRecovers','nonUniqueDatabaseFailureRollsBackAndIsNotTreatedAsExisting'],
 '05 tenant/AppID/OpenID隔离':['tenantAppAndOpenIdAreAllPartOfBindingKey','compositeForeignKeyRefusesCustomerFromAnotherTenant'],
 '06 UnionID缺失':['missingUnionIdDoesNotBlockAndSameUnionDoesNotMerge'],
 '07 UnionID不合并':['missingUnionIdDoesNotBlockAndSameUnionDoesNotMerge'],
 '08 无效code':['invalidCodeCreatesNoCustomerOrSession'],
 '09 上游超时/错误':['timeoutRequiresFreshCodeAndNeverCreatesIdentity','upstreamAndMissingIdentityErrorsAreRecognizable','wrongApplicationResultIsRejected'],
 '10 停用拒绝':['disabledTenantNeverExchangesAndExistingSessionRevoked','disabledCustomerCannotBeRecreatedThroughLogin','disabledBindingCannotBeBypassed'],
 '11 会话失败恢复':['sessionCreationFailureKeepsIdentityAndFreshCodeCanRetry','auditWriteFailureCannotReturnSuccessfulToken'],
 '12 真实DB/Session当前身份':['currentIdentityComesFromDatabaseAndProviderSelfDomain'],
 '13 同UUID三域':['sameUuidAcrossThreeDomainsStillIsolated'],
 '14 退出/期限/状态':['currentLogoutInvalidatesOnlyCurrentCustomerDevice','allLogoutInvalidatesEveryOldCustomerDeviceAndFreshLoginWorks','absoluteAndIdlePoliciesDoNotSlideOnMe','idleExpiryRefusesExistingToken','customerAndTenantVersionInvalidateOldSessions','delayedCustomerCleanupNeverDeletesFreshGeneration','concurrentCustomerLogoutAllIncrementsVersionOnlyOnce'],
 '15 员工权限拒绝':['customerCannotAccessStaffOrPlatformAndReverseIsRejected'],
 '16 异步边界':['customerAsyncIsExplicitlyRejectedBeforeQueueing'],
 '17 频控/故障':['invalidInputsCannotConsumeUnlimitedGatewayCalls','exchangeRateIsDistributedAndLimitsFailureAsWellAsSuccess','actualRedisOutageFailsClosedAndRecoversExistingSession','actualDatabasePermissionFaultIs503AndDoesNotDeleteSession','redisCleanupFailureStillRevokesByDatabaseVersion'],
 '18 最小安全记录/生产无秘密':['eventsContainKnownIdsOnlyAndNeverExternalSecrets','customerProductionClassesContainNoTechnicalWechatCredentialsOrProbe','packagedJarContainsNoTestClassResourceMigrationOrDependency']}
unknown=[name for names in coverage.values() for name in names if name not in cases]
(e/'test-coverage.json').write_text(json.dumps({'scope':'真实PG/Redis/Sa/HTTP；外部微信替身仅测试包，不是真实微信认证','coverage':coverage,'missingTestNames':unknown},ensure_ascii=False,indent=2)+'\n')
patterns=[rb'OnlyTestSecretInput',rb'OnlyTestSessionKey',rb'Bearer [A-Za-z0-9_-]{16,256}',rb'"value"\s*:\s*"[A-Za-z0-9_-]{16,256}"',rb'jscode2session\?[^\s]*secret=']
log_hits=[]
for log in e.glob('*.log'):
    raw=log.read_bytes()
    hits=[i for i,p in enumerate(patterns) if re.search(p,raw)]
    if hits:log_hits.append({'file':log.name,'patternIndexes':hits})
diff=subprocess.run(['git','diff','--check'],cwd=root,capture_output=True,text=True)
result={'status':'PASS' if not(missing or before or unknown or log_hits or diff.returncode) else 'FAIL','missingLocalLinks':missing,'priorEvidenceChanges':before,'missingCoverageNames':unknown,'secretValuePatternsInCommandLogs':log_hits,'diffCheckExitCode':diff.returncode,'diffCheckOutput':diff.stdout+diff.stderr,'sourceChangedFiles':len(source),'limits':'日志扫描是已知秘密/Token/SDK请求模式检查，不证明外部日志基础设施；不读取实际秘密或历史私有配置。'}
(e/'final-audit-result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(result,ensure_ascii=False,indent=2))
assert result['status']=='PASS'
