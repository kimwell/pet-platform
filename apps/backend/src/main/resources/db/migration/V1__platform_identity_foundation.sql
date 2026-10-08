-- P05-01 首次正式身份结构；前置角色由独立管理员脚本配置。
-- Flyway会恢复连接初始role；每个正式结构迁移显式进入能力角色，history由迁移登录身份维护。
SET ROLE pet_migrator;
DO $$ BEGIN
 IF current_user <> 'pet_migrator' THEN RAISE EXCEPTION '迁移必须以 pet_migrator 执行'; END IF;
 IF EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname IN ('pet_migrator','pet_runtime','pet_bootstrap','pet_auth_owner','pet_bootstrap_owner') AND (rolsuper OR rolbypassrls OR rolcreaterole OR rolcreatedb OR rolcanlogin))
 OR (SELECT count(*) FROM pg_catalog.pg_roles WHERE rolname IN ('pet_migrator','pet_runtime','pet_bootstrap','pet_auth_owner','pet_bootstrap_owner')) <> 5
 THEN RAISE EXCEPTION '身份数据库能力角色配置不符合安全要求'; END IF;
END $$;
CREATE SCHEMA pet_identity AUTHORIZATION pet_migrator;
REVOKE ALL ON SCHEMA pet_identity FROM PUBLIC;
GRANT USAGE ON SCHEMA public, pet_identity TO pet_runtime, pet_bootstrap, pet_auth_owner, pet_bootstrap_owner;
CREATE TABLE public.platform_tenant (
 id uuid PRIMARY KEY,
 code varchar(32) NOT NULL UNIQUE CHECK (code ~ '^[a-z0-9][a-z0-9-]{0,31}$'),
 name varchar(100) NOT NULL CHECK (length(btrim(name)) BETWEEN 1 AND 100),
 status varchar(16) NOT NULL CHECK (status IN ('ACTIVE','DISABLED')),
 security_version bigint NOT NULL DEFAULT 0 CHECK (security_version >= 0),
 created_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 updated_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);
