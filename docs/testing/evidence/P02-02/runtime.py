"""只启动和停止本轮记录的进程组；真实 HTTP 404 是工程壳的预期响应。"""
import json
import os
import pathlib
import signal
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request
from runner import ENV, OUT, ROOT, run, save

def request(url, accept='text/html'):
    try:
        response=urllib.request.urlopen(urllib.request.Request(url,headers={'Accept':accept}),timeout=5)
    except urllib.error.HTTPError as error:
        response=error
    return {'url':url,'status':response.code,'content_type':response.headers.get('Content-Type'),'body':response.read().decode()[:1500]}

def start(name, args, cwd, port):
    log=(OUT/(name+'.log')).open('w')
    process=subprocess.Popen(args,cwd=cwd,env=ENV,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
    save(name,{'cwd':str(cwd),'command':args,'pid':process.pid,'port':port,'status':'STARTING','exit_code':None,'evidence':name+'.log'})
    for _ in range(120):
        if process.poll() is not None:
            raise RuntimeError(name+' 进程提前结束，退出码 '+str(process.returncode))
        with socket.socket() as sock:
            if sock.connect_ex(('127.0.0.1',port)) == 0:
                save(name,{'cwd':str(cwd),'command':args,'pid':process.pid,'port':port,'status':'PASS','exit_code':None,'evidence':name+'.log','boundary':'长驻进程启动观察，不伪造命令退出码'})
                print(name+' started pid='+str(process.pid),flush=True)
                return
        time.sleep(.25)
    raise RuntimeError(name+' 未在限时内监听')

def stop(name):
    record=json.loads((OUT/(name+'.json')).read_text()); pid=record['pid']
    args=['ps','-o','pid=,pgid=,command=','-g',str(pid)]
    before=subprocess.run(args,capture_output=True,text=True)
    save(name+'-group-before',{'command':args,'exit_code':before.returncode,'output':before.stdout})
    os.killpg(pid,signal.SIGTERM)
    for _ in range(100):
        with socket.socket() as sock:
            listening=sock.connect_ex(('127.0.0.1',record['port']))==0
        remaining=subprocess.run(args,capture_output=True,text=True)
        active=[line for line in remaining.stdout.splitlines() if '<defunct>' not in line]
        if not listening and not active:
            save(name+'-stop',{'cwd':str(ROOT),'steps':['向本轮进程组 '+str(pid)+' 发送 SIGTERM','确认监听端口关闭','ps 确认进程组无活动进程'],'status':'PASS','exit_code':None,'process_group':pid,'port':record['port'],'remaining':remaining.stdout})
            print(name+' stopped',flush=True)
            return
        time.sleep(.2)
    raise RuntimeError(name+' 停止后仍有监听或活动进程；不以 SIGKILL 伪装正常停止')

if __name__ == '__main__' and sys.argv[1]=='start':
    start('backend-dev',['./mvnw','spring-boot:run','-Dspring-boot.run.profiles=local'],ROOT/'apps/backend',8080)
    start('web-dev',['pnpm','dev:web'],ROOT,5173)
    start('web-preview',['pnpm','preview:web'],ROOT,4173)
    checks=[request('http://127.0.0.1:8080/', 'application/json'),request('http://127.0.0.1:8080/missing','application/json'),request('http://127.0.0.1:5173/'),request('http://127.0.0.1:5173/missing/deep'),request('http://127.0.0.1:5173/api/p02-02-missing','application/json'),request('http://127.0.0.1:4173/'),request('http://127.0.0.1:4173/missing/deep')]
    assert [x['status'] for x in checks]==[404,404,200,200,404,200,200]
    save('startup-http',{'cwd':str(ROOT),'command':['python3',str(pathlib.Path(__file__)),'start'],'exit_code':0,'status':'PASS','checks':checks,'proxy_boundary':'/api/p02-02-missing 真实透传后端 404，没有正式业务接口'})
    print('真实 HTTP 与开发代理检查通过')
elif __name__ == '__main__' and sys.argv[1]=='stop':
    for name in ['web-preview','web-dev','backend-dev']:stop(name)
