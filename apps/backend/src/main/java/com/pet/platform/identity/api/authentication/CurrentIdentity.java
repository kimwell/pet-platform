package com.pet.platform.identity.api.authentication;

import com.pet.platform.identity.application.authentication.*;
import com.pet.platform.shared.tenancy.DataScopeType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.*;

@Schema(requiredProperties={"principalId","principalType","tenantId","displayName","sessionId","permissionCodes","dataScope","authorizedStoreIds","authorizationVersion","expiresAt","idleTimeoutSeconds"})
public record CurrentIdentity(UUID principalId,StaffType principalType,UUID tenantId,String displayName,UUID sessionId,
        List<String> permissionCodes,ScopeData dataScope,List<UUID> authorizedStoreIds,
        @Schema(pattern="^(0|[1-9][0-9]*)$",description="当前数据库授权版本；每请求重载授权，不仅依赖版本") String authorizationVersion,
        Instant expiresAt,@Schema(minimum="1",maximum="86400") int idleTimeoutSeconds) {
    public enum StaffType { STAFF }
    @Schema(requiredProperties={"grants"}) public record ScopeData(List<Grant> grants) { }
    @Schema(requiredProperties={"permissionCode","scopes"}) public record Grant(String permissionCode,List<Scope> scopes) { }
    @Schema(oneOf={TenantScope.class,StoresScope.class,SelfScope.class}) public sealed interface Scope permits TenantScope,StoresScope,SelfScope { }
    @Schema(requiredProperties={"type"}) public record TenantScope(@Schema(allowableValues={"TENANT"}) String type) implements Scope { }
    @Schema(requiredProperties={"type","storeIds"}) public record StoresScope(@Schema(allowableValues={"STORES"}) String type,List<UUID> storeIds) implements Scope { }
    @Schema(requiredProperties={"type"}) public record SelfScope(@Schema(allowableValues={"SELF"}) String type) implements Scope { }
    static CurrentIdentity of(StaffIdentity staff,StaffSessionPort.SessionFact session) {
        var grants=staff.grants().entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> {
            List<Scope> scopes=new ArrayList<>();var d=e.getValue();
            if(d.types().contains(DataScopeType.TENANT))scopes.add(new TenantScope("TENANT"));
            else {if(d.types().contains(DataScopeType.STORES))scopes.add(new StoresScope("STORES",d.storeIds().stream().sorted().toList()));if(d.types().contains(DataScopeType.SELF))scopes.add(new SelfScope("SELF"));}
            return new Grant(e.getKey(),scopes);
        }).toList();
        return new CurrentIdentity(staff.employeeId(),StaffType.STAFF,staff.tenantId(),staff.displayName(),session.sessionId(),staff.grants().keySet().stream().sorted().toList(),new ScopeData(grants),staff.authorizedStoreIds().stream().sorted().toList(),Long.toString(staff.authorizationVersion()),session.expiresAt(),session.idleTimeoutSeconds());
    }
}
