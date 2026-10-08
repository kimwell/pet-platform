package com.pet.platform.shared.tenancy;

import com.pet.platform.shared.security.PrincipalType;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** 当前执行的最小信息；身份范围上限不是当前门店，traceId可为空（同步后台无请求）。 */
public record TenantContext(UUID tenantId, PrincipalType principalType, UUID principalId, UUID sessionId,
        long authorizationVersion, String permissionCode, DataScope dataScope, Set<UUID> authorizedStoreIds,
        UUID currentStoreId, String traceId, TenantPurpose purpose) {
    public TenantContext {
        Objects.requireNonNull(tenantId); Objects.requireNonNull(principalType); Objects.requireNonNull(principalId);
        Objects.requireNonNull(sessionId); Objects.requireNonNull(purpose);
        authorizedStoreIds = Set.copyOf(authorizedStoreIds);
        if (principalType == PrincipalType.PLATFORM || authorizationVersion < 0) throw new IllegalArgumentException("租户上下文身份不正确");
        if (purpose == TenantPurpose.AUTHORITY_READ) {
            if (permissionCode != null || dataScope != null || currentStoreId != null) throw new IllegalArgumentException("身份边界不得携带业务范围");
        } else if (permissionCode == null || dataScope == null || !tenantId.equals(dataScope.tenantId())
                || principalType != dataScope.principalType() || !principalId.equals(dataScope.principalId())
                || !authorizedStoreIds.containsAll(dataScope.storeIds())) {
            throw new IllegalArgumentException("业务上下文必须有一致的权限范围");
        }
        if (currentStoreId != null && (!authorizedStoreIds.contains(currentStoreId)
                || !(dataScope.types().contains(DataScopeType.TENANT)
                     || dataScope.types().contains(DataScopeType.STORES) && dataScope.storeIds().contains(currentStoreId)))) {
            throw new IllegalArgumentException("当前门店不在有效范围");
        }
    }
    @Override public String toString() { return "TenantContext[受限执行上下文]"; }
}
