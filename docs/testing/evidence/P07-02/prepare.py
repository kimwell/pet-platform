import pathlib,subprocess,json
root=pathlib.Path.cwd();ev=root/'docs/testing/evidence/P07-02'
cmd=['docker','exec','-i','pet-p07-02-pg','psql','-X','-qAt','-v','ON_ERROR_STOP=1','-U','postgres','-d','p07_02_acceptance']
sql="""
DO $$ DECLARE t uuid; a uuid; s uuid; b uuid; e uuid; h text; BEGIN
SELECT id INTO t FROM platform_tenant WHERE code='p07-02-acceptance';
SELECT id,password_hash INTO a,h FROM identity_employee WHERE tenant_id=t;
SELECT id INTO s FROM platform_store WHERE tenant_id=t AND code='store-a';
b=gen_random_uuid(); INSERT INTO platform_store(id,tenant_id,code,name,status) VALUES(b,t,'store-b','验收门店B','ACTIVE');
FOR i IN 1..55 LOOP
 e=gen_random_uuid();
 INSERT INTO identity_employee(id,tenant_id,login_name,display_name,status,password_hash,created_at,updated_at)
 VALUES(e,t,'employee-'||lpad(i::text,2,'0'), CASE WHEN i=1 THEN E'验收员工01 A%_\\\\AbC' WHEN i=2 THEN '验收员工02'||repeat('长姓名',25) ELSE '验收员工'||lpad(i::text,2,'0') END,
 CASE WHEN i%4=0 THEN 'DISABLED' ELSE 'ACTIVE' END,h,'2026-10-01T00:00:00Z'::timestamptz+i*interval '1 hour','2026-10-02T00:00:00Z'::timestamptz+i*interval '1 hour');
 IF i%3=0 OR i=1 THEN INSERT INTO identity_employee_store(id,tenant_id,employee_id,store_id) VALUES(gen_random_uuid(),t,e,s); END IF;
 IF i%3=1 THEN INSERT INTO identity_employee_store(id,tenant_id,employee_id,store_id) VALUES(gen_random_uuid(),t,e,b); END IF;
END LOOP;
END $$;
SELECT json_build_object('total',count(*)::text,'disabled',count(*) FILTER(WHERE status='DISABLED')::text) FROM identity_employee WHERE tenant_id=(SELECT id FROM platform_tenant WHERE code='p07-02-acceptance');
"""
p=subprocess.run(cmd,input=sql,text=True,capture_output=True)
(ev/'data-preparation.json').write_text(json.dumps({'cwd':str(root),'command':cmd,'exitCode':p.returncode,'summary':p.stdout,'purpose':'正式bootstrap操作者与第二租户；55名目标通过隔离SQL准备，不是员工创建功能验收；正式JAR/唯一Provider，无测试profile'},ensure_ascii=False,indent=2))
if p.returncode: raise RuntimeError('隔离准备失败')
print('隔离多员工与多门店场景准备完成；精确统计已留证')
