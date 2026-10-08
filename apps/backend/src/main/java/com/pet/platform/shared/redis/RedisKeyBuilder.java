package com.pet.platform.shared.redis;

import com.pet.platform.shared.tenancy.StoreScopeGuard;
import com.pet.platform.shared.tenancy.TenantScopeGuard;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/** 仅原始资源；命中不代表数据授权。权限过滤结果当前禁止缓存。 */
public final class RedisKeyBuilder {
    private final String root;
    private final StoreScopeGuard stores;

    public RedisKeyBuilder(String prefix, String environment, StoreScopeGuard stores) {
        validateCode(prefix, "pet.redis.prefix");
        if (!java.util.Set.of("local", "test", "prod").contains(environment)) {
            throw new IllegalArgumentException("Redis环境必须是local/test/prod");
        }
        this.root = prefix + ":" + environment + ":v1:";
        this.stores = java.util.Objects.requireNonNull(stores);
    }

    public RedisKey tenantResource(String module, String businessKey) {
        var c = TenantScopeGuard.requireBusiness();
        return new RedisKey(this, address("tenant:" + c.tenantId() + ":raw:", module, businessKey), c, null, null);
    }

    public RedisKey storeResource(UUID storeId, String module, String businessKey) {
        stores.requireStore(storeId);
        var c = TenantScopeGuard.requireBusiness();
        if (c.currentStoreId() != null && !c.currentStoreId().equals(storeId)) {
            throw new com.pet.platform.shared.exception.TenantAccessDeniedException();
        }
        return new RedisKey(this, address("tenant:" + c.tenantId() + ":store:" + storeId + ":raw:", module, businessKey), c, storeId, null);
    }

    String address(String namespace, String module, String businessKey) {
        validateCode(module, "Redis模块标识");
        byte[] bytes = utf8(businessKey);
        if (bytes.length == 0 || bytes.length > 128 || businessKey.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Redis业务标识必须是1～128字节且不含控制字符的非敏感标识");
        }
        String result = root + namespace + module + ":b" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        if (result.length() > 512) throw new IllegalArgumentException("Redis地址不能超过512字节");
        return result;
    }

    static void validateCode(String value, String name) {
        if (value == null || !value.matches("[a-z][a-z0-9-]{0,31}")) {
            throw new IllegalArgumentException(name + "必须是1～32位小写字母、数字或连字符且以字母开头");
        }
    }

    static byte[] utf8(String value) {
        if (value == null) throw new IllegalArgumentException("Redis文本不能为null");
        try {
            ByteBuffer bytes = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).encode(java.nio.CharBuffer.wrap(value));
            byte[] result = new byte[bytes.remaining()]; bytes.get(result); return result;
        } catch (CharacterCodingException failure) {
            throw new IllegalArgumentException("Redis文本必须是合法Unicode");
        }
    }
}
