package com.pet.platform.identity.application.authentication;

import com.pet.platform.shared.tenancy.DataScope;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 权威员工事实，不是会话或CurrentPrincipal；后续认证适配器必须另验真实会话。 */
@JsonIgnoreType
public record StaffIdentity(UUID tenantId,UUID employeeId,String displayName,long securityVersion,long authorizationVersion,
        long tenantSecurityVersion,Set<UUID> authorizedStoreIds,Map<String,DataScope> grants,boolean passwordChangeRequired) {
    public StaffIdentity(UUID tenantId,UUID employeeId,String displayName,long securityVersion,long authorizationVersion,long tenantSecurityVersion,Set<UUID> stores,Map<String,DataScope> grants) {this(tenantId,employeeId,displayName,securityVersion,authorizationVersion,tenantSecurityVersion,stores,grants,false); }
    public StaffIdentity { authorizedStoreIds=Set.copyOf(authorizedStoreIds);grants=Map.copyOf(grants); }
    @Override public String toString() { return "StaffIdentity[受限员工事实]"; }
}
