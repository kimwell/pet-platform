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
abstract class StaffManagementExecutor {
    protected final ManagementStore store;
    protected final EmployeePolicy policy;
    protected final StaffSecurityStore securityStore;
    protected final StaffSecurityOperations security;
    protected final StaffAuthentication authentication;
    protected final StaffSessionPort sessions;
    protected final PasswordService passwords;
    protected final StoreScopeGuard stores;
    protected StaffManagementExecutor(ManagementStore store,EmployeePolicy policy,StaffSecurityStore securityStore,StaffSecurityOperations security,
            StaffAuthentication authentication,StaffSessionPort sessions,PasswordService passwords,StoreScopeGuard stores){
        this.store=store;this.policy=policy;this.securityStore=securityStore;this.security=security;this.authentication=authentication;this.sessions=sessions;this.passwords=passwords;this.stores=stores;
    }
    protected static BusinessException fail(ErrorCode e){return new BusinessException(e);}
    protected static BusinessException field(String name,String message){return new BusinessException(ErrorCode.VALIDATION_FAILED,List.of(new FieldErrorDetail(name,"INVALID",message)));}
    protected static long version(String text){try{if(!text.matches("0|[1-9][0-9]{0,18}"))throw new NumberFormatException();return Long.parseLong(text);}catch(RuntimeException e){throw field("version","资源版本格式无效");}}
    protected static void expected(String input,String actual){if(version(input)!=version(actual))throw fail(ErrorCode.VERSION_CONFLICT);}
    protected static String name(String text,String field){try{return IdentityNames.name(text);}catch(IllegalArgumentException e){throw field(field,e.getMessage());}}
    protected static List<UUID> ids(List<UUID> ids){if(ids.stream().anyMatch(Objects::isNull)||new HashSet<>(ids).size()!=ids.size())throw field("ids","关联 ID 不可为空或重复");return List.copyOf(ids);}
    protected final <T> T transaction(StaffHttpAuthentication.Authenticated current,String permission,Supplier<T> action){
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
    protected final StaffIdentity verify(StaffHttpAuthentication.Authenticated current){
        var a=authentication.loadForSession(current.identity().tenantId(),current.identity().employeeId()).orElseThrow(()->fail(ErrorCode.SESSION_REVOKED));
        if(a.securityVersion()!=current.session().securityVersion()||a.tenantSecurityVersion()!=current.session().tenantSecurityVersion()
                ||a.authorizationVersion()!=current.identity().authorizationVersion()||!sessions.isActive(a.tenantId(),a.employeeId(),current.session().sessionId()))throw fail(ErrorCode.SESSION_REVOKED);
        if(a.passwordChangeRequired())throw fail(ErrorCode.PASSWORD_CHANGE_REQUIRED);
        return a;
    }
    protected final EmployeeManagement target(String permission,UUID id,boolean mutation){
        policy.require(permission,id);var e=store.employee(id);var c=TenantScopeGuard.requireBusiness();
        // 关联资料与管理不同于基础读取：STORES必须覆盖全部门店，SELF只允许本人基础编辑/管理读取。
        boolean covered=c.dataScope().types().contains(DataScopeType.TENANT)
            ||c.dataScope().types().contains(DataScopeType.STORES)&&!e.storeIds().isEmpty()&&c.dataScope().storeIds().containsAll(e.storeIds())
            ||Set.of("identity:user:update","identity:user:detail").contains(permission)&&c.dataScope().types().contains(DataScopeType.SELF)&&c.principalId().equals(id);
        if(!covered)throw fail(ErrorCode.RESOURCE_NOT_FOUND);
        if(mutation&&e.protectedAccount()&&!permission.equals("identity:user:update"))throw fail(ErrorCode.PERMISSION_DENIED);
        return e;
    }
    protected final void coverGrant(PermissionGrant g,Set<UUID> storeIds){
        if(g.scopeType()==null||!PermissionCatalog.supports(g.permissionCode(),g.scopeType()))throw field("grants","权限或数据范围不在正式目录中");
        var context=TenantScopeGuard.requireBusiness();var authority=authentication.loadForSession(context.tenantId(),context.principalId()).orElseThrow(()->fail(ErrorCode.SESSION_REVOKED));
        var own=authority.grants().get(g.permissionCode());
        if(own==null||!own.types().contains(DataScopeType.TENANT)
            &&(g.scopeType()!=DataScopeType.STORES||!own.types().contains(DataScopeType.STORES)||!own.storeIds().containsAll(storeIds)))throw fail(ErrorCode.PERMISSION_DENIED);
    }
    protected final void coverEmployee(EmployeeManagement e){if(e.id().equals(TenantScopeGuard.requireBusiness().principalId()))return;
        for(UUID r:e.roleIds())for(var g:store.role(r).grants())coverGrant(g,new HashSet<>(e.storeIds()));
    }

}
