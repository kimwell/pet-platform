package com.pet.platform.shared.redis;

import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/** 私有驱动：单Key GET/SET带TTL/DEL；禁Java原生反序列化、事务队列与任意脚本。 */
final class RedisValueStore {
    static final int MAX_VALUE_BYTES = 64 * 1024;
    static final Duration MAX_TTL = Duration.ofHours(24);
    private final RedisTemplate<String, byte[]> template;

    RedisValueStore(RedisConnectionFactory factory) {
        template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setEnableDefaultSerializer(false);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.byteArray());
        template.setEnableTransactionSupport(false);
        template.afterPropertiesSet();
    }
    Optional<String> read(String key) {
        byte[] value;
        try { value = template.opsForValue().get(key); }
        catch (DataAccessException failure) { throw unavailable(); }
        if (value == null) return Optional.empty();
        if (value.length < 3 || value.length > MAX_VALUE_BYTES + 3 || value[0] != 'v' || value[1] != '1' || value[2] != ':') {
            throw unavailable();
        }
        try {
            return Optional.of(StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(value, 3, value.length - 3)).toString());
        } catch (java.nio.charset.CharacterCodingException failure) { throw unavailable(); }
    }
    void write(String key, String value, Duration ttl) {
        if (ttl == null || ttl.compareTo(Duration.ofMillis(1)) < 0 || ttl.compareTo(MAX_TTL) > 0
                || ttl.getNano() % 1_000_000 != 0) {
            throw new IllegalArgumentException("Redis TTL必须是1毫秒～24小时的整毫秒正值");
        }
        byte[] payload = RedisKeyBuilder.utf8(value);
        if (payload.length > MAX_VALUE_BYTES) throw new IllegalArgumentException("Redis值不能超过64KiB UTF-8");
        byte[] bytes = Arrays.copyOf(new byte[]{'v', '1', ':'}, payload.length + 3);
        System.arraycopy(payload, 0, bytes, 3, payload.length);
        try { template.opsForValue().set(key, bytes, ttl); }
        catch (DataAccessException failure) { throw unavailable(); }
    }
    boolean delete(String key) {
        try {
            Boolean result = template.delete(key);
            if (result == null) throw unavailable();
            return result;
        } catch (DataAccessException failure) { throw unavailable(); }
    }
    private static BusinessException unavailable() { return new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE); }
}
