package com.pet.platform.shared.tenancy;

import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.exception.TenantAccessDeniedException;
import com.pet.platform.shared.security.CurrentPrincipalProvider;
import java.util.function.Supplier;

/** 同步后台调用必须由已登记的可信Provider提供身份；无任意tenantId/runAs入口。 */
public final class TrustedTenantExecutor {
    private final CurrentPrincipalProvider provider;
    public TrustedTenantExecutor(CurrentPrincipalProvider provider) { this.provider = provider; }
    public <T> T execute(String permissionCode, Supplier<T> action) {
        if (TenantContextHolder.current().isPresent()) throw new TenantAccessDeniedException();
        var principal = provider.currentPrincipal().orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REQUIRED));
        var boundary = (provider instanceof com.pet.platform.shared.security.SessionPrincipalProvider ? TenantExecutionScope.openSessionIdentity(principal) : TenantExecutionScope.openIdentity(principal));
        try (var scope = TenantExecutionScope.forPermission(permissionCode)) { return action.get(); }
        finally { boundary.finishBoundary(); }
    }
}
