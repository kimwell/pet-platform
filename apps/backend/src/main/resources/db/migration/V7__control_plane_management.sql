-- B02固定控制面函数；运行角色保持无表权限，能力owner没有超级用户或BYPASSRLS。
SET ROLE pet_migrator;
ALTER TABLE public.platform_tenant ADD COLUMN initialized boolean NOT NULL DEFAULT true;
ALTER TABLE pet_control.platform_permission DROP CONSTRAINT platform_permission_permission_code_check;
ALTER TABLE pet_control.platform_permission ADD CONSTRAINT platform_permission_permission_code_check CHECK(permission_code IN ('platform:session:manage','platform:credential:change','platform:redis:operate','platform:tenant:list','platform:tenant:detail','platform:tenant:create','platform:tenant:update','platform:tenant:enable','platform:tenant:disable','platform:account:list','platform:account:detail','platform:account:create','platform:account:update','platform:account:enable','platform:account:disable','platform:account:grant','platform:account:reset-password','platform:account:revoke-sessions','platform:store:control-list','platform:store:control-create','platform:store:control-update','platform:store:control-status'));
CREATE TABLE pet_control.management_event (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),actor_id uuid NOT NULL REFERENCES pet_control.platform_account(id),
 target_id uuid NOT NULL,operation varchar(40) NOT NULL CHECK(operation IN ('CREATE_TENANT','EDIT_TENANT','TENANT_STATUS','CREATE_ACCOUNT','EDIT_ACCOUNT','ACCOUNT_STATUS','ACCOUNT_GRANTS','RESET_PASSWORD','REVOKE_SESSIONS','CREATE_STORE','EDIT_STORE','STORE_STATUS')),
 trace_id varchar(64) NOT NULL CHECK(trace_id ~ '^[A-Za-z0-9_-]{1,64}$'),occurred_at timestamptz(3) NOT NULL DEFAULT clock_timestamp()
);
ALTER TABLE pet_control.management_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE pet_control.management_event FORCE ROW LEVEL SECURITY;
GRANT USAGE ON SCHEMA public,pet_control TO pet_control_manager_owner;
GRANT SELECT,INSERT,UPDATE(name,status,security_version,version,updated_at) ON public.platform_tenant TO pet_control_manager_owner;
GRANT SELECT,INSERT,UPDATE(name,status,version,updated_at) ON public.platform_store TO pet_control_manager_owner;
GRANT SELECT,INSERT,UPDATE(display_name,status,password_hash,security_version,authorization_version,version,updated_at) ON pet_control.platform_account TO pet_control_manager_owner;
GRANT SELECT,INSERT,DELETE ON pet_control.platform_permission TO pet_control_manager_owner;
GRANT INSERT ON pet_control.platform_session_cleanup,pet_control.management_event TO pet_control_manager_owner;
CREATE POLICY control_metadata ON public.platform_tenant TO pet_control_manager_owner USING(true) WITH CHECK(true);
CREATE POLICY control_metadata ON public.platform_store TO pet_control_manager_owner USING(true) WITH CHECK(true);
CREATE POLICY management ON pet_control.platform_account TO pet_control_manager_owner USING(true) WITH CHECK(true);
CREATE POLICY management ON pet_control.platform_permission TO pet_control_manager_owner USING(true) WITH CHECK(true);
CREATE POLICY management ON pet_control.platform_session_cleanup FOR INSERT TO pet_control_manager_owner WITH CHECK(true);
CREATE POLICY management ON pet_control.management_event FOR INSERT TO pet_control_manager_owner WITH CHECK(true);
GRANT CREATE ON SCHEMA pet_control TO pet_control_manager_owner;
SET ROLE pet_control_manager_owner;
CREATE FUNCTION pet_control.require_management(p_permission text,p_write boolean) RETURNS uuid
 LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
