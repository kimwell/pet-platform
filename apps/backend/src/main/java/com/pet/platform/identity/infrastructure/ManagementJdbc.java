package com.pet.platform.identity.infrastructure;

import com.pet.platform.identity.application.management.*;
import com.pet.platform.identity.application.management.ManagementModels.*;
import com.pet.platform.shared.tenancy.*;
import com.pet.platform.shared.api.*;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.observability.TraceContext;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** B01登记的固定SQL；无动态表/列/权限输入，RLS及可信租户条件覆盖全部DML。 */
@Repository
public class ManagementJdbc implements ManagementStore {
    public List<OrganizationView> organizations(int page,int size){return jdbc.query("select id,code,name,status,version from identity_organization where tenant_id=? order by code,id limit ? offset ?",(r,n)->new OrganizationView(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4),Long.toString(r.getLong(5))),tenant(),size,((long)page-1)*size);}
    public long organizationCount(){return jdbc.queryForObject("select count(*) from identity_organization where tenant_id=?",Long.class,tenant());}
    public OrganizationView organization(UUID id){return jdbc.query("select id,code,name,status,version from identity_organization where tenant_id=? and id=?",(r,n)->new OrganizationView(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4),Long.toString(r.getLong(5))),tenant(),id).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));}
    public UUID createOrganization(String code,String name){UUID id=UUID.randomUUID();jdbc.update("insert into identity_organization(id,tenant_id,code,name,status) values(?,?,?,?,'ACTIVE')",id,tenant(),code,name);return id;}
    public void editOrganization(UUID id,String name){jdbc.update("update identity_organization set name=?,version=version+1,updated_at=clock_timestamp() where tenant_id=? and id=?",name,tenant(),id);}
    public void organizationStatus(UUID id,String status){jdbc.update("update identity_organization set status=?,version=version+1,updated_at=clock_timestamp() where tenant_id=? and id=?",status,tenant(),id);}
    private final JdbcTemplate jdbc;
    public ManagementJdbc(JdbcTemplate jdbc){this.jdbc=jdbc;}
    private UUID tenant(){return TenantScopeGuard.requireBusiness().tenantId();}
    @Override public void serializeTenant(){jdbc.queryForObject("select pg_advisory_xact_lock(hashtextextended(?::text,0))",Object.class,tenant().toString());jdbc.query("select id from identity_employee where tenant_id=? order by id for update",(r,n)->r.getObject(1,UUID.class),tenant());}
    @Override public EmployeeManagement employee(UUID id){
        var rows=jdbc.query("select id,login_name,display_name,status,version,system_reserved,password_change_required from identity_employee where tenant_id=? and id=?",(r,n)->new EmployeeManagement(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4),r.getString(5),List.of(),List.of(),r.getBoolean(6),r.getBoolean(7)),tenant(),id);
        if(rows.isEmpty())throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        var e=rows.getFirst();var roleIds=jdbc.query("select role_id from identity_employee_role where tenant_id=? and employee_id=? order by role_id",(r,n)->r.getObject(1,UUID.class),tenant(),id);
        var storeIds=jdbc.query("select store_id from identity_employee_store where tenant_id=? and employee_id=? order by store_id",(r,n)->r.getObject(1,UUID.class),tenant(),id);
        boolean admin=jdbc.queryForObject("select exists(select 1 from identity_employee_role er join identity_role r on r.tenant_id=er.tenant_id and r.id=er.role_id where er.tenant_id=? and er.employee_id=? and r.code='tenant-admin')",Boolean.class,tenant(),id);
        return new EmployeeManagement(e.id(),e.loginName(),e.displayName(),e.status(),e.version(),roleIds,storeIds,e.protectedAccount()||admin,e.passwordChangeRequired());
    }
    private org.springframework.jdbc.core.RowMapper<RoleSummary> roleMapper(){return (r,n)->new RoleSummary(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4),r.getString(5),"tenant-admin".equals(r.getString(2)));}
    @Override public List<RoleSummary> roles(int page,int size){return jdbc.query("select id,code,name,status,version from identity_role where tenant_id=? order by code,id limit ? offset ?",roleMapper(),tenant(),size,(long)(page-1)*size);}
    @Override public long roleCount(){return jdbc.queryForObject("select count(*) from identity_role where tenant_id=?",Long.class,tenant());}
    @Override public RoleDetail role(UUID id){
        var rows=jdbc.query("select id,code,name,status,version from identity_role where tenant_id=? and id=?",roleMapper(),tenant(),id);
        if(rows.isEmpty())throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        var grants=jdbc.query("select permission_code,scope_type from identity_role_permission where tenant_id=? and role_id=? order by permission_code,scope_type",(r,n)->new PermissionGrant(r.getString(1),DataScopeType.valueOf(r.getString(2))),tenant(),id);
        return new RoleDetail(rows.getFirst(),grants);
    }
    @Override public List<StoreOption> stores(int page,int size){
        var c=TenantScopeGuard.requireBusiness();var ids=c.dataScope().types().contains(DataScopeType.TENANT)?c.authorizedStoreIds():c.dataScope().storeIds();
        return jdbc.query("select id,code,name from platform_store where tenant_id=? and status='ACTIVE' and id=any(?::uuid[]) order by code,id limit ? offset ?",(r,n)->new StoreOption(r.getObject(1,UUID.class),r.getString(2),r.getString(3)),tenant(),ids.toArray(UUID[]::new),size,(long)(page-1)*size);
    }
    @Override public long storeCount(){var c=TenantScopeGuard.requireBusiness();var ids=c.dataScope().types().contains(DataScopeType.TENANT)?c.authorizedStoreIds():c.dataScope().storeIds();return jdbc.queryForObject("select count(*) from platform_store where tenant_id=? and status='ACTIVE' and id=any(?::uuid[])",Long.class,tenant(),ids.toArray(UUID[]::new));}
    @Override public UUID createEmployee(String login,String name,String hash){if(jdbc.queryForObject("select exists(select 1 from identity_employee where tenant_id=? and login_name=?)",Boolean.class,tenant(),login))throw new BusinessException(ErrorCode.VALIDATION_FAILED,List.of(new FieldErrorDetail("loginName","DUPLICATE","账号已存在")));UUID id=UUID.randomUUID();jdbc.update("insert into identity_employee(id,tenant_id,login_name,display_name,status,password_hash,password_change_required) values(?,?,?,?,'ACTIVE',?,true)",id,tenant(),login,name,hash);return id;}
    @Override public void editEmployee(UUID id,String name){jdbc.update("update identity_employee set display_name=?,version=version+1,updated_at=clock_timestamp() where tenant_id=? and id=?",name,tenant(),id);}
    @Override public void status(UUID id,String status){jdbc.update("update identity_employee set status=? where tenant_id=? and id=?",status,tenant(),id);}
    @Override public void assignments(UUID id,List<UUID> ids,boolean roles){
        if(roles){jdbc.update("delete from identity_employee_role where tenant_id=? and employee_id=?",tenant(),id);for(UUID role:ids)jdbc.update("insert into identity_employee_role(id,tenant_id,employee_id,role_id) values(?,?,?,?)",UUID.randomUUID(),tenant(),id,role);}
        else{jdbc.update("delete from identity_employee_store where tenant_id=? and employee_id=?",tenant(),id);for(UUID store:ids)jdbc.update("insert into identity_employee_store(id,tenant_id,employee_id,store_id) values(?,?,?,?)",UUID.randomUUID(),tenant(),id,store);}
    }
    @Override public void authorizationChanged(UUID id){jdbc.update("update identity_employee set authorization_version=authorization_version+1,updated_at=clock_timestamp() where tenant_id=? and id=?",tenant(),id);}
    @Override public UUID createRole(String code,String name){if(jdbc.queryForObject("select exists(select 1 from identity_role where tenant_id=? and code=?)",Boolean.class,tenant(),code))throw new BusinessException(ErrorCode.VALIDATION_FAILED,List.of(new FieldErrorDetail("code","DUPLICATE","角色编码已存在")));UUID id=UUID.randomUUID();jdbc.update("insert into identity_role(id,tenant_id,code,name,status) values(?,?,?,?,'ACTIVE')",id,tenant(),code,name);return id;}
    @Override public void editRole(UUID id,String name,String status){jdbc.update("update identity_role set name=?,status=?,version=version+1,updated_at=clock_timestamp() where tenant_id=? and id=?",name,status,tenant(),id);}
    @Override public void grants(UUID id,List<PermissionGrant> grants){jdbc.update("delete from identity_role_permission where tenant_id=? and role_id=?",tenant(),id);for(var g:grants)jdbc.update("insert into identity_role_permission(id,tenant_id,role_id,permission_code,scope_type) values(?,?,?,?,?)",UUID.randomUUID(),tenant(),id,g.permissionCode(),g.scopeType().name());jdbc.update("update identity_role set version=version+1,updated_at=clock_timestamp() where tenant_id=? and id=?",tenant(),id);}
    @Override public List<UUID> roleEmployees(UUID role){return jdbc.query("select employee_id from identity_employee_role where tenant_id=? and role_id=? order by employee_id",(r,n)->r.getObject(1,UUID.class),tenant(),role);}
    @Override public void lockEmployees(Collection<UUID> ids){for(UUID id:new TreeSet<>(ids))jdbc.query("select id from identity_employee where tenant_id=? and id=? for update",(r,n)->r.getObject(1,UUID.class),tenant(),id);}
    @Override public void event(UUID target,String operation){jdbc.update("insert into identity_management_event(id,tenant_id,operator_id,target_id,operation,trace_id) values(?,?,?,?,?,?)",UUID.randomUUID(),tenant(),TenantScopeGuard.requireBusiness().principalId(),target,operation,TraceContext.currentId());}
}