CREATE TABLE public.platform_store (
 id uuid PRIMARY KEY,
 tenant_id uuid NOT NULL REFERENCES public.platform_tenant(id) ON DELETE RESTRICT,
 code varchar(32) NOT NULL CHECK (code ~ '^[a-z0-9][a-z0-9-]{0,31}$'),
 name varchar(100) NOT NULL CHECK (length(btrim(name)) BETWEEN 1 AND 100),
 status varchar(16) NOT NULL CHECK (status IN ('ACTIVE','DISABLED')),
 UNIQUE (tenant_id,code),
 created_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 updated_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
 UNIQUE (tenant_id,id)
);
CREATE TABLE public.identity_employee (
 id uuid PRIMARY KEY,
 tenant_id uuid NOT NULL REFERENCES public.platform_tenant(id) ON DELETE RESTRICT,
 login_name varchar(64) NOT NULL CHECK (login_name ~ '^[a-z0-9][a-z0-9._-]{0,63}$'),
 display_name varchar(100) NOT NULL CHECK (length(btrim(display_name)) BETWEEN 1 AND 100),
 status varchar(16) NOT NULL CHECK (status IN ('ACTIVE','DISABLED')),
 password_hash varchar(256) NOT NULL CHECK (password_hash ~ '^\$pbkdf2-sha256\$v1\$[0-9]{6,7}\$[A-Za-z0-9+/]{43}=\$[A-Za-z0-9+/]{43}=$'),
 security_version bigint NOT NULL DEFAULT 0 CHECK (security_version >= 0),
 authorization_version bigint NOT NULL DEFAULT 0 CHECK (authorization_version >= 0),
 UNIQUE (tenant_id,login_name),
 created_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 updated_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
 UNIQUE (tenant_id,id)
);
CREATE TABLE public.identity_role (
 id uuid PRIMARY KEY,
 tenant_id uuid NOT NULL REFERENCES public.platform_tenant(id) ON DELETE RESTRICT,
 code varchar(32) NOT NULL CHECK (code ~ '^[a-z0-9][a-z0-9-]{0,31}$'),
 name varchar(100) NOT NULL CHECK (length(btrim(name)) BETWEEN 1 AND 100),
 status varchar(16) NOT NULL CHECK (status IN ('ACTIVE','DISABLED')),
 UNIQUE (tenant_id,code),
 created_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 updated_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
 UNIQUE (tenant_id,id)
);
CREATE TABLE public.identity_employee_role (
 id uuid PRIMARY KEY,
 tenant_id uuid NOT NULL REFERENCES public.platform_tenant(id) ON DELETE RESTRICT,
 employee_id uuid NOT NULL,
 role_id uuid NOT NULL,
 UNIQUE (tenant_id,employee_id,role_id),
 FOREIGN KEY (tenant_id,employee_id) REFERENCES public.identity_employee(tenant_id,id) ON DELETE RESTRICT,
 FOREIGN KEY (tenant_id,role_id) REFERENCES public.identity_role(tenant_id,id) ON DELETE RESTRICT,
 created_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 updated_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
 UNIQUE (tenant_id,id)
);
CREATE TABLE public.identity_role_permission (
 id uuid PRIMARY KEY,
 tenant_id uuid NOT NULL REFERENCES public.platform_tenant(id) ON DELETE RESTRICT,
 role_id uuid NOT NULL,
 permission_code varchar(100) NOT NULL CHECK (permission_code ~ '^[a-z][a-z0-9-]*:[a-z][a-z0-9-]*:[a-z][a-z0-9-]*$'),
 scope_type varchar(16) NOT NULL CHECK (scope_type IN ('TENANT','STORES','SELF')),
 UNIQUE (tenant_id,role_id,permission_code,scope_type),
 FOREIGN KEY (tenant_id,role_id) REFERENCES public.identity_role(tenant_id,id) ON DELETE RESTRICT,
 created_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 updated_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
 UNIQUE (tenant_id,id)
);
CREATE TABLE public.identity_employee_store (
 id uuid PRIMARY KEY,
 tenant_id uuid NOT NULL REFERENCES public.platform_tenant(id) ON DELETE RESTRICT,
 employee_id uuid NOT NULL,
 store_id uuid NOT NULL,
 UNIQUE (tenant_id,employee_id,store_id),
 FOREIGN KEY (tenant_id,employee_id) REFERENCES public.identity_employee(tenant_id,id) ON DELETE RESTRICT,
 FOREIGN KEY (tenant_id,store_id) REFERENCES public.platform_store(tenant_id,id) ON DELETE RESTRICT,
 created_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 updated_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
 UNIQUE (tenant_id,id)
);
CREATE INDEX employee_role_role_idx ON public.identity_employee_role(tenant_id,role_id);
CREATE INDEX employee_store_store_idx ON public.identity_employee_store(tenant_id,store_id);
CREATE INDEX store_active_idx ON public.platform_store(tenant_id,status,id);
ALTER TABLE public.platform_tenant ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.platform_tenant FORCE ROW LEVEL SECURITY;
CREATE POLICY runtime_tenant ON public.platform_tenant TO pet_runtime
 USING (id=nullif(pg_catalog.current_setting('pet.tenant_id',true),'')::uuid)
 WITH CHECK (id=nullif(pg_catalog.current_setting('pet.tenant_id',true),'')::uuid);
