-- 只追加正式迁移；客户认证不借用员工/平台函数权限。
SET ROLE pet_migrator;
DO $$ BEGIN
 IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_roles WHERE rolname='pet_customer_auth_owner' AND NOT (rolsuper OR rolbypassrls OR rolcreaterole OR rolcreatedb OR rolcanlogin))
 THEN RAISE EXCEPTION '客户认证能力角色不符合安全要求'; END IF;
END $$;
CREATE SCHEMA pet_customer AUTHORIZATION pet_migrator;
REVOKE ALL ON SCHEMA pet_customer FROM PUBLIC;
GRANT USAGE ON SCHEMA public,pet_customer TO pet_runtime,pet_customer_auth_owner;
CREATE TABLE public.customer_subject (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL REFERENCES public.platform_tenant(id) ON DELETE RESTRICT,
 status varchar(16) NOT NULL CHECK(status IN ('ACTIVE','DISABLED')),
 security_version bigint NOT NULL DEFAULT 0 CHECK(security_version>=0),
 created_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 updated_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0), UNIQUE(tenant_id,id)
);
CREATE TABLE public.customer_wechat_binding (
 id uuid PRIMARY KEY, tenant_id uuid NOT NULL,
 customer_id uuid NOT NULL, app_id varchar(32) NOT NULL CHECK(app_id ~ '^wx[a-f0-9]{16}$'),
 open_id varchar(128) NOT NULL CHECK(open_id ~ '^[A-Za-z0-9_-]{1,128}$'),
 status varchar(16) NOT NULL CHECK(status IN ('ACTIVE','DISABLED')),
 created_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 updated_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
 CONSTRAINT customer_wechat_identity_unique UNIQUE(tenant_id,app_id,open_id),
 -- 当前只有微信注册，不开放附加绑定或自动合并，一个主体一条绑定。
 UNIQUE(tenant_id,customer_id), UNIQUE(tenant_id,id),
 FOREIGN KEY(tenant_id,customer_id) REFERENCES public.customer_subject(tenant_id,id) ON DELETE RESTRICT
);
CREATE TABLE pet_customer.security_event (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), tenant_id uuid REFERENCES public.platform_tenant(id),
 principal_type varchar(16) NOT NULL DEFAULT 'CUSTOMER' CHECK(principal_type='CUSTOMER'),
 customer_id uuid, result varchar(64) NOT NULL CHECK(result ~ '^[A-Z][A-Z0-9_]{0,63}$'),
 occurred_at timestamp(3) with time zone NOT NULL DEFAULT current_timestamp,
 trace_id varchar(32) NOT NULL CHECK(trace_id ~ '^[a-f0-9]{32}$'),
 FOREIGN KEY(tenant_id,customer_id) REFERENCES public.customer_subject(tenant_id,id),
 CHECK(customer_id IS NULL OR tenant_id IS NOT NULL)
);
ALTER TABLE public.customer_subject ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.customer_subject FORCE ROW LEVEL SECURITY;
ALTER TABLE public.customer_wechat_binding ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.customer_wechat_binding FORCE ROW LEVEL SECURITY;
ALTER TABLE pet_customer.security_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE pet_customer.security_event FORCE ROW LEVEL SECURITY;
CREATE POLICY runtime_tenant ON public.customer_subject TO pet_runtime USING(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid) WITH CHECK(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid);
CREATE POLICY runtime_tenant ON public.customer_wechat_binding TO pet_runtime USING(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid) WITH CHECK(tenant_id=nullif(current_setting('pet.tenant_id',true),'')::uuid);
CREATE POLICY customer_auth ON public.customer_subject TO pet_customer_auth_owner USING(true) WITH CHECK(true);
CREATE POLICY customer_auth ON public.customer_wechat_binding TO pet_customer_auth_owner USING(true) WITH CHECK(true);
CREATE POLICY customer_event ON pet_customer.security_event FOR INSERT TO pet_customer_auth_owner WITH CHECK(true);
CREATE POLICY customer_tenant_read ON public.platform_tenant FOR SELECT TO pet_customer_auth_owner USING(true);
-- FOR SHARE需要UPDATE USING；WITH CHECK false禁止实际更新租户。
CREATE POLICY customer_tenant_lock ON public.platform_tenant FOR UPDATE TO pet_customer_auth_owner USING(true) WITH CHECK(false);
REVOKE ALL ON public.customer_subject,public.customer_wechat_binding,pet_customer.security_event FROM PUBLIC;
GRANT SELECT,INSERT ON public.customer_subject,public.customer_wechat_binding TO pet_customer_auth_owner;
GRANT UPDATE(security_version,updated_at,version) ON public.customer_subject TO pet_customer_auth_owner;
GRANT SELECT(id,code,status,security_version) ON public.platform_tenant TO pet_customer_auth_owner;
-- 行锁需要UPDATE权限，但没有给runtime更新能力；owner仅由固定函数调用。
GRANT UPDATE(status) ON public.platform_tenant TO pet_customer_auth_owner;
GRANT INSERT ON pet_customer.security_event TO pet_customer_auth_owner;
GRANT SELECT ON public.customer_subject TO pet_runtime;
GRANT SELECT(id,tenant_id,customer_id,app_id,status,created_at,updated_at,version) ON public.customer_wechat_binding TO pet_runtime;
GRANT CREATE ON SCHEMA pet_customer TO pet_customer_auth_owner;
SET ROLE pet_customer_auth_owner;
CREATE FUNCTION pet_customer.resolve_tenant(p_code text) RETURNS TABLE(tenant_id uuid,security_version bigint)
 LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
 SELECT t.id,t.security_version FROM public.platform_tenant t WHERE t.code=p_code AND t.status='ACTIVE' AND p_code ~ '^[a-z0-9][a-z0-9-]{0,31}$'
