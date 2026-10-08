package com.pet.platform.customeridentity.api;
import com.pet.platform.customeridentity.application.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.*;
@Schema(requiredProperties={"principalId","principalType","tenantId","displayName","sessionId","permissionCodes","dataScope","authorizedStoreIds","authorizationVersion","expiresAt","idleTimeoutSeconds"})
public record CustomerCurrentIdentity(UUID principalId,CustomerType principalType,UUID tenantId,String displayName,UUID sessionId,List<String> permissionCodes,CustomerScopeData dataScope,
 @Schema(maxLength=0) List<UUID> authorizedStoreIds,@Schema(pattern="^(0|[1-9][0-9]*)$") String authorizationVersion,Instant expiresAt,@Schema(minimum="1",maximum="604800") int idleTimeoutSeconds){
 public enum CustomerType {CUSTOMER}
 @Schema(requiredProperties={"grants"}) public record CustomerScopeData(List<CustomerGrant> grants){}
 @Schema(requiredProperties={"permissionCode","scopes"}) public record CustomerGrant(String permissionCode,List<CustomerSelfScope> scopes){}
 @Schema(requiredProperties={"type"}) public record CustomerSelfScope(@Schema(allowableValues={"SELF"}) String type){}
 static CustomerCurrentIdentity of(CustomerIdentity c,CustomerSessionPort.Fact s){return new CustomerCurrentIdentity(c.customerId(),CustomerType.CUSTOMER,c.tenantId(),"微信客户",s.sessionId(),List.of(CustomerHttpAuthentication.SESSION_PERMISSION),new CustomerScopeData(List.of(new CustomerGrant(CustomerHttpAuthentication.SESSION_PERMISSION,List.of(new CustomerSelfScope("SELF"))))),List.of(),"0",s.expiresAt(),s.idleTimeoutSeconds());}
}
