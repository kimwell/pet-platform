package com.pet.platform.shared.redis;

import com.pet.platform.shared.security.PlatformScopeGuard;
import com.pet.platform.shared.tenancy.StoreScopeGuard;
import java.time.Duration;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration(proxyBeanMethods = false)
public class RedisConfiguration {
    @Bean static BeanFactoryPostProcessor redisConfigurationValidation(Environment env) {
        return factory -> validate(env);
    }
    static void validate(Environment env) {
        RedisKeyBuilder.validateCode(env.getProperty("pet.redis.prefix"), "pet.redis.prefix");
        required(env, "spring.data.redis.host");
        required(env, "spring.data.redis.password");
        String host = env.getProperty("spring.data.redis.host");
        if (!host.matches("[A-Za-z0-9.-]{1,253}")) throw new IllegalStateException("spring.data.redis.host必须是主机名或IPv4地址");
        int port = env.getProperty("spring.data.redis.port", Integer.class, 0);
        int database = env.getProperty("spring.data.redis.database", Integer.class, -1);
        if (port < 1 || port > 65535 || database < 0 || database > 15) throw new IllegalStateException("Redis端口或逻辑库配置不正确");
        for (String name : java.util.List.of("spring.data.redis.connect-timeout", "spring.data.redis.timeout")) {
            Duration timeout = org.springframework.boot.convert.DurationStyle.detectAndParse(required(env, name));
            if (timeout.compareTo(Duration.ofMillis(100)) < 0 || timeout.compareTo(Duration.ofSeconds(5)) > 0) {
                throw new IllegalStateException(name + "必须在100毫秒～5秒之间");
            }
        }
        for (String unsupported : java.util.List.of("spring.data.redis.url", "spring.data.redis.cluster.nodes", "spring.data.redis.sentinel.master")) {
            if (env.containsProperty(unsupported)) throw new IllegalStateException(unsupported + "不在当前单节点Redis接入范围内");
        }
    }
    private static String required(Environment env, String name) {
        String value = env.getProperty(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("缺少必要Redis配置：" + name);
        return value;
    }
    @Bean RedisKeyBuilder redisKeyBuilder(Environment env, StoreScopeGuard stores) {
        return new RedisKeyBuilder(env.getRequiredProperty("pet.redis.prefix"), env.getRequiredProperty("pet.environment"), stores);
    }
    @Bean RedisValueStore redisValueStore(RedisConnectionFactory factory) { return new RedisValueStore(factory); }
    @Bean TenantRedisAccess tenantRedisAccess(RedisKeyBuilder keys, RedisValueStore values, StoreScopeGuard stores) { return new TenantRedisAccess(keys, values, stores); }
    @Bean PlatformRedisAccess platformRedisAccess(RedisKeyBuilder keys, RedisValueStore values, PlatformScopeGuard platform) {
        return new PlatformRedisAccess(keys, values, platform);
    }
}
