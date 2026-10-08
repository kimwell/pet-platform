import json,pathlib,subprocess,os,datetime
root=pathlib.Path('/Users/kimwell/work/pet-platform');e=root/'docs/testing/evidence/P05-04'
jdk=json.loads((root/'docs/testing/evidence/P05-01/toolchain.json').read_text())['JAVA_HOME']
cp='/tmp/pet-p05-04-browser-classes/main:/tmp/pet-p05-04-browser-classes/test:'+pathlib.Path('/tmp/pet-p05-04-classpath').read_text().strip()
argv=[jdk+'/bin/java','--class-path',cp,'/tmp/P05PlatformBrowserHarness.java'];start=datetime.datetime.now(datetime.timezone.utc).isoformat()
with (e/'browser-harness.log').open('w') as f:r=subprocess.run(argv,cwd=root/'apps/backend',stdout=f,stderr=subprocess.STDOUT)
meta=dict(cwd=str(root/'apps/backend'),command='java --class-path <冻结测试依赖与编译类> /tmp/P05PlatformBrowserHarness.java',exitCode=r.returncode,startedAtUTC=start,finishedAtUTC=datetime.datetime.now(datetime.timezone.utc).isoformat(),log='browser-harness.log')
(e/'browser-harness.json').write_text(json.dumps(meta,ensure_ascii=False,indent=2));print(json.dumps(meta,ensure_ascii=False));raise SystemExit(r.returncode)