DECLARE actor uuid:=nullif(current_setting('pet.platform_id',true),'')::uuid;
BEGIN
 IF p_write THEN PERFORM pg_advisory_xact_lock(50202,1); END IF;
 PERFORM a.id FROM pet_control.platform_account a WHERE a.id=actor AND a.status='ACTIVE'
 AND a.security_version=nullif(current_setting('pet.platform_security',true),'')::bigint
 AND a.authorization_version=nullif(current_setting('pet.platform_authorization',true),'')::bigint FOR SHARE;
 IF NOT FOUND THEN RAISE EXCEPTION USING ERRCODE='P0201',MESSAGE='平台身份已失效'; END IF;
 IF NOT EXISTS(SELECT 1 FROM pet_control.platform_permission WHERE account_id=actor AND permission_code=p_permission)
 THEN RAISE EXCEPTION USING ERRCODE='P0203',MESSAGE='没有此平台操作权限'; END IF;
 RETURN actor;
END $$;
CREATE FUNCTION pet_control.tenant_view(p public.platform_tenant) RETURNS jsonb LANGUAGE sql IMMUTABLE SET search_path=pg_catalog,pg_temp AS $$
 SELECT jsonb_build_object('id',p.id,'code',p.code,'name',p.name,'status',p.status,'initialized',p.initialized,'version',p.version::text,'createdAt',p.created_at,'updatedAt',p.updated_at)
$$;
CREATE FUNCTION pet_control.account_view(p pet_control.platform_account) RETURNS jsonb LANGUAGE sql STABLE SET search_path=pg_catalog,pg_temp AS $$
 SELECT jsonb_build_object('id',p.id,'loginName',p.login_name,'displayName',p.display_name,'status',p.status,'version',p.version::text,'createdAt',p.created_at,'updatedAt',p.updated_at,
 'permissions',coalesce((SELECT jsonb_agg(permission_code ORDER BY permission_code) FROM pet_control.platform_permission WHERE account_id=p.id),'[]'::jsonb))
$$;
CREATE FUNCTION pet_control.store_view(p public.platform_store) RETURNS jsonb LANGUAGE sql IMMUTABLE SET search_path=pg_catalog,pg_temp AS $$
 SELECT jsonb_build_object('id',p.id,'tenantId',p.tenant_id,'code',p.code,'name',p.name,'status',p.status,'version',p.version::text,'createdAt',p.created_at,'updatedAt',p.updated_at)
$$;
CREATE FUNCTION pet_control.tenant_read(p_id uuid,p_keyword text,p_status text,p_page integer,p_size integer,p_sort text,p_order text) RETURNS jsonb
 LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
DECLARE result jsonb; total bigint;
BEGIN
 PERFORM pet_control.require_management(CASE WHEN p_id IS NULL THEN 'platform:tenant:list' ELSE 'platform:tenant:detail' END,false);
 IF p_id IS NOT NULL THEN SELECT pet_control.tenant_view(t) INTO result FROM public.platform_tenant t WHERE t.id=p_id;
 IF result IS NULL THEN RAISE EXCEPTION USING ERRCODE='P0204',MESSAGE='租户不存在'; END IF; RETURN result; END IF;
 SELECT count(*) INTO total FROM public.platform_tenant WHERE (p_status IS NULL OR status=p_status) AND (p_keyword IS NULL OR position(lower(p_keyword) in lower(code||' '||name))>0);
 SELECT coalesce(jsonb_agg(v),'[]'::jsonb) INTO result FROM (SELECT pet_control.tenant_view(t) v FROM public.platform_tenant t
 WHERE (p_status IS NULL OR status=p_status) AND (p_keyword IS NULL OR position(lower(p_keyword) in lower(code||' '||name))>0)
 ORDER BY CASE WHEN p_order='asc' THEN CASE p_sort WHEN 'code' THEN code WHEN 'name' THEN name WHEN 'status' THEN status ELSE created_at::text END END ASC,
 CASE WHEN p_order='desc' THEN CASE p_sort WHEN 'code' THEN code WHEN 'name' THEN name WHEN 'status' THEN status ELSE created_at::text END END DESC,id ASC
 LIMIT p_size OFFSET (p_page::bigint-1)*p_size) q;
 RETURN jsonb_build_object('items',result,'page',p_page,'pageSize',p_size,'total',total::text);
