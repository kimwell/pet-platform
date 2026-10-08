package com.pet.testing;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

/** 所有操作只指向独立技术容器；临时认证配置在内存生成，不输出或保存凭据。 */
public final class RedisTestSupport {
    public static final String IMAGE = "redis:8.2.10-bookworm@sha256:47670742d7924adbcdb404d1288b6327815b23141969c0939b8e5d61f78c2634";
    private static final String PASSWORD = UUID.randomUUID().toString();
    public static final GenericContainer<?> REDIS = isolatedContainer();
    public static GenericContainer<?> isolatedContainer() { return new GenericContainer<>(DockerImageName.parse(IMAGE))
            .withExposedPorts(6379)
            .withCopyToContainer(Transferable.of(("bind 0.0.0.0\nprotected-mode yes\nsave \"\"\nappendonly no\nrequirepass " + PASSWORD + "\n").getBytes(StandardCharsets.UTF_8)), "/tmp/technical-redis.conf")
            .withCommand("redis-server", "/tmp/technical-redis.conf"); }
    static {
        REDIS.start();
        Runtime.getRuntime().addShutdownHook(new Thread(REDIS::stop, "p04-redis-cleanup"));
    }
    private RedisTestSupport() { }
    public static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> PASSWORD);
        registry.add("spring.data.redis.timeout", () -> "300ms");
        registry.add("spring.data.redis.connect-timeout", () -> "300ms");
    }
    public static void environment(Map<String, String> env) {
        env.put("PET_REDIS_HOST", REDIS.getHost()); env.put("PET_REDIS_PORT", REDIS.getMappedPort(6379).toString());
        env.put("PET_REDIS_PASSWORD", PASSWORD);
    }
    public static String password() { return PASSWORD; }
}
