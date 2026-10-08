"""实际基础设施技术连接探针：不建表、不发送业务消息、不输出凭据。"""
import base64
import json
import os
import pathlib
import socket
import struct
import subprocess
import time
import urllib.error
import urllib.request
from runner import COMPOSE, ENV, OUT, ROOT, run, save

def frame(sock):
    header = sock.recv(7)
    while len(header) < 7:
        header += sock.recv(7-len(header))
    kind, channel, size = struct.unpack('>BHI', header)
    payload = b''
    while len(payload) < size+1:
        chunk = sock.recv(size+1-len(payload))
        if not chunk:
            raise RuntimeError('AMQP 连接意外关闭')
        payload += chunk
    assert kind == 1 and channel == 0 and payload[-1] == 206
    return payload[:-1]

def send(sock, method, fields):
    payload = struct.pack('>HH', 10, method) + fields
    sock.sendall(struct.pack('>BHI', 1, 0, len(payload)) + payload + b'\xce')

config = {}
for line in (ROOT / '.local-data/p02-infra.env').read_text().splitlines():
    if '=' in line and not line.startswith('#'):
        key, value = line.split('=', 1)
        config[key] = value.strip("'\"")
run('infra-healthy', COMPOSE+['ps','--format','json'])
pg = run('postgres-auth', COMPOSE+['exec','-T','postgres','sh','-ec',
    'PGPASSWORD="$POSTGRES_PASSWORD" psql -h host.docker.internal -p '+config['POSTGRES_PORT']+' -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -Atc "SELECT current_database(), current_user, 1"'])
assert pg.returncode == 0 and '|1' in pg.stdout
redis = run('redis-auth', COMPOSE+['exec','-T','redis','sh','-ec',
    'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli -h host.docker.internal -p '+config['REDIS_PORT']+' ping'])
assert redis.stdout.strip() == 'PONG'
redis_no = run('redis-no-auth', COMPOSE+['exec','-T','redis','redis-cli','-h','host.docker.internal','-p',config['REDIS_PORT'],'ping'])
assert 'NOAUTH' in redis_no.stdout
run('rabbit-node',COMPOSE+['exec','-T','rabbitmq','rabbitmq-diagnostics','-q','check_running'])
run('rabbit-auth',COMPOSE+['exec','-T','rabbitmq','sh','-ec','rabbitmqctl authenticate_user "$RABBITMQ_DEFAULT_USER" "$RABBITMQ_DEFAULT_PASS"'])
auth = b'\0'+config['RABBITMQ_DEFAULT_USER'].encode()+b'\0'+config['RABBITMQ_DEFAULT_PASS'].encode()
with socket.create_connection(('127.0.0.1',int(config['RABBITMQ_PORT'])), timeout=10) as sock:
    sock.sendall(b'AMQP\0\0\x09\x01')
    assert frame(sock)[:4] == struct.pack('>HH',10,10)
    send(sock,11,struct.pack('>I',0)+b'\x05PLAIN'+struct.pack('>I',len(auth))+auth+b'\x05en_US')
    tune=frame(sock);assert tune[:4] == struct.pack('>HH',10,30)
    send(sock,31,tune[4:])
    send(sock,40,b'\x01/\x00\x00')
    assert frame(sock)[:4] == struct.pack('>HH',10,41)
    send(sock,50,struct.pack('>H',200)+b'\x00'+struct.pack('>HH',0,0))
    assert frame(sock)[:4] == struct.pack('>HH',10,51)
print('AMQP 0-9-1 真实认证握手、打开 vhost /、正常关闭通过；未发布消息。')
url='http://127.0.0.1:'+config['RABBITMQ_MANAGEMENT_PORT']+'/api/overview'
for authenticated,expected in [(False,401),(True,200)]:
    headers={}
    if authenticated:
        encoded=base64.b64encode((config['RABBITMQ_DEFAULT_USER']+':'+config['RABBITMQ_DEFAULT_PASS']).encode()).decode()
        headers={'Authorization':'Basic '+encoded}
    try:
        with urllib.request.urlopen(urllib.request.Request(url,headers=headers),timeout=10) as response:
            status=response.status
    except urllib.error.HTTPError as error:
        status=error.code
    assert status == expected
    print('RabbitMQ management authenticated='+str(authenticated)+' HTTP '+str(status))
save('infra-auth-observation',{'cwd':str(ROOT),'steps':['PostgreSQL 经宿主映射端口 TCP 密码认证 SELECT','Redis 经宿主映射端口认证 PING / 无认证 NOAUTH','RabbitMQ diagnostics / authenticate_user','AMQP 实际认证握手 / vhost open / close','management 401 / 200'],'status':'PASS','application_integration':'NOT_EXECUTED','message_publish':'NOT_EXECUTED'})
