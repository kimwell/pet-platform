-- 内部组织与Store独立；最小平面目录，不推定组织归属可以扩展授权。
SET ROLE pet_migrator;
CREATE TABLE public.identity_organization (
 id uuid PRIMARY KEY,tenant_id uuid NOT NULL REFERENCES public.platform_tenant(id) ON DELETE RESTRICT,
 code varchar(32) NOT NULL CHECK(code ~ '^[a-z0-9][a-z0-9-]{0,31}$'),name varchar(100) NOT NULL CHECK(length(btrim(name)) BETWEEN 1 AND 100),
 status varchar(16) NOT NULL CHECK(status IN ('ACTIVE','DISABLED')),version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
 created_at timestamptz(3) NOT NULL DEFAULT clock_timestamp(),updated_at timestamptz(3) NOT NULL DEFAULT clock_timestamp(),
 UNIQUE(tenant_id,code),UNIQUE(tenant_id,id)
);
ALTER TABLE public.identity_organization ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.identity_organization FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.identity_organization FROM PUBLIC;
GRANT SELECT,INSERT,UPDATE(name,status,version,updated_at) ON public.identity_organization TO pet_runtime;
CREATE POLICY runtime_tenant ON public.identity_organization TO pet_runtime
 USING(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid)
 WITH CHECK(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid);
ALTER TABLE public.identity_management_event DROP CONSTRAINT identity_management_event_operation_check;
ALTER TABLE public.identity_management_event ADD CONSTRAINT identity_management_event_operation_check CHECK(operation IN ('CREATE_EMPLOYEE','EDIT_EMPLOYEE','ENABLE_EMPLOYEE','DISABLE_EMPLOYEE','ASSIGN_ROLES','ASSIGN_STORES','CREATE_ROLE','EDIT_ROLE','ROLE_GRANTS','CREATE_ORGANIZATION','EDIT_ORGANIZATION','ORGANIZATION_STATUS'));
GRANT CREATE ON SCHEMA pet_identity TO pet_bootstrap_owner;
SET ROLE pet_bootstrap_owner;
CREATE FUNCTION pet_identity.upgrade_organizations(p_tenant uuid) RETURNS integer LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
DECLARE changed integer; target_role uuid;
BEGIN
 PERFORM set_config('pet.tenant_id',p_tenant::text,true);
 PERFORM pg_advisory_xact_lock(hashtextextended(p_tenant::text,0));
 SELECT id INTO target_role FROM public.identity_role WHERE tenant_id=p_tenant AND code='tenant-admin' AND status='ACTIVE';
 IF target_role IS NULL THEN RAISE EXCEPTION '组织权限升级不可用'; END IF;
 INSERT INTO public.identity_role_permission(id,tenant_id,role_id,permission_code,scope_type)
 SELECT gen_random_uuid(),p_tenant,target_role,p,'TENANT' FROM unnest(ARRAY['identity:organization:list','identity:organization:detail','identity:organization:create','identity:organization:update','identity:organization:enable','identity:organization:disable']) p
 WHERE EXISTS(SELECT 1 FROM public.identity_role_permission WHERE tenant_id=p_tenant AND role_id=target_role AND permission_code='identity:role:update' AND scope_type='TENANT')
 ON CONFLICT DO NOTHING;
 GET DIAGNOSTICS changed=ROW_COUNT;
 IF changed>0 THEN UPDATE public.identity_employee SET authorization_version=authorization_version+1,version=version+1,updated_at=clock_timestamp() WHERE tenant_id=p_tenant AND id IN(SELECT employee_id FROM public.identity_employee_role WHERE tenant_id=p_tenant AND role_id=target_role); END IF;
 RETURN changed;
END $$;
REVOKE ALL ON FUNCTION pet_identity.upgrade_organizations(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION pet_identity.upgrade_organizations(uuid) TO pet_bootstrap;
RESET ROLE;
SET ROLE pet_migrator;
REVOKE CREATE ON SCHEMA pet_identity FROM pet_bootstrap_owner;
RESET ROLE;
