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

/** 本模块独立用例，共享受限身份与事务执行机制。 */
@Service
public final class EmployeeManagementService extends StaffManagementExecutor {
    public EmployeeManagementService(ManagementStore store,EmployeePolicy policy,StaffSecurityStore securityStore,StaffSecurityOperations security,
            StaffAuthentication authentication,StaffSessionPort sessions,PasswordService passwords,StoreScopeGuard stores){ super(store,policy,securityStore,security,authentication,sessions,passwords,stores); }

    public EmployeeManagement employee(StaffHttpAuthentication.Authenticated current,UUID id){return transaction(current,"identity:user:detail",()->target("identity:user:detail",id,false));}

    public PageResponse<StoreOption> stores(StaffHttpAuthentication.Authenticated current,int page,int size){return transaction(current,"platform:store:list",()->new PageResponse<>(store.stores(page,size),page,size,Long.toString(store.storeCount())));}

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
}
