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
        assertEquals(7, entityManagerFactory.getMetamodel().getEntities().size());
        org.junit.jupiter.api.Assertions.assertTrue(entityManagerFactory.getMetamodel().getEntities().stream()
                .noneMatch(entity -> entity.getJavaType().getName().startsWith("com.pet.testing.")));
        try(var c=java.sql.DriverManager.getConnection(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());var statement=c.createStatement();var result=statement.executeQuery("select count(*) from public.identity_employee")) {
            result.next();assertEquals(0,result.getInt(1),"普通启动不自动创建管理员");
        } catch(java.sql.SQLException failure) { throw new IllegalStateException("无法核查独立测试数据库",failure); }
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
