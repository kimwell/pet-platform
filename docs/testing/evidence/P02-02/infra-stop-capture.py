"""最终停止链路与事件记录，只操作本轮启动的验证项目。"""
import subprocess
import time
from runner import ENV, OUT, ROOT, run, save

args=['docker','events','--filter','label=com.docker.compose.project=pet-platform-p02-validation',
      '--filter','event=kill','--filter','event=die','--filter','event=stop','--filter','event=oom','--format','{{json .}}']
with (OUT/'rabbit-stop-final-events.log').open('w') as log:
    observer=subprocess.Popen(args,stdout=log,stderr=subprocess.STDOUT,env=ENV,cwd=ROOT)
    time.sleep(.5)
    result=run('infra-stop-final',['pnpm','stop:infra','--env-file','.local-data/p02-infra.env','--project','pet-platform-p02-validation'])
    time.sleep(.5)
    observer.terminate()
    code=observer.wait(timeout=5)
    save('rabbit-stop-final-events',{'cwd':str(ROOT),'command':args,'exit_code':code,'expected_exit_code':-15,
         'status':'PASS' if code==-15 else 'FAIL','evidence':'rabbit-stop-final-events.log',
         'boundary':'只读事件观察器由本轮有意SIGTERM结束；与容器主进程退出码分别记录'})
    assert result.returncode == 0
run('containers-final-r2',['docker','inspect','--format',
    '{{.Name}} Status={{.State.Status}} Exit={{.State.ExitCode}} OOM={{.State.OOMKilled}} Hostname={{.Config.Hostname}} StopTimeout={{.Config.StopTimeout}} {{json .Mounts}}',
    'pet-platform-p02-validation-postgres-1','pet-platform-p02-validation-redis-1','pet-platform-p02-validation-rabbitmq-1'])
run('volumes-final-r2',['docker','volume','ls','--format','{{.Name}} {{.Label "com.docker.compose.project"}}'])
