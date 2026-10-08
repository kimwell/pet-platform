package com.pet.platform.shared.tenancy;

import com.pet.platform.shared.exception.TenantAccessDeniedException;
import com.pet.platform.shared.exception.PermissionDeniedException;
import java.util.UUID;

public final class TenantScopeGuard {
    private TenantScopeGuard() { }
    public static TenantContext requireBusiness() {
        var context = TenantContextHolder.required();
        if (context.purpose() != TenantPurpose.BUSINESS) throw new TenantAccessDeniedException();
        return context;
    }
    public static TenantContext requirePermission(String permissionCode) {
        var context = requireBusiness();
        if (!context.permissionCode().equals(permissionCode)) throw new PermissionDeniedException();
        return context;
    }
    public static TenantContext requireTenant(UUID expectedTenantId) {
        var context = requireBusiness();
        if (!context.tenantId().equals(expectedTenantId)) throw new TenantAccessDeniedException();
        return context;
    }
}
