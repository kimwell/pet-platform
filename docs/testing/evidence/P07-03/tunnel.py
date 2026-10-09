"""仅将本轮回环端口转发到独立容器真实服务，不解释或替换协议数据。"""
import socket,subprocess,threading,select,json,pathlib,os
root=pathlib.Path.cwd();local=root/'.local-data/p07-03';raw='unix:///Users/kimwell/Library/Containers/com.docker.docker/Data/docker.raw.sock'
def serve(listener,name,port):
 def connection(client):
  relay = r'''use Socket; socket(my $s,PF_INET,SOCK_STREAM,getprotobyname('tcp')) or die; connect($s,sockaddr_in(PORT,inet_aton('127.0.0.1'))) or die; binmode STDIN; binmode STDOUT; sub forward { my($from,$to)=@_; my $n=sysread($from,my $buf,65536); exit unless $n; while(length($buf)){my $w=syswrite($to,$buf); exit unless $w; substr($buf,0,$w,'');}} while(1){my $bits=''; vec($bits,fileno(STDIN),1)=1; vec($bits,fileno($s),1)=1; select(my $ready=$bits,undef,undef,undef); forward(\*STDIN,$s) if vec($ready,fileno(STDIN),1); forward($s,\*STDOUT) if vec($ready,fileno($s),1);}'''.replace('PORT',str(port))
  proc=subprocess.Popen(['docker','-H',raw,'exec','-i',name,'perl','-e',relay],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,bufsize=0)
  try:
   while True:
    ready,_,_=select.select([client,proc.stdout],[],[],1)
    if proc.poll() is not None and not ready:break
    for source in ready:
     data=client.recv(65536) if source is client else os.read(proc.stdout.fileno(),65536)
     if not data:return
     if source is client:proc.stdin.write(data)
     else:client.sendall(data)
  except (OSError,ValueError):pass
  finally:
   client.close();proc.stdin.close();proc.terminate()
 while True:
  client,_=listener.accept();threading.Thread(target=connection,args=(client,),daemon=True).start()
listeners=[]
for name,port,host in [('pet-p07-03-pg',5432,18094),('pet-p07-03-redis',6379,18095)]:
 listener=socket.socket();listener.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1);listener.bind(('127.0.0.1',host));listener.listen(64);listeners.append(listener);threading.Thread(target=serve,args=(listener,name,port),daemon=True).start()
(local/'tunnel-resources.json').write_text(json.dumps({'pid':os.getpid(),'pgPort':18094,'redisPort':18095}));(local/'tunnel-resources.json').chmod(0o600)
threading.Event().wait()