END $$;
CREATE FUNCTION pet_control.tenant_write(p_action text,p_id uuid,p_version bigint,p_code text,p_name text,p_status text,p_trace text) RETURNS jsonb
 LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
DECLARE actor uuid; t public.platform_tenant; op text;
BEGIN
 actor:=pet_control.require_management('platform:tenant:'||CASE p_action WHEN 'CREATE' THEN 'create' WHEN 'EDIT' THEN 'update' WHEN 'STATUS' THEN CASE WHEN p_status='ACTIVE' THEN 'enable' ELSE 'disable' END ELSE 'invalid' END,true);
 IF p_action='CREATE' THEN
 PERFORM pg_advisory_xact_lock(hashtextextended(p_code,0));
 INSERT INTO public.platform_tenant(id,code,name,status,initialized) VALUES(gen_random_uuid(),p_code,p_name,'ACTIVE',false) RETURNING * INTO t;op:='CREATE_TENANT';
 ELSE
 SELECT * INTO t FROM public.platform_tenant WHERE id=p_id FOR UPDATE;
 IF NOT FOUND THEN RAISE EXCEPTION USING ERRCODE='P0204',MESSAGE='租户不存在'; END IF;
 IF t.version<>p_version THEN RAISE EXCEPTION USING ERRCODE='P0209',MESSAGE='租户已被修改'; END IF;
 IF p_action='EDIT' THEN UPDATE public.platform_tenant SET name=p_name,version=version+1,updated_at=clock_timestamp() WHERE id=p_id RETURNING * INTO t;op:='EDIT_TENANT';
 ELSE
 IF t.status=p_status THEN RAISE EXCEPTION USING ERRCODE='P0210',MESSAGE='租户已经处于目标状态'; END IF;
 UPDATE public.platform_tenant SET status=p_status,security_version=security_version+1,version=version+1,updated_at=clock_timestamp() WHERE id=p_id RETURNING * INTO t;op:='TENANT_STATUS';
 END IF; END IF;
 INSERT INTO pet_control.management_event(actor_id,target_id,operation,trace_id) VALUES(actor,t.id,op,p_trace);
 RETURN jsonb_build_object('id',t.id,'version',t.version::text,'sessionCleanupComplete',p_action<>'STATUS');
END $$;
CREATE FUNCTION pet_control.account_read(p_id uuid,p_keyword text,p_status text,p_page integer,p_size integer,p_sort text,p_order text) RETURNS jsonb
 LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
DECLARE result jsonb; total bigint;
BEGIN
 PERFORM pet_control.require_management(CASE WHEN p_id IS NULL THEN 'platform:account:list' ELSE 'platform:account:detail' END,false);
 IF p_id IS NOT NULL THEN SELECT pet_control.account_view(a) INTO result FROM pet_control.platform_account a WHERE id=p_id;
 IF result IS NULL THEN RAISE EXCEPTION USING ERRCODE='P0204',MESSAGE='平台账号不存在'; END IF; RETURN result; END IF;
 SELECT count(*) INTO total FROM pet_control.platform_account WHERE (p_status IS NULL OR status=p_status) AND (p_keyword IS NULL OR position(lower(p_keyword) in lower(login_name||' '||display_name))>0);
 SELECT coalesce(jsonb_agg(v),'[]'::jsonb) INTO result FROM (SELECT pet_control.account_view(a) v FROM pet_control.platform_account a
 WHERE (p_status IS NULL OR status=p_status) AND (p_keyword IS NULL OR position(lower(p_keyword) in lower(login_name||' '||display_name))>0)
 ORDER BY CASE WHEN p_order='asc' THEN CASE p_sort WHEN 'loginName' THEN login_name WHEN 'displayName' THEN display_name WHEN 'status' THEN status ELSE created_at::text END END ASC,
 CASE WHEN p_order='desc' THEN CASE p_sort WHEN 'loginName' THEN login_name WHEN 'displayName' THEN display_name WHEN 'status' THEN status ELSE created_at::text END END DESC,id ASC
 LIMIT p_size OFFSET (p_page::bigint-1)*p_size) q;
 RETURN jsonb_build_object('items',result,'page',p_page,'pageSize',p_size,'total',total::text);