CREATE POLICY authentication_read ON public.platform_tenant FOR SELECT TO pet_auth_owner USING (true);
CREATE POLICY bootstrap_read ON public.platform_tenant FOR SELECT TO pet_bootstrap_owner USING (true);
CREATE POLICY bootstrap_insert ON public.platform_tenant FOR INSERT TO pet_bootstrap_owner WITH CHECK (true);
REVOKE ALL ON public.platform_tenant FROM PUBLIC;
GRANT SELECT ON public.platform_tenant TO pet_auth_owner;
GRANT SELECT, INSERT ON public.platform_tenant TO pet_bootstrap_owner;
GRANT SELECT ON public.platform_tenant TO pet_runtime;
ALTER TABLE public.platform_store ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.platform_store FORCE ROW LEVEL SECURITY;
CREATE POLICY runtime_tenant ON public.platform_store TO pet_runtime
 USING (tenant_id=nullif(pg_catalog.current_setting('pet.tenant_id',true),'')::uuid)
 WITH CHECK (tenant_id=nullif(pg_catalog.current_setting('pet.tenant_id',true),'')::uuid);
CREATE POLICY authentication_read ON public.platform_store FOR SELECT TO pet_auth_owner USING (true);
CREATE POLICY bootstrap_insert ON public.platform_store FOR INSERT TO pet_bootstrap_owner WITH CHECK (true);
REVOKE ALL ON public.platform_store FROM PUBLIC;
GRANT SELECT ON public.platform_store TO pet_auth_owner;
GRANT INSERT ON public.platform_store TO pet_bootstrap_owner;
GRANT SELECT ON public.platform_store TO pet_runtime;
ALTER TABLE public.identity_employee ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.identity_employee FORCE ROW LEVEL SECURITY;
CREATE POLICY runtime_tenant ON public.identity_employee TO pet_runtime
 USING (tenant_id=nullif(pg_catalog.current_setting('pet.tenant_id',true),'')::uuid)
 WITH CHECK (tenant_id=nullif(pg_catalog.current_setting('pet.tenant_id',true),'')::uuid);
CREATE POLICY authentication_read ON public.identity_employee FOR SELECT TO pet_auth_owner USING (true);
CREATE POLICY bootstrap_insert ON public.identity_employee FOR INSERT TO pet_bootstrap_owner WITH CHECK (true);
REVOKE ALL ON public.identity_employee FROM PUBLIC;
GRANT SELECT ON public.identity_employee TO pet_auth_owner;
GRANT INSERT ON public.identity_employee TO pet_bootstrap_owner;
ALTER TABLE public.identity_role ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.identity_role FORCE ROW LEVEL SECURITY;
CREATE POLICY runtime_tenant ON public.identity_role TO pet_runtime
 USING (tenant_id=nullif(pg_catalog.current_setting('pet.tenant_id',true),'')::uuid)
 WITH CHECK (tenant_id=nullif(pg_catalog.current_setting('pet.tenant_id',true),'')::uuid);
CREATE POLICY authentication_read ON public.identity_role FOR SELECT TO pet_auth_owner USING (true);
CREATE POLICY bootstrap_insert ON public.identity_role FOR INSERT TO pet_bootstrap_owner WITH CHECK (true);
REVOKE ALL ON public.identity_role FROM PUBLIC;
GRANT SELECT ON public.identity_role TO pet_auth_owner;
GRANT INSERT ON public.identity_role TO pet_bootstrap_owner;
GRANT SELECT ON public.identity_role TO pet_runtime;
ALTER TABLE public.identity_employee_role ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.identity_employee_role FORCE ROW LEVEL SECURITY;
CREATE POLICY runtime_tenant ON public.identity_employee_role TO pet_runtime
 USING (tenant_id=nullif(pg_catalog.current_setting('pet.tenant_id',true),'')::uuid)
 WITH CHECK (tenant_id=nullif(pg_catalog.current_setting('pet.tenant_id',true),'')::uuid);
