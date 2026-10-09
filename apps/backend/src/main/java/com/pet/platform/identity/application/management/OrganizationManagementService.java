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
public final class OrganizationManagementService extends StaffManagementExecutor {
    public OrganizationManagementService(ManagementStore store,EmployeePolicy policy,StaffSecurityStore securityStore,StaffSecurityOperations security,
            StaffAuthentication authentication,StaffSessionPort sessions,PasswordService passwords,StoreScopeGuard stores){ super(store,policy,securityStore,security,authentication,sessions,passwords,stores); }

    public PageResponse<OrganizationView> organizations(StaffHttpAuthentication.Authenticated c,int page,int size){return transaction(c,"identity:organization:list",()->new PageResponse<>(store.organizations(page,size),page,size,Long.toString(store.organizationCount())));}

    public OrganizationView organization(StaffHttpAuthentication.Authenticated c,UUID id){return transaction(c,"identity:organization:detail",()->store.organization(id));}

    public MutationResult createOrganization(StaffHttpAuthentication.Authenticated c,CreateOrganization input){String code;try{code=IdentityNames.tenantCode(input.code());}catch(IllegalArgumentException e){throw field("code",e.getMessage());}String label=name(input.name(),"name");try{UUID id=transaction(c,"identity:organization:create",()->{UUID created=store.createOrganization(code,label);store.event(created,"CREATE_ORGANIZATION");return created;});return new MutationResult(id,"0",true);}catch(BusinessException e){if(e.error().code()==ErrorCode.BUSINESS_STATE_CONFLICT)throw field("code","组织编码已存在");throw e;}}

    public MutationResult editOrganization(StaffHttpAuthentication.Authenticated c,UUID id,EditOrganization input){return transaction(c,"identity:organization:update",()->{expected(input.version(),store.organization(id).version());store.editOrganization(id,name(input.name(),"name"));store.event(id,"EDIT_ORGANIZATION");return new MutationResult(id,store.organization(id).version(),true);});}

    public MutationResult organizationStatus(StaffHttpAuthentication.Authenticated c,UUID id,ChangeStatus input){return transaction(c,"identity:organization:"+(input.status().equals("ACTIVE")?"enable":"disable"),()->{var org=store.organization(id);expected(input.version(),org.version());if(org.status().equals(input.status()))throw fail(ErrorCode.BUSINESS_STATE_CONFLICT);store.organizationStatus(id,input.status());store.event(id,"ORGANIZATION_STATUS");return new MutationResult(id,store.organization(id).version(),true);});}
}
