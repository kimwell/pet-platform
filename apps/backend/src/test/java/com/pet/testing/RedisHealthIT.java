package com.pet.testing;

import com.pet.platform.Application;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;

/** 只停止本测试自己的Redis，验证实际HTTP健康边界，不触及共享或开发服务。 */
@Testcontainers @ActiveProfiles("test")
@SpringBootTest(classes=Application.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class RedisHealthIT {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse(PostgresIntegrationSupport.IMAGE).asCompatibleSubstituteFor("postgres"));
    @Container static final GenericContainer<?> REDIS = RedisTestSupport.isolatedContainer();
    @DynamicPropertySource static void services(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",()->POSTGRES.getJdbcUrl().split("\\?",2)[0]);
        r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);
        r.add("spring.data.redis.host",REDIS::getHost);r.add("spring.data.redis.port",()->REDIS.getMappedPort(6379));
        r.add("spring.data.redis.password",RedisTestSupport::password);
        r.add("spring.data.redis.timeout",()->"300ms");r.add("spring.data.redis.connect-timeout",()->"300ms");
    }
    @LocalServerPort int port;
    final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(3)).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void redisOutageFailsOverallAndReadinessWhileLivenessRemainsHealthy() throws Exception {
        assertEquals(200,get("/actuator/health").statusCode());assertEquals(200,get("/actuator/health/readiness").statusCode());
        REDIS.stop();
        for(String path:java.util.List.of("/actuator/health","/actuator/health/readiness")) {
            var response=get(path);assertEquals(503,response.statusCode());assertTrue(response.body().contains("DOWN"));
            assertFalse(response.body().contains("components"));assertFalse(response.body().contains("details"));assertFalse(response.body().contains(RedisTestSupport.password()));
        }
        var alive=get("/actuator/health/liveness");assertEquals(200,alive.statusCode());assertEquals("{\"status\":\"UP\"}",alive.body());
    }
}
