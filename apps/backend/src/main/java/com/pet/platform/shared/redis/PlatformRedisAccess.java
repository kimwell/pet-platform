package com.pet.platform.shared.redis;

import com.pet.platform.shared.exception.PermissionDeniedException;
import com.pet.platform.shared.security.PlatformScopeGuard;
import com.pet.platform.shared.tenancy.TenantContextHolder;
import java.time.Duration;
import java.util.Optional;

/** 独立控制面能力；不接受租户范围，也不将空tenant降级为平台。 */
public final class PlatformRedisAccess {
    public static final String PERMISSION = "platform:redis:operate";
    private final RedisKeyBuilder keys;
    private final RedisValueStore values;
    private final PlatformScopeGuard platform;
    PlatformRedisAccess(RedisKeyBuilder keys, RedisValueStore values, PlatformScopeGuard platform) {
        this.keys = keys; this.values = values; this.platform = platform;
    }
    public RedisKey resource(String module, String businessKey) {
        requireNoTenant();
        var p = platform.requirePermission(PERMISSION);
        return new RedisKey(keys, keys.address("platform:raw:", module, businessKey), null, null, p);
    }
    public Optional<String> read(RedisKey key) { verify(key); return values.read(key.encoded); }
    public void write(RedisKey key, String value, Duration ttl) { verify(key); values.write(key.encoded, value, ttl); }
    public boolean delete(RedisKey key) { verify(key); return values.delete(key.encoded); }
    private void verify(RedisKey key) {
        requireNoTenant();
        var p = platform.requirePermission(PERMISSION);
        if (key == null || key.issuer != keys || key.tenantScope != null || key.platformScope == null || !p.equals(key.platformScope)) {
            throw new PermissionDeniedException();
        }
    }
    private static void requireNoTenant() {
        if (TenantContextHolder.current().isPresent()) throw new PermissionDeniedException();
    }
}
