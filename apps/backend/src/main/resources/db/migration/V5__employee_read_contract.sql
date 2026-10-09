-- 员工读取的查询索引与显式保留管理员详情权限补充。普通迁移不写角色授权。
SET ROLE pet_migrator;
CREATE INDEX employee_page_idx ON public.identity_employee(tenant_id,created_at DESC,id DESC);
-- 使用已有NOLOGIN初始化owner；不授密码读取、运行角色成员关系或任意授权API。
GRANT SELECT ON public.identity_role,public.identity_role_permission,public.identity_employee_role TO pet_bootstrap_owner;
GRANT SELECT(id,tenant_id,authorization_version,updated_at,version) ON public.identity_employee TO pet_bootstrap_owner;
GRANT UPDATE(authorization_version,updated_at,version) ON public.identity_employee TO pet_bootstrap_owner;
CREATE POLICY employee_read_upgrade ON public.identity_role FOR SELECT TO pet_bootstrap_owner
 USING(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid);
CREATE POLICY employee_read_upgrade ON public.identity_role_permission FOR SELECT TO pet_bootstrap_owner
 USING(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid);
CREATE POLICY employee_read_upgrade ON public.identity_employee_role FOR SELECT TO pet_bootstrap_owner
 USING(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid);
CREATE POLICY employee_read_upgrade ON public.identity_employee FOR SELECT TO pet_bootstrap_owner
 USING(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid);
CREATE POLICY employee_read_upgrade_update ON public.identity_employee FOR UPDATE TO pet_bootstrap_owner
 USING(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid)
 WITH CHECK(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid);
GRANT CREATE ON SCHEMA pet_identity TO pet_bootstrap_owner;
SET ROLE pet_bootstrap_owner;
CREATE FUNCTION pet_identity.upgrade_employee_read(p_tenant uuid) RETURNS integer
 LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
DECLARE changed integer; target_role uuid;
BEGIN
 PERFORM pg_catalog.set_config('pet.tenant_id',p_tenant::text,true);
 PERFORM pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(p_tenant::text,0));
 SELECT id INTO target_role FROM public.identity_role WHERE tenant_id=p_tenant AND code='tenant-admin' AND status='ACTIVE';
 IF target_role IS NULL THEN RAISE EXCEPTION 'employee read upgrade unavailable'; END IF;
 INSERT INTO public.identity_role_permission(id,tenant_id,role_id,permission_code,scope_type)
 SELECT pg_catalog.gen_random_uuid(),p_tenant,target_role,'identity:user:detail',scope_type
 FROM public.identity_role_permission WHERE tenant_id=p_tenant AND role_id=target_role AND permission_code='identity:user:list'
 ON CONFLICT(tenant_id,role_id,permission_code,scope_type) DO NOTHING;
 GET DIAGNOSTICS changed=ROW_COUNT;
 IF changed>0 THEN
  UPDATE public.identity_employee e SET authorization_version=e.authorization_version+1,version=e.version+1,updated_at=pg_catalog.clock_timestamp()
  WHERE e.tenant_id=p_tenant AND EXISTS(SELECT 1 FROM public.identity_employee_role er WHERE er.tenant_id=p_tenant AND er.role_id=target_role AND er.employee_id=e.id);
 END IF;
 RETURN changed;
END $$;
REVOKE ALL ON FUNCTION pet_identity.upgrade_employee_read(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION pet_identity.upgrade_employee_read(uuid) TO pet_bootstrap;
RESET ROLE;
SET ROLE pet_migrator;
REVOKE CREATE ON SCHEMA pet_identity FROM pet_bootstrap_owner;
RESET ROLE;
