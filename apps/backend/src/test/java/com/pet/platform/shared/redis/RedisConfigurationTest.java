package com.pet.platform.shared.redis;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class RedisConfigurationTest {
    MockEnvironment valid() { return new MockEnvironment().withProperty("pet.redis.prefix","pet")
        .withProperty("spring.data.redis.host","localhost").withProperty("spring.data.redis.port","6379")
        .withProperty("spring.data.redis.database","0").withProperty("spring.data.redis.password","隔离技术夹具")
        .withProperty("spring.data.redis.connect-timeout","2s").withProperty("spring.data.redis.timeout","2s"); }
    @Test void validConfigurationAndMissingRequiredNames() {
        assertDoesNotThrow(() -> RedisConfiguration.validate(valid()));
        for (String name : java.util.List.of("spring.data.redis.host","spring.data.redis.password","spring.data.redis.timeout")) {
            var env = valid().withProperty(name,"");
            var error = assertThrows(IllegalStateException.class, () -> RedisConfiguration.validate(env));
            assertTrue(error.getMessage().contains(name)); assertFalse(error.getMessage().contains("隔离技术夹具"));
        }
    }
    @Test void rejectsAmbiguousPrefixUnboundedTimeoutAndUnsupportedTopologies() {
        assertThrows(IllegalArgumentException.class, () -> RedisConfiguration.validate(valid().withProperty("pet.redis.prefix","pet:*")));
        for (String timeout : java.util.List.of("0ms","99ms","6s")) assertThrows(IllegalStateException.class,
                () -> RedisConfiguration.validate(valid().withProperty("spring.data.redis.timeout",timeout)));
        for (String name : java.util.List.of("spring.data.redis.url","spring.data.redis.cluster.nodes","spring.data.redis.sentinel.master")) {
            assertThrows(IllegalStateException.class, () -> RedisConfiguration.validate(valid().withProperty(name,"禁止的覆盖")));
        }
        assertThrows(IllegalStateException.class, () -> RedisConfiguration.validate(valid().withProperty("spring.data.redis.port","0")));
        assertThrows(IllegalStateException.class, () -> RedisConfiguration.validate(valid().withProperty("spring.data.redis.database","16")));
        assertThrows(IllegalStateException.class, () -> RedisConfiguration.validate(valid().withProperty("spring.data.redis.host","host:6379")));
    }
}
