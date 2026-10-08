package com.pet.platform;

import com.pet.testing.PostgresIntegrationSupport;
import com.pet.platform.shared.config.EnvironmentSettings;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApplicationTest extends PostgresIntegrationSupport {
    @LocalServerPort
    private int port;

    @Autowired
    private EnvironmentSettings settings;

    @Autowired
    private jakarta.persistence.EntityManagerFactory entityManagerFactory;

    @Test
    void defaultApplicationScanDoesNotLoadTestEntities() {
        assertEquals(0, entityManagerFactory.getMetamodel().getEntities().size());
    }

    @Test
    void startsRealHttpServerWithoutBusinessEndpoints() throws Exception {
        for (String path : List.of("/", "/__protocol/success")) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build();
            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
            assertEquals(404, response.statusCode(), "未导入夹具时，工程壳不应注册业务或协议测试端点");
        }
        assertEquals("test", settings.environment());
    }

    @Test
    void rejectsMissingEnvironmentAndInsecureProductionOrigin() {
        assertThrows(IllegalArgumentException.class, () -> new EnvironmentSettings(null, URI.create("http://localhost")));
        assertThrows(IllegalArgumentException.class, () -> new EnvironmentSettings("prod", URI.create("http://localhost")));
        assertThrows(IllegalArgumentException.class, () -> new EnvironmentSettings("local", URI.create("https://example.com/api")));
        assertThrows(IllegalArgumentException.class, () -> new EnvironmentSettings("local", URI.create("http://localhost:0")));
        assertThrows(IllegalArgumentException.class, () -> new EnvironmentSettings("local", URI.create("http://localhost:65536")));
        var local = new EnvironmentSettings("local", URI.create("http://localhost"));
        local.verifyProfiles("local");
        assertThrows(IllegalArgumentException.class, () -> local.verifyProfiles("prod"));
        assertThrows(IllegalArgumentException.class, () -> local.verifyProfiles("prod", "local"));
    }
}
