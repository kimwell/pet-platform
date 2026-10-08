-- 控制面账号独立于租户；运行角色只有固定函数，无表读取或高权限旁路。
SET ROLE pet_migrator;
CREATE SCHEMA pet_control AUTHORIZATION pet_migrator;
REVOKE ALL ON SCHEMA pet_control FROM PUBLIC;
GRANT USAGE ON SCHEMA pet_control TO pet_runtime,pet_platform_bootstrap,pet_platform_auth_owner,pet_platform_bootstrap_owner;
CREATE TABLE pet_control.platform_account (
 id uuid PRIMARY KEY,
 login_name varchar(64) NOT NULL UNIQUE CHECK(login_name ~ '^[a-z0-9][a-z0-9._-]{0,63}$'),
 display_name varchar(100) NOT NULL CHECK(length(display_name)>0),
 password_hash varchar(256) NOT NULL CHECK(password_hash LIKE '$pbkdf2-sha256$v1$%'),
 status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','DISABLED')),
 security_version bigint NOT NULL DEFAULT 0 CHECK(security_version>=0),
 authorization_version bigint NOT NULL DEFAULT 0 CHECK(authorization_version>=0),
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
 created_at timestamp(3) with time zone NOT NULL DEFAULT clock_timestamp(),
 updated_at timestamp(3) with time zone NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE pet_control.platform_permission (
 account_id uuid NOT NULL REFERENCES pet_control.platform_account(id),
 permission_code varchar(64) NOT NULL CHECK(permission_code IN ('platform:session:manage','platform:credential:change','platform:redis:operate')),
 PRIMARY KEY(account_id,permission_code)
);
CREATE TABLE pet_control.platform_bootstrap (
 singleton boolean PRIMARY KEY CHECK(singleton),
 account_id uuid NOT NULL UNIQUE REFERENCES pet_control.platform_account(id),
 created_at timestamp(3) with time zone NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE pet_control.platform_security_event (
 id uuid PRIMARY KEY,
 principal_type varchar(16) NOT NULL DEFAULT 'PLATFORM' CHECK(principal_type='PLATFORM'),
 actor_id uuid REFERENCES pet_control.platform_account(id),
 target_id uuid REFERENCES pet_control.platform_account(id),
 operation varchar(32) NOT NULL CHECK(operation IN ('LOGIN','LOGOUT','CHANGE_PASSWORD','LOGOUT_ALL')),
 result varchar(40) NOT NULL,
 occurred_at timestamp(3) with time zone NOT NULL DEFAULT clock_timestamp(),
 trace_id varchar(64) NOT NULL CHECK(trace_id ~ '^[A-Za-z0-9_-]{1,64}$'),
 CHECK(actor_id IS NOT NULL OR (operation='LOGIN' AND result='LOGIN_FAILED'))
);
CREATE TABLE pet_control.platform_session_cleanup (
 account_id uuid NOT NULL REFERENCES pet_control.platform_account(id),
 revoke_before bigint NOT NULL CHECK(revoke_before>0),
 completed boolean NOT NULL DEFAULT false,
 created_at timestamp(3) with time zone NOT NULL DEFAULT clock_timestamp(),
 completed_at timestamp(3) with time zone,
 PRIMARY KEY(account_id,revoke_before)
);
-- 明确控制面表清单，不放松任何租户表策略。
ALTER TABLE pet_control.platform_account ENABLE ROW LEVEL SECURITY;
ALTER TABLE pet_control.platform_account FORCE ROW LEVEL SECURITY;
ALTER TABLE pet_control.platform_permission ENABLE ROW LEVEL SECURITY;
ALTER TABLE pet_control.platform_permission FORCE ROW LEVEL SECURITY;
ALTER TABLE pet_control.platform_bootstrap ENABLE ROW LEVEL SECURITY;
ALTER TABLE pet_control.platform_bootstrap FORCE ROW LEVEL SECURITY;
ALTER TABLE pet_control.platform_security_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE pet_control.platform_security_event FORCE ROW LEVEL SECURITY;
ALTER TABLE pet_control.platform_session_cleanup ENABLE ROW LEVEL SECURITY;
ALTER TABLE pet_control.platform_session_cleanup FORCE ROW LEVEL SECURITY;
REVOKE ALL ON ALL TABLES IN SCHEMA pet_control FROM PUBLIC;
GRANT SELECT,UPDATE(password_hash,security_version,version,updated_at) ON pet_control.platform_account TO pet_platform_auth_owner;
GRANT SELECT ON pet_control.platform_permission TO pet_platform_auth_owner;
GRANT INSERT ON pet_control.platform_security_event TO pet_platform_auth_owner;
GRANT SELECT,INSERT,UPDATE(completed,completed_at) ON pet_control.platform_session_cleanup TO pet_platform_auth_owner;
GRANT SELECT(id),INSERT ON pet_control.platform_account TO pet_platform_bootstrap_owner;
GRANT SELECT,INSERT ON pet_control.platform_bootstrap TO pet_platform_bootstrap_owner;
GRANT INSERT ON pet_control.platform_permission TO pet_platform_bootstrap_owner;
CREATE POLICY authentication ON pet_control.platform_account TO pet_platform_auth_owner USING(true) WITH CHECK(true);
CREATE POLICY authentication ON pet_control.platform_permission FOR SELECT TO pet_platform_auth_owner USING(true);
CREATE POLICY authentication ON pet_control.platform_security_event FOR INSERT TO pet_platform_auth_owner WITH CHECK(true);
CREATE POLICY authentication ON pet_control.platform_session_cleanup TO pet_platform_auth_owner USING(true) WITH CHECK(true);
CREATE POLICY bootstrap ON pet_control.platform_account TO pet_platform_bootstrap_owner USING(true) WITH CHECK(true);
CREATE POLICY bootstrap ON pet_control.platform_permission FOR INSERT TO pet_platform_bootstrap_owner WITH CHECK(true);
CREATE POLICY bootstrap ON pet_control.platform_bootstrap TO pet_platform_bootstrap_owner USING(true) WITH CHECK(true);
GRANT CREATE ON SCHEMA pet_control TO pet_platform_auth_owner,pet_platform_bootstrap_owner;
SET ROLE pet_platform_bootstrap_owner;
CREATE FUNCTION pet_control.bootstrap_platform(p_id uuid,p_login text,p_name text,p_hash text)
 RETURNS uuid LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
BEGIN
 PERFORM pg_catalog.pg_advisory_xact_lock(50404,1);
 IF EXISTS(SELECT 1 FROM pet_control.platform_bootstrap) OR EXISTS(SELECT id FROM pet_control.platform_account) THEN
  RAISE EXCEPTION 'platform initialization already exists' USING ERRCODE='P0001';
 END IF;
 INSERT INTO pet_control.platform_account(id,login_name,display_name,password_hash) VALUES(p_id,p_login,p_name,p_hash);
 INSERT INTO pet_control.platform_permission(account_id,permission_code) VALUES
  (p_id,'platform:session:manage'),(p_id,'platform:credential:change'),(p_id,'platform:redis:operate');
 INSERT INTO pet_control.platform_bootstrap(singleton,account_id) VALUES(true,p_id);
 RETURN p_id;
END $$;
REVOKE ALL ON FUNCTION pet_control.bootstrap_platform(uuid,text,text,text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION pet_control.bootstrap_platform(uuid,text,text,text) TO pet_platform_bootstrap;
SET ROLE pet_platform_auth_owner;
CREATE FUNCTION pet_control.authentication_candidate(p_login text)
 RETURNS TABLE(id uuid,password_hash text,security_version bigint)
 LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
 SELECT a.id,a.password_hash::text,a.security_version FROM pet_control.platform_account a WHERE a.login_name=p_login AND a.status='ACTIVE'
$$;
CREATE FUNCTION pet_control.authorization(p_id uuid)
 RETURNS TABLE(display_name text,security_version bigint,authorization_version bigint,permission_codes text[])
 LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
 SELECT a.display_name::text,a.security_version,a.authorization_version,
 ARRAY(SELECT p.permission_code::text FROM pet_control.platform_permission p WHERE p.account_id=a.id ORDER BY p.permission_code)
 FROM pet_control.platform_account a WHERE a.id=p_id AND a.status='ACTIVE'
$$;
-- 已认证敏感事务先由服务器设置本人的独立GUC；不使用tenant GUC。
CREATE FUNCTION pet_control.lock_credential()
 RETURNS TABLE(password_hash text,security_version bigint)
 LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
 SELECT a.password_hash::text,a.security_version FROM pet_control.platform_account a
 WHERE a.id=nullif(current_setting('pet.platform_id',true),'')::uuid AND a.status='ACTIVE' FOR UPDATE
$$;
CREATE FUNCTION pet_control.revoke_self(p_hash text)
 RETURNS bigint LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
DECLARE p_id uuid:=nullif(current_setting('pet.platform_id',true),'')::uuid; v bigint;
BEGIN
 UPDATE pet_control.platform_account SET password_hash=coalesce(p_hash,password_hash),security_version=security_version+1,version=version+1,updated_at=clock_timestamp()
 WHERE id=p_id AND status='ACTIVE' RETURNING security_version INTO v;
 IF v IS NULL THEN RAISE EXCEPTION 'platform account unavailable'; END IF;
 INSERT INTO pet_control.platform_session_cleanup(account_id,revoke_before) VALUES(p_id,v);
 RETURN v;
END $$;
CREATE FUNCTION pet_control.security_event(p_actor uuid,p_op text,p_result text,p_trace text)
 RETURNS void LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
 INSERT INTO pet_control.platform_security_event(id,actor_id,target_id,operation,result,trace_id)
 VALUES(gen_random_uuid(),p_actor,p_actor,p_op,p_result,p_trace)
$$;
CREATE FUNCTION pet_control.pending_cleanup(p_id uuid)
 RETURNS bigint LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
 SELECT coalesce(max(revoke_before),0) FROM pet_control.platform_session_cleanup WHERE account_id=p_id AND NOT completed
$$;
CREATE FUNCTION pet_control.complete_cleanup(p_id uuid,p_cutoff bigint)
 RETURNS void LANGUAGE sql VOLATILE SECURITY DEFINER SET search_path=pg_catalog,pg_temp AS $$
 UPDATE pet_control.platform_session_cleanup SET completed=true,completed_at=clock_timestamp() WHERE account_id=p_id AND revoke_before<=p_cutoff AND NOT completed
$$;
REVOKE ALL ON FUNCTION pet_control.authentication_candidate(text),pet_control.authorization(uuid),pet_control.lock_credential(),pet_control.revoke_self(text),pet_control.security_event(uuid,text,text,text),pet_control.pending_cleanup(uuid),pet_control.complete_cleanup(uuid,bigint) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION pet_control.authentication_candidate(text),pet_control.authorization(uuid),pet_control.lock_credential(),pet_control.revoke_self(text),pet_control.security_event(uuid,text,text,text),pet_control.pending_cleanup(uuid),pet_control.complete_cleanup(uuid,bigint) TO pet_runtime;
RESET ROLE;
SET ROLE pet_migrator;
REVOKE CREATE ON SCHEMA pet_control FROM pet_platform_auth_owner,pet_platform_bootstrap_owner;
