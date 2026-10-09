-- B01正式管理能力；保持V1～V5校验和与已有角色授权不变。
SET ROLE pet_migrator;
GRANT INSERT ON public.identity_employee TO pet_runtime;
GRANT UPDATE(display_name,status,authorization_version) ON public.identity_employee TO pet_runtime;
GRANT INSERT,UPDATE,DELETE ON public.identity_role,public.identity_role_permission,public.identity_employee_role,public.identity_employee_store TO pet_runtime;
CREATE TABLE public.identity_management_event (
 id uuid PRIMARY KEY,
 tenant_id uuid NOT NULL REFERENCES public.platform_tenant(id),
 operator_id uuid NOT NULL,
 target_id uuid NOT NULL,
 operation varchar(32) NOT NULL CHECK(operation IN ('CREATE_EMPLOYEE','EDIT_EMPLOYEE','ENABLE_EMPLOYEE','DISABLE_EMPLOYEE','ASSIGN_ROLES','ASSIGN_STORES','CREATE_ROLE','EDIT_ROLE','ROLE_GRANTS')),
 trace_id varchar(64) NOT NULL CHECK(trace_id ~ '^[A-Za-z0-9_-]{1,64}$'),
 occurred_at timestamp(3) with time zone NOT NULL DEFAULT clock_timestamp(),
 FOREIGN KEY(tenant_id,operator_id) REFERENCES public.identity_employee(tenant_id,id)
);
ALTER TABLE public.identity_management_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.identity_management_event FORCE ROW LEVEL SECURITY;
CREATE POLICY runtime_tenant ON public.identity_management_event TO pet_runtime
 USING(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid)
 WITH CHECK(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid);
REVOKE ALL ON public.identity_management_event FROM PUBLIC;
GRANT INSERT ON public.identity_management_event TO pet_runtime;
-- 仅显式升级命令补充保留管理员；scope沿来源权限，不自动赋权。
GRANT CREATE ON SCHEMA pet_identity TO pet_bootstrap_owner;
SET ROLE pet_bootstrap_owner;
CREATE FUNCTION pet_identity.upgrade_identity_management(p_tenant uuid) RETURNS integer
 LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
DECLARE changed integer; target_role uuid;
BEGIN
 PERFORM pg_catalog.set_config('pet.tenant_id',p_tenant::text,true);
 PERFORM pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(p_tenant::text,0));
 SELECT id INTO target_role FROM public.identity_role WHERE tenant_id=p_tenant AND code='tenant-admin' AND status='ACTIVE';
 IF target_role IS NULL THEN RAISE EXCEPTION 'identity management upgrade unavailable'; END IF;
 INSERT INTO public.identity_role_permission(id,tenant_id,role_id,permission_code,scope_type)
 SELECT pg_catalog.gen_random_uuid(),p_tenant,target_role,m.new_code,rp.scope_type
 FROM (VALUES ('identity:user:detail','identity:user:list'),('identity:user:roles','identity:user:update'),
 ('identity:user:stores','identity:user:update'),('identity:user:enable','identity:user:disable'),
 ('identity:role:detail','identity:role:list'),('identity:role:create','identity:role:update'),
 ('identity:role:grant','identity:role:update'),('identity:user:revoke-sessions','identity:user:reset-password')) m(new_code,source_code)
 JOIN public.identity_role_permission rp ON rp.tenant_id=p_tenant AND rp.role_id=target_role AND rp.permission_code=m.source_code
 WHERE (m.new_code NOT LIKE 'identity:role:%' OR rp.scope_type='TENANT') AND (m.new_code<>'identity:user:revoke-sessions' OR rp.scope_type IN ('TENANT','STORES'))
 ON CONFLICT(tenant_id,role_id,permission_code,scope_type) DO NOTHING;
 GET DIAGNOSTICS changed=ROW_COUNT;
 IF changed>0 THEN
 UPDATE public.identity_employee e SET authorization_version=e.authorization_version+1,version=e.version+1,updated_at=clock_timestamp()
 WHERE e.tenant_id=p_tenant AND EXISTS(SELECT 1 FROM public.identity_employee_role er WHERE er.tenant_id=p_tenant AND er.role_id=target_role AND er.employee_id=e.id);
 END IF;
 RETURN changed;
END $$;
REVOKE ALL ON FUNCTION pet_identity.upgrade_identity_management(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION pet_identity.upgrade_identity_management(uuid) TO pet_bootstrap;
RESET ROLE;
SET ROLE pet_migrator;
REVOKE CREATE ON SCHEMA pet_identity FROM pet_bootstrap_owner;
RESET ROLE;