END $$;
CREATE FUNCTION pet_control.account_write(p_action text,p_id uuid,p_version bigint,p_login text,p_name text,p_hash text,p_permissions text[],p_status text,p_trace text) RETURNS jsonb
 LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
DECLARE actor uuid; a pet_control.platform_account; op text; required text[]:=ARRAY['platform:session:manage','platform:credential:change','platform:account:grant','platform:account:enable','platform:account:reset-password'];
BEGIN
 actor:=pet_control.require_management('platform:account:'||CASE p_action WHEN 'CREATE' THEN 'create' WHEN 'EDIT' THEN 'update' WHEN 'GRANTS' THEN 'grant' WHEN 'RESET' THEN 'reset-password' WHEN 'REVOKE' THEN 'revoke-sessions' WHEN 'STATUS' THEN CASE WHEN p_status='ACTIVE' THEN 'enable' ELSE 'disable' END ELSE 'invalid' END,true);
 IF p_action IN ('CREATE','GRANTS') THEN
 IF p_action='CREATE' THEN PERFORM pet_control.require_management('platform:account:grant',false); END IF;
 IF NOT ('platform:session:manage'=ANY(p_permissions) AND 'platform:credential:change'=ANY(p_permissions)) THEN RAISE EXCEPTION USING ERRCODE='P0222',MESSAGE='账号必须保留本人会话和改密权限'; END IF;
 IF EXISTS(SELECT unnest(p_permissions) EXCEPT SELECT permission_code FROM pet_control.platform_permission WHERE account_id=actor)
 THEN RAISE EXCEPTION USING ERRCODE='P0203',MESSAGE='不能授予自身不具备的权限'; END IF;
 END IF;
 IF p_action='CREATE' THEN
 INSERT INTO pet_control.platform_account(id,login_name,display_name,password_hash) VALUES(gen_random_uuid(),p_login,p_name,p_hash) RETURNING * INTO a;
 INSERT INTO pet_control.platform_permission SELECT a.id,unnest(p_permissions);op:='CREATE_ACCOUNT';
 ELSE
 SELECT * INTO a FROM pet_control.platform_account WHERE id=p_id FOR UPDATE;
 IF NOT FOUND THEN RAISE EXCEPTION USING ERRCODE='P0204',MESSAGE='平台账号不存在'; END IF;
 IF a.version<>p_version THEN RAISE EXCEPTION USING ERRCODE='P0209',MESSAGE='平台账号已被修改'; END IF;
 IF p_action<>'EDIT' AND p_id=actor THEN RAISE EXCEPTION USING ERRCODE='P0203',MESSAGE='请使用本人安全入口，不能自我停用或改授权'; END IF;
 IF p_action='EDIT' THEN UPDATE pet_control.platform_account SET display_name=p_name,version=version+1,updated_at=clock_timestamp() WHERE id=p_id RETURNING * INTO a;op:='EDIT_ACCOUNT';
 ELSE
 IF p_action='STATUS' AND a.status=p_status THEN RAISE EXCEPTION USING ERRCODE='P0210',MESSAGE='账号已经处于目标状态'; END IF;
 -- 管理员治理先串行化全局修改，再核对最后有效恢复入口，不能由两个事务分别通过。
 IF (p_action='STATUS' AND p_status='DISABLED' OR p_action='GRANTS' AND NOT required<@p_permissions)
 AND a.status='ACTIVE' AND required<@ARRAY(SELECT permission_code::text FROM pet_control.platform_permission WHERE account_id=p_id)
 AND NOT EXISTS(SELECT 1 FROM pet_control.platform_account other WHERE other.id<>p_id AND other.status='ACTIVE' AND required<@ARRAY(SELECT permission_code::text FROM pet_control.platform_permission WHERE account_id=other.id))
 THEN RAISE EXCEPTION USING ERRCODE='P0210',MESSAGE='必须保留一个有效平台管理入口'; END IF;
 IF p_action='GRANTS' THEN DELETE FROM pet_control.platform_permission WHERE account_id=p_id; INSERT INTO pet_control.platform_permission SELECT p_id,unnest(p_permissions);op:='ACCOUNT_GRANTS';
 ELSIF p_action='STATUS' THEN op:='ACCOUNT_STATUS';
 ELSIF p_action='RESET' THEN op:='RESET_PASSWORD';
 ELSE op:='REVOKE_SESSIONS'; END IF;
 UPDATE pet_control.platform_account SET status=CASE WHEN p_action='STATUS' THEN p_status ELSE status END,
 password_hash=CASE WHEN p_action='RESET' THEN p_hash ELSE password_hash END,
 authorization_version=authorization_version+CASE WHEN p_action IN ('GRANTS','STATUS') THEN 1 ELSE 0 END,
 security_version=security_version+1,version=version+1,updated_at=clock_timestamp() WHERE id=p_id RETURNING * INTO a;
 INSERT INTO pet_control.platform_session_cleanup(account_id,revoke_before) VALUES(p_id,a.security_version);
 END IF; END IF;
 INSERT INTO pet_control.management_event(actor_id,target_id,operation,trace_id) VALUES(actor,a.id,op,p_trace);
 RETURN jsonb_build_object('id',a.id,'version',a.version::text,'sessionCleanupComplete',true);
