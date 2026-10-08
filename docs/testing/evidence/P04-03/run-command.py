from pathlib import Path
import subprocess, os, json, sys, datetime, shutil
root = Path(__file__).resolve().parents[4]
ev = Path(__file__).resolve().parent
name, *args = sys.argv[1:]
cwd = root/'apps/backend'
env = os.environ.copy()
env['JAVA_HOME'] = json.loads((root/'docs/testing/evidence/P04-01/toolchain.json').read_text())['JAVA_HOME']
started = datetime.datetime.now(datetime.timezone.utc).isoformat()
with (ev/f'{name}.log').open('w') as log:
 result = subprocess.run(args, cwd=cwd, env=env, stdout=log, stderr=subprocess.STDOUT)
summary = (ev/f'{name}.log').read_text()[-5000:]
(ev/f'{name}.json').write_text(json.dumps(dict(cwd=str(cwd),argv=args,exitCode=result.returncode,startedAtUTC=started,finishedAtUTC=datetime.datetime.now(datetime.timezone.utc).isoformat(),JAVA_HOME=env['JAVA_HOME'],log=f'{name}.log',summary=summary),ensure_ascii=False,indent=2))
if name.startswith('verify'):
 for directory in ['surefire-reports','failsafe-reports','p04-02-database-observations','p04-03-database-observations']:
  source=cwd/'target'/directory
  if source.exists(): shutil.copytree(source,ev/f'{name}-reports'/directory,dirs_exist_ok=True)
print(summary)
print('exitCode', result.returncode)
sys.exit(result.returncode)
