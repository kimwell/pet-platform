package com.pet.platform.shared.redis;

import com.pet.platform.shared.security.CurrentPrincipal;
import com.pet.platform.shared.tenancy.TenantContext;
import java.util.UUID;

/** 只能由受控入口签发的短期逻辑地址；不是可以跨执行范围转交的授权凭证。 */
public final class RedisKey {
    final RedisKeyBuilder issuer;
    final String encoded;
    final TenantContext tenantScope;
    final UUID storeId;
    final CurrentPrincipal platformScope;

    RedisKey(RedisKeyBuilder issuer, String encoded, TenantContext tenantScope, UUID storeId, CurrentPrincipal platformScope) {
        this.issuer = issuer;
        this.encoded = encoded;
        this.tenantScope = tenantScope;
        this.storeId = storeId;
        this.platformScope = platformScope;
    }
    @Override public String toString() { return "RedisKey[受控原始资源地址]"; }
}