CREATE POLICY authentication_read ON public.identity_employee_role FOR SELECT TO pet_auth_owner USING (true);
CREATE POLICY bootstrap_insert ON public.identity_employee_role FOR INSERT TO pet_bootstrap_owner WITH CHECK (true);
REVOKE ALL ON public.identity_employee_role FROM PUBLIC;
GRANT SELECT ON public.identity_employee_role TO pet_auth_owner;
GRANT INSERT ON public.identity_employee_role TO pet_bootstrap_owner;
GRANT SELECT ON public.identity_employee_role TO pet_runtime;
ALTER TABLE public.identity_role_permission ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.identity_role_permission FORCE ROW LEVEL SECURITY;
CREATE POLICY runtime_tenant ON public.identity_role_permission TO pet_runtime
 USING (tenant_id=nullif(pg_catalog.current_setting('pet.tenant_id',true),'')::uuid)
 WITH CHECK (tenant_id=nullif(pg_catalog.current_setting('pet.tenant_id',true),'')::uuid);
CREATE POLICY authentication_read ON public.identity_role_permission FOR SELECT TO pet_auth_owner USING (true);
CREATE POLICY bootstrap_insert ON public.identity_role_permission FOR INSERT TO pet_bootstrap_owner WITH CHECK (true);
REVOKE ALL ON public.identity_role_permission FROM PUBLIC;
GRANT SELECT ON public.identity_role_permission TO pet_auth_owner;
GRANT INSERT ON public.identity_role_permission TO pet_bootstrap_owner;
GRANT SELECT ON public.identity_role_permission TO pet_runtime;
ALTER TABLE public.identity_employee_store ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.identity_employee_store FORCE ROW LEVEL SECURITY;
CREATE POLICY runtime_tenant ON public.identity_employee_store TO pet_runtime
 USING (tenant_id=nullif(pg_catalog.current_setting('pet.tenant_id',true),'')::uuid)
 WITH CHECK (tenant_id=nullif(pg_catalog.current_setting('pet.tenant_id',true),'')::uuid);
CREATE POLICY authentication_read ON public.identity_employee_store FOR SELECT TO pet_auth_owner USING (true);
CREATE POLICY bootstrap_insert ON public.identity_employee_store FOR INSERT TO pet_bootstrap_owner WITH CHECK (true);
REVOKE ALL ON public.identity_employee_store FROM PUBLIC;
GRANT SELECT ON public.identity_employee_store TO pet_auth_owner;
GRANT INSERT ON public.identity_employee_store TO pet_bootstrap_owner;
GRANT SELECT ON public.identity_employee_store TO pet_runtime;
-- 普通运行角色连本租户也不能直接读取凭据；JPA仅validate此映射。
GRANT SELECT (id,tenant_id,login_name,display_name,status,security_version,authorization_version,created_at,updated_at,version)
 ON public.identity_employee TO pet_runtime;
GRANT CREATE ON SCHEMA pet_identity TO pet_auth_owner, pet_bootstrap_owner;
SET ROLE pet_auth_owner;
CREATE FUNCTION pet_identity.authentication_candidate(p_code text,p_login text)
 RETURNS TABLE(tenant_id uuid,employee_id uuid,password_hash text,employee_status text,security_version bigint,authorization_version bigint,tenant_security_version bigint)
 LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
 SELECT t.id,e.id,e.password_hash::text,e.status::text,e.security_version,e.authorization_version,t.security_version
 FROM public.platform_tenant t JOIN public.identity_employee e ON e.tenant_id=t.id
 WHERE t.code=p_code AND t.status='ACTIVE' AND e.login_name=p_login
 AND p_code ~ '^[a-z0-9][a-z0-9-]{0,31}$' AND p_login ~ '^[a-z0-9][a-z0-9._-]{0,63}$'