END $$;
CREATE FUNCTION pet_control.store_read(p_tenant uuid) RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
BEGIN
 PERFORM pet_control.require_management('platform:store:control-list',false);
 IF NOT EXISTS(SELECT 1 FROM public.platform_tenant WHERE id=p_tenant) THEN RAISE EXCEPTION USING ERRCODE='P0204',MESSAGE='租户不存在'; END IF;
 RETURN coalesce((SELECT jsonb_agg(pet_control.store_view(s) ORDER BY s.code,s.id) FROM public.platform_store s WHERE tenant_id=p_tenant),'[]'::jsonb);
END $$;
CREATE FUNCTION pet_control.store_write(p_action text,p_tenant uuid,p_id uuid,p_version bigint,p_code text,p_name text,p_status text,p_trace text) RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
DECLARE actor uuid; s public.platform_store;
BEGIN
 actor:=pet_control.require_management('platform:store:'||CASE p_action WHEN 'CREATE' THEN 'control-create' WHEN 'EDIT' THEN 'control-update' WHEN 'STATUS' THEN 'control-status' ELSE 'invalid' END,true);
 PERFORM id FROM public.platform_tenant WHERE id=p_tenant AND initialized AND status='ACTIVE' FOR UPDATE;
 IF NOT FOUND THEN RAISE EXCEPTION USING ERRCODE='P0210',MESSAGE='租户尚未初始化或已停用'; END IF;
 IF p_action='CREATE' THEN INSERT INTO public.platform_store(id,tenant_id,code,name,status) VALUES(gen_random_uuid(),p_tenant,p_code,p_name,'ACTIVE') RETURNING * INTO s;
 ELSE SELECT * INTO s FROM public.platform_store WHERE id=p_id AND tenant_id=p_tenant FOR UPDATE;
 IF NOT FOUND THEN RAISE EXCEPTION USING ERRCODE='P0204',MESSAGE='门店不存在'; END IF;
 IF s.version<>p_version THEN RAISE EXCEPTION USING ERRCODE='P0209',MESSAGE='门店已被修改'; END IF;
 IF p_action='STATUS' AND s.status=p_status THEN RAISE EXCEPTION USING ERRCODE='P0210',MESSAGE='门店已经处于目标状态'; END IF;
 UPDATE public.platform_store SET name=CASE WHEN p_action='EDIT' THEN p_name ELSE name END,status=CASE WHEN p_action='STATUS' THEN p_status ELSE status END,version=version+1,updated_at=clock_timestamp() WHERE id=p_id RETURNING * INTO s;
 -- 门店范围改变使用租户安全代际关闭旧租户身份，避免赋予控制面读取员工关系的权限。
 IF p_action='STATUS' THEN UPDATE public.platform_tenant SET security_version=security_version+1,version=version+1,updated_at=clock_timestamp() WHERE id=p_tenant; END IF;
 END IF;
 INSERT INTO pet_control.management_event(actor_id,target_id,operation,trace_id) VALUES(actor,s.id,CASE p_action WHEN 'CREATE' THEN 'CREATE_STORE' WHEN 'EDIT' THEN 'EDIT_STORE' ELSE 'STORE_STATUS' END,p_trace);
 RETURN jsonb_build_object('id',s.id,'version',s.version::text,'sessionCleanupComplete',true);
