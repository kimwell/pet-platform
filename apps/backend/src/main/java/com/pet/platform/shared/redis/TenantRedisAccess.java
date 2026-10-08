package com.pet.platform.shared.redis;

import com.pet.platform.shared.exception.TenantAccessDeniedException;
import com.pet.platform.shared.tenancy.StoreScopeGuard;
import com.pet.platform.shared.tenancy.TenantScopeGuard;
import java.time.Duration;
import java.util.Optional;

/** 地址每次使用都复核当前完整执行范围；业务不得传入裸Key。 */
public final class TenantRedisAccess {
    private final RedisKeyBuilder keys;
    private final RedisValueStore values;
    private final StoreScopeGuard stores;
    TenantRedisAccess(RedisKeyBuilder keys, RedisValueStore values, StoreScopeGuard stores) { this.keys = keys; this.values = values; this.stores = stores; }
    public Optional<String> read(RedisKey key) { verify(key); return values.read(key.encoded); }
    public void write(RedisKey key, String value, Duration ttl) { verify(key); values.write(key.encoded, value, ttl); }
    public boolean delete(RedisKey key) { verify(key); return values.delete(key.encoded); }
    private void verify(RedisKey key) {
        var current = TenantScopeGuard.requireBusiness();
        if (key == null || key.issuer != keys || key.tenantScope == null || key.platformScope != null || !current.equals(key.tenantScope)) {
            throw new TenantAccessDeniedException();
        }
        if (key.storeId != null) stores.requireStore(key.storeId);
    }
}
