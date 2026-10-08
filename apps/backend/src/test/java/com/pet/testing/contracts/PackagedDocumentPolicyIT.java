package com.pet.testing.contracts;

import com.pet.testing.PostgresIntegrationSupport;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** 运行生产JAR；测试Controller不在该进程classpath。使用独立Testcontainers数据库。 */
class PackagedDocumentPolicyIT extends PostgresIntegrationSupport {
    @Test void localAndProductionJarHaveActualDocumentAndUiPolicies() throws Exception {
        var records = new ArrayList<String>();
        for (String profile : List.of("local", "prod")) {
            int port;
            try (var socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) { port = socket.getLocalPort(); }
            var logs = Path.of("target/p03-03-" + profile + "-jar.log");
            var builder = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(), "-jar", "target/pet-platform-backend-0.0.0-SNAPSHOT.jar", "--spring.profiles.active=" + profile);
            var environment = builder.environment();
            com.pet.testing.RedisTestSupport.environment(environment);
            environment.put("PET_DATABASE_URL", POSTGRES.getJdbcUrl().split("\\?", 2)[0]);
            environment.put("PET_DATABASE_USERNAME", com.pet.testing.IdentityDatabaseSupport.RUNTIME); environment.put("PET_DATABASE_PASSWORD", POSTGRES.getPassword());
            environment.put("PET_MIGRATION_DATABASE_URL", POSTGRES.getJdbcUrl().split("\\?",2)[0]);
            environment.put("PET_MIGRATION_DATABASE_USERNAME", com.pet.testing.IdentityDatabaseSupport.MIGRATION);
            environment.put("PET_MIGRATION_DATABASE_PASSWORD", POSTGRES.getPassword());
            environment.put("PET_PUBLIC_ORIGIN", "https://example.invalid"); environment.put("SERVER_PORT", Integer.toString(port));
            environment.put("SERVER_ADDRESS", "127.0.0.1");
            var process = builder.redirectErrorStream(true).redirectOutput(logs.toFile()).start();
            try {
                var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(35);
                boolean ready = false;
                while (System.nanoTime() < deadline && process.isAlive()) {
                    try {
                        var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/actuator/health")).timeout(Duration.ofSeconds(1)).build(), HttpResponse.BodyHandlers.ofString());
                        if (response.statusCode() == 200) { ready = true; break; }
                    } catch (java.io.IOException exception) { /* 尚未开始监听。 */ }
                    Thread.sleep(100);
                }
                assertTrue(ready, "生产JAR未就绪，见" + logs);
                for (String path : List.of("/v3/api-docs", "/swagger-ui/index.html", "/v3/api-docs/test-contract", "/__contracts/null", "/__protocol/success")) {
                    var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(3)).build(), HttpResponse.BodyHandlers.ofString());
                    int expected = profile.equals("local") && Set.of("/v3/api-docs", "/swagger-ui/index.html").contains(path) ? 200 : 404;
                    assertEquals(expected, response.statusCode(), profile + " " + path);
                    if (path.equals("/v3/api-docs") && response.statusCode() == 200) {
                        var doc = JsonMapper.builder().build().readTree(response.body());
                        assertEquals(15, doc.path("paths").size()); assertFalse(response.body().contains("ScalarInput"));
                    }
                    records.add(profile + " " + path + " " + response.statusCode());
                }
                assertFalse(Files.readString(logs).contains("TECHNICAL_SECRET_RAW_INPUT"));
            } finally {
                process.destroy(); assertTrue(process.waitFor(15, TimeUnit.SECONDS), "JAR未退出");
                records.add(profile + " SIGTERM exit=" + process.exitValue());
            }
        }
        Files.write(Path.of("target/p03-03-document-runtime.txt"), records);
    }
}
