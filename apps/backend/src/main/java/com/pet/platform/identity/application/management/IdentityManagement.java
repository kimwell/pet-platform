package com.pet.platform.identity.application.management;

import com.pet.platform.identity.application.*;
import com.pet.platform.identity.application.authentication.*;
import com.pet.platform.identity.application.management.ManagementModels.*;
import com.pet.platform.shared.api.*;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.tenancy.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.dao.*;

/** 管理权限独立选择，锁内重验正式身份；全部关联/版本/撤销意图/事件共享事务。 */
@Service
public class IdentityManagement {
    private final ManagementStore store;
    private final EmployeePolicy policy;
    private final StaffSecurityStore securityStore;
    private final StaffSecurityOperations security;
    private final StaffAuthentication authentication;
    private final StaffSessionPort sessions;
    private final PasswordService passwords;
    private final StoreScopeGuard stores;
    public IdentityManagement(ManagementStore store,EmployeePolicy policy,StaffSecurityStore securityStore,StaffSecurityOperations security,
            StaffAuthentication authentication,StaffSessionPort sessions,PasswordService passwords,StoreScopeGuard stores){
        this.store=store;this.policy=policy;this.securityStore=securityStore;this.security=security;this.authentication=authentication;this.sessions=sessions;this.passwords=passwords;this.stores=stores;
    }
    private static BusinessException fail(ErrorCode e){return new BusinessException(e);}
    private static BusinessException field(String name,String message){return new BusinessException(ErrorCode.VALIDATION_FAILED,List.of(new FieldErrorDetail(name,"INVALID",message)));}
    private static long version(String text){try{if(!text.matches("0|[1-9][0-9]{0,18}"))throw new NumberFormatException();return Long.parseLong(text);}catch(RuntimeException e){throw field("version","资源版本格式无效");}}
    private static void expected(String input,String actual){if(version(input)!=version(actual))throw fail(ErrorCode.VERSION_CONFLICT);}
    private static String name(String text,String field){try{return IdentityNames.name(text);}catch(IllegalArgumentException e){throw field(field,e.getMessage());}}
    private static List<UUID> ids(List<UUID> ids){if(ids.stream().anyMatch(Objects::isNull)||new HashSet<>(ids).size()!=ids.size())throw field("ids","关联 ID 不可为空或重复");return List.copyOf(ids);}
    private <T> T transaction(StaffHttpAuthentication.Authenticated current,String permission,Supplier<T> action){
        try(var scope=TenantExecutionScope.forPermission(permission)){
            return securityStore.transaction(current.identity().tenantId(),()->{
                securityStore.lockTenant();
                if(!Set.of("identity:user:detail","identity:role:list","identity:role:detail","platform:store:list").contains(permission))store.serializeTenant();
                var authority=verify(current);
                T result=action.get();
                var after=verify(current);
                if(authority.authorizationVersion()!=after.authorizationVersion()||!authority.grants().equals(after.grants())||!authority.authorizedStoreIds().equals(after.authorizedStoreIds()))throw fail(ErrorCode.PERMISSION_DENIED);
                return result;
            });
        }catch(DuplicateKeyException e){throw new BusinessException(ErrorCode.BUSINESS_STATE_CONFLICT,"账号或角色编码已存在");}
        catch(DataAccessException|jakarta.persistence.PersistenceException|org.springframework.transaction.TransactionException e){throw fail(ErrorCode.DEPENDENCY_UNAVAILABLE);}
    }
    private StaffIdentity verify(StaffHttpAuthentication.Authenticated current){
        var a=authentication.loadForSession(current.identity().tenantId(),current.identity().employeeId()).orElseThrow(()->fail(ErrorCode.SESSION_REVOKED));
        if(a.securityVersion()!=current.session().securityVersion()||a.tenantSecurityVersion()!=current.session().tenantSecurityVersion()
                ||a.authorizationVersion()!=current.identity().authorizationVersion()||!sessions.isActive(a.tenantId(),a.employeeId(),current.session().sessionId()))throw fail(ErrorCode.SESSION_REVOKED);
        if(a.passwordChangeRequired())throw fail(ErrorCode.PASSWORD_CHANGE_REQUIRED);
        return a;
    }
    private EmployeeManagement target(String permission,UUID id,boolean mutation){
        policy.require(permission,id);var e=store.employee(id);var c=TenantScopeGuard.requireBusiness();
        // 关联资料与管理不同于基础读取：STORES必须覆盖全部门店，SELF只允许本人基础编辑/管理读取。
        boolean covered=c.dataScope().types().contains(DataScopeType.TENANT)
            ||c.dataScope().types().contains(DataScopeType.STORES)&&!e.storeIds().isEmpty()&&c.dataScope().storeIds().containsAll(e.storeIds())
            ||Set.of("identity:user:update","identity:user:detail").contains(permission)&&c.dataScope().types().contains(DataScopeType.SELF)&&c.principalId().equals(id);
        if(!covered)throw fail(ErrorCode.RESOURCE_NOT_FOUND);
        if(mutation&&e.protectedAccount()&&!permission.equals("identity:user:update"))throw fail(ErrorCode.PERMISSION_DENIED);
        return e;
    }
    private void coverGrant(PermissionGrant g,Set<UUID> storeIds){
        if(g.scopeType()==null||!PermissionCatalog.supports(g.permissionCode(),g.scopeType()))throw field("grants","权限或数据范围不在正式目录中");
        var context=TenantScopeGuard.requireBusiness();var authority=authentication.loadForSession(context.tenantId(),context.principalId()).orElseThrow(()->fail(ErrorCode.SESSION_REVOKED));
        var own=authority.grants().get(g.permissionCode());
        if(own==null||!own.types().contains(DataScopeType.TENANT)
            &&(g.scopeType()!=DataScopeType.STORES||!own.types().contains(DataScopeType.STORES)||!own.storeIds().containsAll(storeIds)))throw fail(ErrorCode.PERMISSION_DENIED);
    }
    private void coverEmployee(EmployeeManagement e){if(e.id().equals(TenantScopeGuard.requireBusiness().principalId()))return;
        for(UUID r:e.roleIds())for(var g:store.role(r).grants())coverGrant(g,new HashSet<>(e.storeIds()));
    }
    public PageResponse<OrganizationView> organizations(StaffHttpAuthentication.Authenticated c,int page,int size){return transaction(c,"identity:organization:list",()->new PageResponse<>(store.organizations(page,size),page,size,Long.toString(store.organizationCount())));}
    public OrganizationView organization(StaffHttpAuthentication.Authenticated c,UUID id){return transaction(c,"identity:organization:detail",()->store.organization(id));}
    public MutationResult createOrganization(StaffHttpAuthentication.Authenticated c,CreateOrganization input){String code;try{code=IdentityNames.tenantCode(input.code());}catch(IllegalArgumentException e){throw field("code",e.getMessage());}String label=name(input.name(),"name");try{UUID id=transaction(c,"identity:organization:create",()->{UUID created=store.createOrganization(code,label);store.event(created,"CREATE_ORGANIZATION");return created;});return new MutationResult(id,"0",true);}catch(BusinessException e){if(e.error().code()==ErrorCode.BUSINESS_STATE_CONFLICT)throw field("code","组织编码已存在");throw e;}}
    public MutationResult editOrganization(StaffHttpAuthentication.Authenticated c,UUID id,EditOrganization input){return transaction(c,"identity:organization:update",()->{expected(input.version(),store.organization(id).version());store.editOrganization(id,name(input.name(),"name"));store.event(id,"EDIT_ORGANIZATION");return new MutationResult(id,store.organization(id).version(),true);});}
    public MutationResult organizationStatus(StaffHttpAuthentication.Authenticated c,UUID id,ChangeStatus input){return transaction(c,"identity:organization:"+(input.status().equals("ACTIVE")?"enable":"disable"),()->{var org=store.organization(id);expected(input.version(),org.version());if(org.status().equals(input.status()))throw fail(ErrorCode.BUSINESS_STATE_CONFLICT);store.organizationStatus(id,input.status());store.event(id,"ORGANIZATION_STATUS");return new MutationResult(id,store.organization(id).version(),true);});}
    public EmployeeManagement employee(StaffHttpAuthentication.Authenticated current,UUID id){return transaction(current,"identity:user:detail",()->target("identity:user:detail",id,false));}
    public PageResponse<RoleSummary> roles(StaffHttpAuthentication.Authenticated current,int page,int size){return transaction(current,"identity:role:list",()->new PageResponse<>(store.roles(page,size),page,size,Long.toString(store.roleCount())));}
    public RoleDetail role(StaffHttpAuthentication.Authenticated current,UUID id){return transaction(current,"identity:role:detail",()->store.role(id));}
    public PageResponse<StoreOption> stores(StaffHttpAuthentication.Authenticated current,int page,int size){return transaction(current,"platform:store:list",()->new PageResponse<>(store.stores(page,size),page,size,Long.toString(store.storeCount())));}
    public List<PermissionOption> permissions(StaffHttpAuthentication.Authenticated current){return transaction(current,"identity:role:detail",()->PermissionCatalog.DECLARED.values().stream().sorted(Comparator.comparing(PermissionCatalog.Permission::code)).map(p->{
        var own=current.identity().grants().get(p.code());var allowed=new HashSet<DataScopeType>();
        if(own!=null)for(var s:p.scopes())if(own.types().contains(DataScopeType.TENANT)||s==DataScopeType.STORES&&own.types().contains(s))allowed.add(s);
        return new PermissionOption(p.code(),p.chineseName(),p.scopes(),allowed);
    }).toList());}
    public MutationResult create(StaffHttpAuthentication.Authenticated current,CreateEmployee input){
        String login;try{login=IdentityNames.loginName(input.loginName());}catch(IllegalArgumentException e){throw field("loginName",e.getMessage());}
        String display=name(input.displayName(),"displayName");char[] secret=input.initialPassword().toCharArray();String hash;
        try{hash=passwords.hash(secret);}catch(IllegalArgumentException e){throw field("initialPassword",e.getMessage());}finally{Arrays.fill(secret,'\0');}
        UUID id=transaction(current,"identity:user:create",()->{UUID created=store.createEmployee(login,display,hash);store.event(created,"CREATE_EMPLOYEE");return created;});
        return new MutationResult(id,"0",true);
    }
    public MutationResult edit(StaffHttpAuthentication.Authenticated current,UUID id,EditEmployee input){return transaction(current,"identity:user:update",()->{
        store.lockEmployees(List.of(id));var e=target("identity:user:update",id,true);expected(input.version(),e.version());coverEmployee(e);
        store.editEmployee(id,name(input.displayName(),"displayName"));store.event(id,"EDIT_EMPLOYEE");return new MutationResult(id,store.employee(id).version(),true);
    });}
    public MutationResult status(StaffHttpAuthentication.Authenticated current,UUID id,ChangeStatus input){String permission=input.status().equals("ACTIVE")?"identity:user:enable":"identity:user:disable";
        String next=transaction(current,permission,()->{store.lockEmployees(List.of(id));var e=target(permission,id,true);expected(input.version(),e.version());coverEmployee(e);
            if(id.equals(current.identity().employeeId()))throw fail(ErrorCode.PERMISSION_DENIED);
            if(e.status().equals(input.status()))throw fail(ErrorCode.BUSINESS_STATE_CONFLICT);
            store.status(id,input.status());store.authorizationChanged(id);securityStore.revoke(id);store.event(id,input.status().equals("ACTIVE")?"ENABLE_EMPLOYEE":"DISABLE_EMPLOYEE");return store.employee(id).version();});
        return new MutationResult(id,next,security.cleanup(current.identity().tenantId(),id));
    }
    public MutationResult assign(StaffHttpAuthentication.Authenticated current,UUID id,EmployeeAssignments input,boolean roleAssignment){String permission=roleAssignment?"identity:user:roles":"identity:user:stores";
        var selected=ids(input.ids());String next=transaction(current,permission,()->{store.lockEmployees(List.of(id));var e=target(permission,id,true);expected(input.version(),e.version());coverEmployee(e);
            if(id.equals(current.identity().employeeId()))throw fail(ErrorCode.PERMISSION_DENIED);
            if(roleAssignment){for(UUID r:selected){var role=store.role(r);if(!role.role().status().equals("ACTIVE")||role.role().protectedRole())throw fail(ErrorCode.PERMISSION_DENIED);for(var g:role.grants())coverGrant(g,new HashSet<>(e.storeIds()));}}
            else{for(UUID s:selected)stores.requireStore(s);for(UUID r:e.roleIds())for(var g:store.role(r).grants())coverGrant(g,new HashSet<>(selected));}
            store.assignments(id,selected,roleAssignment);store.authorizationChanged(id);securityStore.revoke(id);store.event(id,roleAssignment?"ASSIGN_ROLES":"ASSIGN_STORES");return store.employee(id).version();});
        return new MutationResult(id,next,security.cleanup(current.identity().tenantId(),id));
    }
    public MutationResult createRole(StaffHttpAuthentication.Authenticated current,CreateRole input){String code;try{code=IdentityNames.tenantCode(input.code());}catch(IllegalArgumentException e){throw field("code",e.getMessage());}
        if(code.equals("tenant-admin"))throw fail(ErrorCode.PERMISSION_DENIED);String label=name(input.name(),"name");UUID id=transaction(current,"identity:role:create",()->{UUID created=store.createRole(code,label);store.event(created,"CREATE_ROLE");return created;});return new MutationResult(id,"0",true);
    }
    public MutationResult roleChange(StaffHttpAuthentication.Authenticated current,UUID id,String expected,String label,String state,List<PermissionGrant> grants){
        String permission=grants==null?"identity:role:update":"identity:role:grant";var affected=new ArrayList<UUID>();
        String next=transaction(current,permission,()->{var role=store.role(id);if(role.role().protectedRole())throw fail(ErrorCode.PERMISSION_DENIED);expected(expected,role.role().version());
            affected.addAll(store.roleEmployees(id));if(affected.contains(current.identity().employeeId()))throw fail(ErrorCode.PERMISSION_DENIED);store.lockEmployees(affected);
            for(UUID employee:affected){var e=store.employee(employee);if(e.protectedAccount())throw fail(ErrorCode.PERMISSION_DENIED);coverEmployee(e);}
            // 尚未分配的角色也不能接管自己无权授予的既有权限。
            for(var g:role.grants())coverGrant(g,Set.of());
            if(grants==null)store.editRole(id,name(label,"name"),state);
            else{if(new HashSet<>(grants).size()!=grants.size())throw field("grants","权限范围不可重复");for(var g:grants){coverGrant(g,Set.of());for(UUID e:affected)coverGrant(g,new HashSet<>(store.employee(e).storeIds()));}store.grants(id,grants);}
            for(UUID e:affected){store.authorizationChanged(e);securityStore.revoke(e);}store.event(id,grants==null?"EDIT_ROLE":"ROLE_GRANTS");return store.role(id).role().version();});
        boolean complete=true;for(UUID e:affected)complete=security.cleanup(current.identity().tenantId(),e)&&complete;return new MutationResult(id,next,complete);
    }
}