$$;
CREATE FUNCTION pet_identity.staff_authorization(p_tenant uuid,p_employee uuid)
 RETURNS TABLE(display_name text,security_version bigint,authorization_version bigint,tenant_security_version bigint,store_ids uuid[],permission_code text,scope_type text)
 LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
 SELECT e.display_name::text,e.security_version,e.authorization_version,t.security_version,
 ARRAY(SELECT s.id FROM public.identity_employee_store es JOIN public.platform_store s ON s.tenant_id=es.tenant_id AND s.id=es.store_id
       WHERE es.tenant_id=e.tenant_id AND es.employee_id=e.id AND s.status='ACTIVE' ORDER BY s.id),
 grants.permission_code::text,grants.scope_type::text
 FROM public.identity_employee e JOIN public.platform_tenant t ON t.id=e.tenant_id
 LEFT JOIN LATERAL (
   SELECT DISTINCT rp.permission_code,rp.scope_type FROM public.identity_employee_role er
   JOIN public.identity_role r ON r.tenant_id=er.tenant_id AND r.id=er.role_id AND r.status='ACTIVE'
   JOIN public.identity_role_permission rp ON rp.tenant_id=r.tenant_id AND rp.role_id=r.id
   WHERE er.tenant_id=e.tenant_id AND er.employee_id=e.id
 ) grants ON true
 WHERE t.id=p_tenant AND e.id=p_employee AND t.status='ACTIVE' AND e.status='ACTIVE'
$$;
REVOKE ALL ON FUNCTION pet_identity.authentication_candidate(text,text),pet_identity.staff_authorization(uuid,uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION pet_identity.authentication_candidate(text,text),pet_identity.staff_authorization(uuid,uuid) TO pet_runtime;
RESET ROLE;
SET ROLE pet_bootstrap_owner;
CREATE FUNCTION pet_identity.bootstrap_tenant(p_code text,p_name text,p_login text,p_hash text,p_store_code text,p_store_name text)
 RETURNS TABLE(tenant_id uuid,employee_id uuid,store_id uuid)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
DECLARE t uuid := pg_catalog.gen_random_uuid(); e uuid := pg_catalog.gen_random_uuid(); r uuid := pg_catalog.gen_random_uuid(); s uuid;
BEGIN
 IF p_code IS NULL OR p_code !~ '^[a-z0-9][a-z0-9-]{0,31}$' OR p_login IS NULL OR p_login !~ '^[a-z0-9][a-z0-9._-]{0,63}$'
 OR p_name IS NULL OR length(btrim(p_name)) NOT BETWEEN 1 AND 100
 OR p_hash IS NULL OR p_hash !~ '^\$pbkdf2-sha256\$v1\$600000\$[A-Za-z0-9+/]{43}=\$[A-Za-z0-9+/]{43}=$'
 OR (p_store_code IS NULL) <> (p_store_name IS NULL)
 OR (p_store_code IS NOT NULL AND (p_store_code !~ '^[a-z0-9][a-z0-9-]{0,31}$' OR length(btrim(p_store_name)) NOT BETWEEN 1 AND 100))
 THEN RAISE EXCEPTION USING ERRCODE='22023', MESSAGE='初始化输入不合法'; END IF;
 PERFORM pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(p_code,0));
 IF EXISTS (SELECT 1 FROM public.platform_tenant WHERE code=p_code) THEN
   RAISE EXCEPTION USING ERRCODE='P0001',MESSAGE='租户已存在，初始化不会覆盖或修复';
 END IF;
 INSERT INTO public.platform_tenant(id,code,name,status) VALUES (t,p_code,p_name,'ACTIVE');
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
 RETURN QUERY SELECT t,e,s;
EXCEPTION
 WHEN unique_violation THEN RAISE EXCEPTION USING ERRCODE='P0001',MESSAGE='初始化冲突，未修改既有数据';
 WHEN check_violation OR foreign_key_violation OR not_null_violation THEN
   RAISE EXCEPTION USING ERRCODE='22023',MESSAGE='初始化数据约束不满足，全部回滚';
END $$;
REVOKE ALL ON FUNCTION pet_identity.bootstrap_tenant(text,text,text,text,text,text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION pet_identity.bootstrap_tenant(text,text,text,text,text,text) TO pet_bootstrap;
RESET ROLE;
REVOKE CREATE ON SCHEMA pet_identity FROM pet_auth_owner,pet_bootstrap_owner;
