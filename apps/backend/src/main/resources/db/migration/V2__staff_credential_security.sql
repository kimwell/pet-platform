-- P05-03 追加迁移；不改V1、不自动扩大既有角色授权。
SET ROLE pet_migrator;
ALTER TABLE public.identity_employee
 ADD COLUMN password_change_required boolean NOT NULL DEFAULT false,
 ADD COLUMN system_reserved boolean NOT NULL DEFAULT false;
GRANT SELECT (password_change_required,system_reserved) ON public.identity_employee TO pet_runtime;
GRANT UPDATE (password_hash,password_change_required,security_version,updated_at,version) ON public.identity_employee TO pet_runtime;
-- 行锁需要UPDATE权限；函数owner只能更新id，函数没有写语句。
GRANT UPDATE (id) ON public.identity_employee TO pet_auth_owner;
CREATE POLICY authentication_lock ON public.identity_employee FOR UPDATE TO pet_auth_owner USING(true) WITH CHECK(false);
CREATE POLICY authentication_lock ON public.platform_tenant FOR UPDATE TO pet_auth_owner USING(true) WITH CHECK(false);
GRANT SELECT,UPDATE(id) ON public.platform_tenant TO pet_auth_owner;
CREATE TABLE public.identity_security_event (
 id uuid PRIMARY KEY,
 tenant_id uuid NOT NULL REFERENCES public.platform_tenant(id),
 operator_id uuid NOT NULL,
 target_employee_id uuid NOT NULL,
 operation varchar(32) NOT NULL CHECK(operation IN ('CHANGE_PASSWORD','RESET_PASSWORD','LOGOUT_ALL','REVOKE_SESSIONS')),
 result varchar(40) NOT NULL,
 occurred_at timestamp(3) with time zone NOT NULL DEFAULT clock_timestamp(),
 trace_id varchar(64) NOT NULL CHECK(trace_id ~ '^[A-Za-z0-9_-]{1,64}$'),
 FOREIGN KEY(tenant_id,operator_id) REFERENCES public.identity_employee(tenant_id,id)
);
CREATE INDEX security_event_subject_idx ON public.identity_security_event(tenant_id,target_employee_id,occurred_at);
CREATE TABLE public.identity_session_cleanup (
 id uuid PRIMARY KEY,
 tenant_id uuid NOT NULL,
 employee_id uuid NOT NULL,
 revoke_before bigint NOT NULL CHECK(revoke_before>0),
 completed boolean NOT NULL DEFAULT false,
 created_at timestamp(3) with time zone NOT NULL DEFAULT clock_timestamp(),
 completed_at timestamp(3) with time zone,
 UNIQUE(tenant_id,employee_id,revoke_before),
 FOREIGN KEY(tenant_id,employee_id) REFERENCES public.identity_employee(tenant_id,id)
);
CREATE INDEX session_cleanup_pending_idx ON public.identity_session_cleanup(tenant_id,employee_id,revoke_before) WHERE NOT completed;
ALTER TABLE public.identity_security_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.identity_security_event FORCE ROW LEVEL SECURITY;
ALTER TABLE public.identity_session_cleanup ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.identity_session_cleanup FORCE ROW LEVEL SECURITY;
CREATE POLICY runtime_tenant ON public.identity_security_event TO pet_runtime
 USING(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid)
 WITH CHECK(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid);
CREATE POLICY runtime_tenant ON public.identity_session_cleanup TO pet_runtime
 USING(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid)
 WITH CHECK(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid);
REVOKE ALL ON public.identity_security_event,public.identity_session_cleanup FROM PUBLIC;
GRANT INSERT ON public.identity_security_event TO pet_runtime;
GRANT SELECT,INSERT ON public.identity_session_cleanup TO pet_runtime;
GRANT UPDATE(completed,completed_at) ON public.identity_session_cleanup TO pet_runtime;
GRANT CREATE ON SCHEMA pet_identity TO pet_auth_owner;
SET ROLE pet_auth_owner;
-- 仅在已验证STAFF事务的当前租户中取凭据与锁，不接受请求tenantId。
CREATE FUNCTION pet_identity.lock_staff_credential(p_employee uuid)
 RETURNS TABLE(password_hash text,security_version bigint,version bigint,password_change_required boolean,system_reserved boolean,tenant_admin boolean,status text)
 LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
 SELECT e.password_hash::text,e.security_version,e.version,e.password_change_required,e.system_reserved,
 EXISTS(SELECT 1 FROM public.identity_employee_role er JOIN public.identity_role r ON r.tenant_id=er.tenant_id AND r.id=er.role_id
        WHERE er.tenant_id=e.tenant_id AND er.employee_id=e.id AND r.code='tenant-admin'),e.status::text
 FROM public.identity_employee e
 WHERE e.tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid AND e.id=p_employee
 FOR UPDATE OF e
$$;
CREATE FUNCTION pet_identity.lock_staff_tenant()
 RETURNS void LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
 SELECT FROM public.platform_tenant t WHERE t.id=nullif(current_setting('pet.tenant_id',true),'')::uuid FOR SHARE
$$;
DROP FUNCTION pet_identity.staff_authorization(uuid,uuid);
CREATE FUNCTION pet_identity.staff_authorization(p_tenant uuid,p_employee uuid)
 RETURNS TABLE(display_name text,security_version bigint,authorization_version bigint,tenant_security_version bigint,store_ids uuid[],permission_code text,scope_type text,password_change_required boolean)
 LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
 SELECT e.display_name::text,e.security_version,e.authorization_version,t.security_version,
 ARRAY(SELECT s.id FROM public.identity_employee_store es JOIN public.platform_store s ON s.tenant_id=es.tenant_id AND s.id=es.store_id
       WHERE es.tenant_id=e.tenant_id AND es.employee_id=e.id AND s.status='ACTIVE' ORDER BY s.id),
 grants.permission_code::text,grants.scope_type::text,e.password_change_required
 FROM public.identity_employee e JOIN public.platform_tenant t ON t.id=e.tenant_id
 LEFT JOIN LATERAL (
   SELECT DISTINCT rp.permission_code,rp.scope_type FROM public.identity_employee_role er
   JOIN public.identity_role r ON r.tenant_id=er.tenant_id AND r.id=er.role_id AND r.status='ACTIVE'
   JOIN public.identity_role_permission rp ON rp.tenant_id=r.tenant_id AND rp.role_id=r.id
   WHERE er.tenant_id=e.tenant_id AND er.employee_id=e.id
 ) grants ON true
 WHERE t.id=p_tenant AND e.id=p_employee AND t.status='ACTIVE' AND e.status='ACTIVE'
$$;
REVOKE ALL ON FUNCTION pet_identity.lock_staff_credential(uuid),pet_identity.lock_staff_tenant(),pet_identity.staff_authorization(uuid,uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION pet_identity.lock_staff_credential(uuid),pet_identity.lock_staff_tenant(),pet_identity.staff_authorization(uuid,uuid) TO pet_runtime;
RESET ROLE;
SET ROLE pet_migrator;
REVOKE CREATE ON SCHEMA pet_identity FROM pet_auth_owner;
