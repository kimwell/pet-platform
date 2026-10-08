package com.pet.platform.shared.tenancy;

import com.pet.platform.shared.exception.TenantAccessDeniedException;
import java.util.UUID;

/** 门店事实与当前权限范围分别核对；范围外/不存在/跨租户统一404。SELF不能单独授权门店。 */
public final class StoreScopeGuard {
    private final StoreOwnershipReader reader;
    public StoreScopeGuard(StoreOwnershipReader reader) { this.reader = reader; }
    public void requireStore(UUID storeId) {
        var context = TenantScopeGuard.requireBusiness();
        if (storeId == null || !reader.findTenantId(storeId).filter(context.tenantId()::equals).isPresent()) throw new TenantAccessDeniedException();
        var scope = context.dataScope();
        if (!context.authorizedStoreIds().contains(storeId) || !(scope.types().contains(DataScopeType.TENANT)
                || scope.types().contains(DataScopeType.STORES) && scope.storeIds().contains(storeId))) throw new TenantAccessDeniedException();
    }
    public TenantExecutionScope openStore(UUID storeId) {
        requireStore(storeId);
        return TenantExecutionScope.withVerifiedStore(storeId);
    }
}
