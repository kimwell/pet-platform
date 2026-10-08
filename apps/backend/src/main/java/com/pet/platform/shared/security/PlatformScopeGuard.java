package com.pet.platform.shared.security;

import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.exception.PermissionDeniedException;

/** 平台控制面独立入口；不建立租户范围，不提供跨租户业务特权。 */
public final class PlatformScopeGuard {
    private final CurrentPrincipalProvider provider;
    public PlatformScopeGuard(CurrentPrincipalProvider provider) { this.provider = provider; }
    public CurrentPrincipal requirePermission(String permissionCode) {
        var principal = provider.currentPrincipal().orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REQUIRED));
        if (principal.principalType() != PrincipalType.PLATFORM) throw new BusinessException(ErrorCode.AUTH_DOMAIN_MISMATCH);
        if (!principal.permissionCodes().contains(permissionCode)) throw new PermissionDeniedException();
        return principal;
    }
}
