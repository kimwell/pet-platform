package com.pet.platform.identity.infrastructure;

import com.pet.platform.identity.domain.Employee;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 正式实体装配时，启动核对实际账号权限；历史专用测试schema不包含正式实体。 */
@Component
public final class IdentityRuntimePermissions implements InitializingBean {
    private final EntityManagerFactory factory;
    private final JdbcTemplate jdbc;
    public IdentityRuntimePermissions(EntityManagerFactory factory,JdbcTemplate jdbc) { this.factory=factory;this.jdbc=jdbc; }
    @Override public void afterPropertiesSet() {
        if(factory.getMetamodel().getEntities().stream().noneMatch(e -> e.getJavaType()==Employee.class)) return;
        boolean safe=Boolean.TRUE.equals(jdbc.queryForObject("""
            select not (rolsuper or rolbypassrls or rolcreaterole or rolcreatedb)
            and pg_has_role(current_user,'pet_runtime','USAGE')
            and not pg_has_role(current_user,'pet_migrator','MEMBER')
            and not pg_has_role(current_user,'pet_auth_owner','MEMBER')
            and not pg_has_role(current_user,'pet_customer_auth_owner','MEMBER')
            and not has_schema_privilege(current_user,'pet_customer','CREATE')
            and not has_column_privilege(current_user,'public.customer_wechat_binding','open_id','SELECT')
            and not pg_has_role(current_user,'pet_bootstrap_owner','MEMBER')
            and not pg_has_role(current_user,'pet_bootstrap','MEMBER')
            and not pg_has_role(current_user,'pet_platform_bootstrap','MEMBER')
            and not pg_has_role(current_user,'pet_control_manager_owner','MEMBER')
            and not has_function_privilege(current_user,'pet_control.upgrade_management()','EXECUTE')
            and not pg_has_role(current_user,'pet_platform_auth_owner','MEMBER')
            and not pg_has_role(current_user,'pet_platform_bootstrap_owner','MEMBER')
            and not has_schema_privilege(current_user,'pet_control','CREATE')
            and not has_table_privilege(current_user,'pet_control.platform_account','SELECT')
            and not has_column_privilege(current_user,'pet_control.platform_account','password_hash','SELECT')
            and not has_function_privilege(current_user,'pet_control.bootstrap_platform(uuid,text,text,text)','EXECUTE')
            and not has_function_privilege(current_user,'pet_identity.upgrade_employee_read(uuid)','EXECUTE')
            and not has_function_privilege(current_user,'pet_identity.upgrade_identity_management(uuid)','EXECUTE')
            and not has_function_privilege(current_user,'pet_identity.upgrade_organizations(uuid)','EXECUTE')
            and not exists (select 1 from pg_catalog.pg_class c join pg_catalog.pg_namespace n on n.oid=c.relnamespace
                where n.nspname='pet_control' and c.relkind='r'
                and (pg_has_role(current_user,c.relowner,'MEMBER') or not c.relrowsecurity or not c.relforcerowsecurity or has_table_privilege(current_user,c.oid,'TRUNCATE')))
            and not has_schema_privilege(current_user,'public','CREATE')
            and not has_schema_privilege(current_user,'pet_identity','CREATE')
            and not has_column_privilege(current_user,'public.identity_employee','password_hash','SELECT')
            and not has_column_privilege(current_user,'public.identity_employee','system_reserved','UPDATE')
            and not exists (select 1 from pg_catalog.pg_class c join pg_catalog.pg_namespace n on n.oid=c.relnamespace
                where n.nspname='public' and c.relname in ('platform_tenant','platform_store','identity_employee','identity_role','identity_employee_role','identity_role_permission','identity_employee_store','identity_security_event','identity_session_cleanup','identity_management_event','identity_organization','customer_subject','customer_wechat_binding')
                and (pg_has_role(current_user,c.relowner,'MEMBER') or not c.relrowsecurity or not c.relforcerowsecurity
                     or has_table_privilege(current_user,c.oid,'TRUNCATE')))
            from pg_catalog.pg_roles where rolname=current_user
            """,Boolean.class));
        if(!safe) throw new IllegalStateException("正式身份数据库运行账号权限不符合要求");
    }
}