$$;
CREATE FUNCTION pet_customer.current_identity(p_tenant uuid,p_customer uuid)
 RETURNS TABLE(customer_id uuid,tenant_id uuid,security_version bigint,tenant_security_version bigint)
 LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
 SELECT c.id,c.tenant_id,c.security_version,t.security_version
 FROM public.customer_subject c JOIN public.platform_tenant t ON t.id=c.tenant_id
 JOIN public.customer_wechat_binding b ON b.tenant_id=c.tenant_id AND b.customer_id=c.id
 WHERE c.tenant_id=p_tenant AND c.id=p_customer AND c.status='ACTIVE' AND t.status='ACTIVE' AND b.status='ACTIVE'
$$;
CREATE FUNCTION pet_customer.register_wechat(p_tenant uuid,p_app text,p_open text)
 RETURNS TABLE(customer_id uuid,tenant_id uuid,security_version bigint,tenant_security_version bigint)
 LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
DECLARE existing uuid; created uuid; conflicting text;
BEGIN
 IF p_tenant IS NULL OR p_app IS NULL OR p_app !~ '^wx[a-f0-9]{16}$' OR p_open IS NULL OR p_open !~ '^[A-Za-z0-9_-]{1,128}$'
 THEN RAISE EXCEPTION USING ERRCODE='22023',MESSAGE='微信身份字段不合法'; END IF;
 PERFORM t.id FROM public.platform_tenant t WHERE t.id=p_tenant AND t.status='ACTIVE' FOR SHARE;
 IF NOT FOUND THEN RETURN; END IF;
 SELECT b.customer_id INTO existing FROM public.customer_wechat_binding b WHERE b.tenant_id=p_tenant AND b.app_id=p_app AND b.open_id=p_open;
 IF existing IS NULL THEN
   BEGIN
     created:=gen_random_uuid();
     INSERT INTO public.customer_subject(id,tenant_id,status) VALUES(created,p_tenant,'ACTIVE');
     INSERT INTO public.customer_wechat_binding(id,tenant_id,customer_id,app_id,open_id,status) VALUES(gen_random_uuid(),p_tenant,created,p_app,p_open,'ACTIVE');
     existing:=created;
   EXCEPTION WHEN unique_violation THEN
     GET STACKED DIAGNOSTICS conflicting=CONSTRAINT_NAME;
     IF conflicting <> 'customer_wechat_identity_unique' THEN RAISE; END IF;
     -- 子事务回滚客户和绑定；仅准确唯一键冲突允许重读。
     SELECT b.customer_id INTO existing FROM public.customer_wechat_binding b WHERE b.tenant_id=p_tenant AND b.app_id=p_app AND b.open_id=p_open;
     IF existing IS NULL THEN RAISE; END IF;
   END;
 END IF;
 RETURN QUERY SELECT * FROM pet_customer.current_identity(p_tenant,existing);
END $$;
CREATE FUNCTION pet_customer.record_login(p_tenant uuid,p_customer uuid,p_result text,p_trace text) RETURNS void
 LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
 INSERT INTO pet_customer.security_event(tenant_id,customer_id,result,trace_id) VALUES(p_tenant,p_customer,p_result,p_trace)
$$;
CREATE FUNCTION pet_customer.revoke_sessions(p_tenant uuid,p_customer uuid,p_security bigint,p_tenant_security bigint,p_trace text) RETURNS bigint
 LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
DECLARE cutoff bigint;
BEGIN
 PERFORM t.id FROM public.platform_tenant t WHERE t.id=p_tenant AND t.status='ACTIVE' AND t.security_version=p_tenant_security FOR SHARE;
 IF NOT FOUND THEN RETURN NULL; END IF;
 PERFORM c.id FROM public.customer_subject c WHERE c.tenant_id=p_tenant AND c.id=p_customer AND c.status='ACTIVE' AND c.security_version=p_security FOR UPDATE;
 IF NOT FOUND OR NOT EXISTS(SELECT 1 FROM public.customer_wechat_binding b WHERE b.tenant_id=p_tenant AND b.customer_id=p_customer AND b.status='ACTIVE') THEN RETURN NULL; END IF;
 UPDATE public.customer_subject SET security_version=security_version+1,version=version+1,updated_at=current_timestamp WHERE tenant_id=p_tenant AND id=p_customer RETURNING security_version INTO cutoff;
 INSERT INTO pet_customer.security_event(tenant_id,customer_id,result,trace_id) VALUES(p_tenant,p_customer,'LOGOUT_ALL',p_trace);
 RETURN cutoff;
END $$;
REVOKE ALL ON FUNCTION pet_customer.revoke_sessions(uuid,uuid,bigint,bigint,text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION pet_customer.revoke_sessions(uuid,uuid,bigint,bigint,text) TO pet_runtime;
REVOKE ALL ON FUNCTION pet_customer.resolve_tenant(text),pet_customer.current_identity(uuid,uuid),pet_customer.register_wechat(uuid,text,text),pet_customer.record_login(uuid,uuid,text,text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION pet_customer.resolve_tenant(text),pet_customer.current_identity(uuid,uuid),pet_customer.register_wechat(uuid,text,text),pet_customer.record_login(uuid,uuid,text,text) TO pet_runtime;
RESET ROLE;
REVOKE CREATE ON SCHEMA pet_customer FROM pet_customer_auth_owner;
