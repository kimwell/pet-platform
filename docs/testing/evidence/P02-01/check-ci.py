from pathlib import Path
import yaml,json,re
root=Path.cwd();d=yaml.load((root/'.github/workflows/check.yml').read_text(),Loader=yaml.BaseLoader);assert {'push','pull_request','workflow_dispatch'}==set(d['on']);rows=json.loads((root/'docs/testing/evidence/P02-01/ci-actions.json').read_text());mapping={r['repo']:r['sha'] for r in rows};manifest=json.loads((root/'package.json').read_text());commands=[]
for name,job in d['jobs'].items():
 assert job['runs-on']=='ubuntu-24.04'
 for step in job['steps']:
  if 'uses' in step:
   repo,sha=step['uses'].split('@');assert sha==mapping[repo] and len(sha)==40
  if 'run' in step:
   cmd=step['run'];commands.append(cmd)
   if cmd.startswith('pnpm ') and not cmd.startswith('pnpm install'):assert cmd.split()[1] in manifest['scripts']
   if './mvnw' in cmd:assert (root/step['working-directory']/'mvnw').exists()
print(json.dumps({'workflow_yaml':'valid','commands':commands,'remote_ci':'NOT_EXECUTED'},ensure_ascii=False))