END $$;
REVOKE ALL ON FUNCTION pet_control.require_management(text,boolean),pet_control.tenant_view(public.platform_tenant),pet_control.account_view(pet_control.platform_account),pet_control.store_view(public.platform_store) FROM PUBLIC;
REVOKE ALL ON FUNCTION pet_control.tenant_read(uuid,text,text,integer,integer,text,text),pet_control.tenant_write(text,uuid,bigint,text,text,text,text),pet_control.account_read(uuid,text,text,integer,integer,text,text),pet_control.account_write(text,uuid,bigint,text,text,text,text[],text,text),pet_control.store_read(uuid),pet_control.store_write(text,uuid,uuid,bigint,text,text,text,text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION pet_control.tenant_read(uuid,text,text,integer,integer,text,text),pet_control.tenant_write(text,uuid,bigint,text,text,text,text),pet_control.account_read(uuid,text,text,integer,integer,text,text),pet_control.account_write(text,uuid,bigint,text,text,text,text[],text,text),pet_control.store_read(uuid),pet_control.store_write(text,uuid,uuid,bigint,text,text,text,text) TO pet_runtime;
RESET ROLE;
SET ROLE pet_migrator;
REVOKE CREATE ON SCHEMA pet_control FROM pet_control_manager_owner;
-- 显式平台升级只作用于首次初始化账号；迁移及普通启动不自动赋权。
GRANT SELECT ON pet_control.platform_permission TO pet_platform_bootstrap_owner;
GRANT SELECT(security_version,authorization_version,version,updated_at) ON pet_control.platform_account TO pet_platform_bootstrap_owner;
GRANT UPDATE(security_version,authorization_version,version,updated_at) ON pet_control.platform_account TO pet_platform_bootstrap_owner;
CREATE POLICY bootstrap_management_read ON pet_control.platform_permission FOR SELECT TO pet_platform_bootstrap_owner USING(true);
GRANT CREATE ON SCHEMA pet_control TO pet_platform_bootstrap_owner;
SET ROLE pet_platform_bootstrap_owner;
CREATE FUNCTION pet_control.upgrade_management() RETURNS integer LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
DECLARE principal uuid; changed integer;
BEGIN
 PERFORM pg_advisory_xact_lock(50202,1);
 SELECT account_id INTO principal FROM pet_control.platform_bootstrap WHERE singleton;
 IF principal IS NULL THEN RAISE EXCEPTION '平台尚未初始化'; END IF;
 INSERT INTO pet_control.platform_permission SELECT principal,unnest(ARRAY['platform:session:manage','platform:credential:change','platform:redis:operate','platform:tenant:list','platform:tenant:detail','platform:tenant:create','platform:tenant:update','platform:tenant:enable','platform:tenant:disable','platform:account:list','platform:account:detail','platform:account:create','platform:account:update','platform:account:enable','platform:account:disable','platform:account:grant','platform:account:reset-password','platform:account:revoke-sessions','platform:store:control-list','platform:store:control-create','platform:store:control-update','platform:store:control-status']) ON CONFLICT DO NOTHING;
 GET DIAGNOSTICS changed=ROW_COUNT;
 IF changed>0 THEN UPDATE pet_control.platform_account SET security_version=security_version+1,authorization_version=authorization_version+1,version=version+1,updated_at=clock_timestamp() WHERE id=principal; END IF;
 RETURN changed;
END $$;
REVOKE ALL ON FUNCTION pet_control.upgrade_management() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION pet_control.upgrade_management() TO pet_platform_bootstrap;
RESET ROLE;
SET ROLE pet_migrator;
REVOKE CREATE ON SCHEMA pet_control FROM pet_platform_bootstrap_owner;
-- 扩展已有受控初始化：仅允许明确待初始化且无身份结构的租户，不覆盖既有账号。
GRANT UPDATE(initialized,version,updated_at) ON public.platform_tenant TO pet_bootstrap_owner;
GRANT SELECT(id,tenant_id) ON public.platform_store TO pet_bootstrap_owner;
CREATE POLICY bootstrap_store_integrity ON public.platform_store FOR SELECT TO pet_bootstrap_owner USING(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid);
CREATE POLICY bootstrap_complete ON public.platform_tenant FOR UPDATE TO pet_bootstrap_owner USING(NOT initialized) WITH CHECK(initialized);
GRANT CREATE ON SCHEMA pet_identity TO pet_bootstrap_owner;
SET ROLE pet_bootstrap_owner;
CREATE OR REPLACE FUNCTION pet_identity.bootstrap_tenant(p_code text,p_name text,p_login text,p_hash text,p_store_code text,p_store_name text)
 RETURNS TABLE(tenant_id uuid,employee_id uuid,store_id uuid)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
DECLARE t uuid := pg_catalog.gen_random_uuid(); e uuid := pg_catalog.gen_random_uuid(); r uuid := pg_catalog.gen_random_uuid(); s uuid; existing public.platform_tenant;
BEGIN
 IF p_code IS NULL OR p_code !~ '^[a-z0-9][a-z0-9-]{0,31}$' OR p_login IS NULL OR p_login !~ '^[a-z0-9][a-z0-9._-]{0,63}$'
 OR p_name IS NULL OR length(btrim(p_name)) NOT BETWEEN 1 AND 100
 OR p_hash IS NULL OR p_hash !~ '^\$pbkdf2-sha256\$v1\$600000\$[A-Za-z0-9+/]{43}=\$[A-Za-z0-9+/]{43}=$'
 OR (p_store_code IS NULL) <> (p_store_name IS NULL)
 OR (p_store_code IS NOT NULL AND (p_store_code !~ '^[a-z0-9][a-z0-9-]{0,31}$' OR length(btrim(p_store_name)) NOT BETWEEN 1 AND 100))
 THEN RAISE EXCEPTION USING ERRCODE='22023', MESSAGE='初始化输入不合法'; END IF;
 PERFORM pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(p_code,0));
 SELECT * INTO existing FROM public.platform_tenant WHERE code=p_code FOR UPDATE;
 IF FOUND THEN
 IF existing.initialized OR existing.status<>'ACTIVE' OR existing.name<>p_name THEN RAISE EXCEPTION USING ERRCODE='P0001',MESSAGE='租户已初始化、停用或名称不一致'; END IF;
 t:=existing.id;
 PERFORM set_config('pet.tenant_id',t::text,true);
 IF EXISTS(SELECT e.id FROM public.identity_employee e WHERE e.tenant_id=t) OR EXISTS(SELECT r.id FROM public.identity_role r WHERE r.tenant_id=t) OR EXISTS(SELECT s.id FROM public.platform_store s WHERE s.tenant_id=t)
 THEN RAISE EXCEPTION USING ERRCODE='P0001',MESSAGE='待初始化租户已有未知结构，不能覆盖或修复'; END IF;
 ELSE INSERT INTO public.platform_tenant(id,code,name,status,initialized) VALUES(t,p_code,p_name,'ACTIVE',false); END IF;
 INSERT INTO public.identity_employee(id,tenant_id,login_name,display_name,status,password_hash) VALUES (e,t,p_login,'租户管理员','ACTIVE',p_hash);
 INSERT INTO public.identity_role(id,tenant_id,code,name,status) VALUES (r,t,'tenant-admin','租户管理员','ACTIVE');
 -- 与 PermissionCatalog.ADMIN_PERMISSIONS 一致，新增权限不会因初始化重跑授予。
 INSERT INTO public.identity_role_permission(id,tenant_id,role_id,permission_code,scope_type)
 SELECT pg_catalog.gen_random_uuid(),t,r,p,'TENANT' FROM unnest(ARRAY[
 'identity:user:list','identity:user:create','identity:user:update','identity:user:disable','identity:user:reset-password',
 'identity:role:list','identity:role:update','platform:store:list','platform:store:update']) AS p;
 INSERT INTO public.identity_employee_role(id,tenant_id,employee_id,role_id) VALUES (pg_catalog.gen_random_uuid(),t,e,r);
 IF p_store_code IS NOT NULL THEN
   s := pg_catalog.gen_random_uuid();
   INSERT INTO public.platform_store(id,tenant_id,code,name,status) VALUES (s,t,p_store_code,p_store_name,'ACTIVE');
   INSERT INTO public.identity_employee_store(id,tenant_id,employee_id,store_id) VALUES (pg_catalog.gen_random_uuid(),t,e,s);
 END IF;
 UPDATE public.platform_tenant SET initialized=true,version=version+CASE WHEN existing.id IS NULL THEN 0 ELSE 1 END,updated_at=clock_timestamp() WHERE id=t;
 RETURN QUERY SELECT t,e,s;
