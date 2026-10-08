-- 仅安全测试夹具；生产Flyway不扫描此目录。
CREATE TABLE safety_store_fact (
    id uuid PRIMARY KEY, tenant_id uuid NOT NULL, UNIQUE (tenant_id, id)
);
CREATE TABLE safety_parent (
    id uuid PRIMARY KEY, tenant_id uuid NOT NULL,
    created_at timestamp(3) with time zone NOT NULL, updated_at timestamp(3) with time zone NOT NULL,
    version bigint NOT NULL, code varchar(255) NOT NULL, display_name varchar(255) NOT NULL,
    UNIQUE (tenant_id, id), UNIQUE (tenant_id, code)
);
CREATE TABLE safety_child (
    id uuid PRIMARY KEY, tenant_id uuid NOT NULL,
    created_at timestamp(3) with time zone NOT NULL, updated_at timestamp(3) with time zone NOT NULL,
    version bigint NOT NULL, parent_id uuid NOT NULL, display_name varchar(255) NOT NULL,
    UNIQUE (tenant_id, id),
    FOREIGN KEY (tenant_id, parent_id) REFERENCES safety_parent (tenant_id, id) ON DELETE RESTRICT
);
CREATE TABLE safety_store_resource (
    id uuid PRIMARY KEY, tenant_id uuid NOT NULL, store_id uuid NOT NULL,
    created_at timestamp(3) with time zone NOT NULL, updated_at timestamp(3) with time zone NOT NULL,
    version bigint NOT NULL, code varchar(255) NOT NULL, display_name varchar(255) NOT NULL,
    owner_type varchar(255) NOT NULL, owner_id uuid NOT NULL,
    UNIQUE (tenant_id, id), UNIQUE (tenant_id, code),
    FOREIGN KEY (tenant_id, store_id) REFERENCES safety_store_fact (tenant_id, id) ON DELETE RESTRICT
);
CREATE TABLE safety_owned_resource (
    id uuid PRIMARY KEY, tenant_id uuid NOT NULL,
    created_at timestamp(3) with time zone NOT NULL, updated_at timestamp(3) with time zone NOT NULL,
    version bigint NOT NULL, owner_type varchar(255) NOT NULL, owner_id uuid NOT NULL,
    display_name varchar(255) NOT NULL, UNIQUE (tenant_id, id)
);
-- 每个租户表显式ENABLE/FORCE；事实查询也只能读取当前租户。
ALTER TABLE safety_store_fact ENABLE ROW LEVEL SECURITY;
ALTER TABLE safety_store_fact FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_boundary ON safety_store_fact TO security_probe_runtime
    USING (tenant_id = nullif(current_setting('pet.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('pet.tenant_id', true), '')::uuid);
ALTER TABLE safety_parent ENABLE ROW LEVEL SECURITY;
ALTER TABLE safety_parent FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_boundary ON safety_parent TO security_probe_runtime
    USING (tenant_id = nullif(current_setting('pet.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('pet.tenant_id', true), '')::uuid);
ALTER TABLE safety_child ENABLE ROW LEVEL SECURITY;
ALTER TABLE safety_child FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_boundary ON safety_child TO security_probe_runtime
    USING (tenant_id = nullif(current_setting('pet.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('pet.tenant_id', true), '')::uuid);
ALTER TABLE safety_store_resource ENABLE ROW LEVEL SECURITY;
ALTER TABLE safety_store_resource FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_boundary ON safety_store_resource TO security_probe_runtime
    USING (tenant_id = nullif(current_setting('pet.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('pet.tenant_id', true), '')::uuid);
ALTER TABLE safety_owned_resource ENABLE ROW LEVEL SECURITY;
ALTER TABLE safety_owned_resource FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_boundary ON safety_owned_resource TO security_probe_runtime
    USING (tenant_id = nullif(current_setting('pet.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('pet.tenant_id', true), '')::uuid);
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO security_probe_runtime;
GRANT SELECT, INSERT, UPDATE, DELETE ON safety_parent, safety_child, safety_store_resource, safety_owned_resource TO security_probe_runtime;
GRANT SELECT ON safety_store_fact TO security_probe_runtime;
