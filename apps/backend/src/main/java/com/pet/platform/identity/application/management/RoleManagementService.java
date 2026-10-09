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
public final class RoleManagementService extends StaffManagementExecutor {
    public RoleManagementService(ManagementStore store,EmployeePolicy policy,StaffSecurityStore securityStore,StaffSecurityOperations security,
            StaffAuthentication authentication,StaffSessionPort sessions,PasswordService passwords,StoreScopeGuard stores){ super(store,policy,securityStore,security,authentication,sessions,passwords,stores); }

    public PageResponse<RoleSummary> roles(StaffHttpAuthentication.Authenticated current,int page,int size){return transaction(current,"identity:role:list",()->new PageResponse<>(store.roles(page,size),page,size,Long.toString(store.roleCount())));}

    public RoleDetail role(StaffHttpAuthentication.Authenticated current,UUID id){return transaction(current,"identity:role:detail",()->store.role(id));}

    public List<PermissionOption> permissions(StaffHttpAuthentication.Authenticated current){return transaction(current,"identity:role:detail",()->PermissionCatalog.DECLARED.values().stream().sorted(Comparator.comparing(PermissionCatalog.Permission::code)).map(p->{
        var own=current.identity().grants().get(p.code());var allowed=new HashSet<DataScopeType>();
        if(own!=null)for(var s:p.scopes())if(own.types().contains(DataScopeType.TENANT)||s==DataScopeType.STORES&&own.types().contains(s))allowed.add(s);
        return new PermissionOption(p.code(),p.chineseName(),p.scopes(),allowed);
    }).toList());}

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