EXCEPTION
 WHEN unique_violation THEN RAISE EXCEPTION USING ERRCODE='P0001',MESSAGE='初始化冲突，未修改既有数据';
 WHEN check_violation OR foreign_key_violation OR not_null_violation THEN
   RAISE EXCEPTION USING ERRCODE='22023',MESSAGE='初始化数据约束不满足，全部回滚';
END $$;
RESET ROLE;
SET ROLE pet_migrator;
REVOKE CREATE ON SCHEMA pet_identity FROM pet_bootstrap_owner;
-- 客户公开入口只接受已完成受控初始化的启用租户。
GRANT SELECT(initialized) ON public.platform_tenant TO pet_customer_auth_owner;
GRANT CREATE ON SCHEMA pet_customer TO pet_customer_auth_owner;
SET ROLE pet_customer_auth_owner;
CREATE OR REPLACE FUNCTION pet_customer.resolve_tenant(p_code text) RETURNS TABLE(tenant_id uuid,security_version bigint) LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
 SELECT id,public.platform_tenant.security_version FROM public.platform_tenant WHERE code=p_code AND status='ACTIVE' AND initialized AND p_code ~ '^[a-z0-9][a-z0-9-]{0,31}$'
$$;
RESET ROLE;
SET ROLE pet_migrator;
REVOKE CREATE ON SCHEMA pet_customer FROM pet_customer_auth_owner;
RESET ROLE;
