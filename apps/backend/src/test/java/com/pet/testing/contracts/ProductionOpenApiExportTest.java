package com.pet.testing.contracts;

import com.pet.testing.PostgresIntegrationSupport;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** 无任何测试Controller导入的生产应用schema导出。 */
@SpringBootTest(classes = com.pet.platform.Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=false"})
@ActiveProfiles("test")
class ProductionOpenApiExportTest extends PostgresIntegrationSupport {
    @LocalServerPort int port;
    @Autowired JsonMapper mapper;
    @Test void exportsActualProductionModelsWithoutTestPaths() throws Exception {
        var client = HttpClient.newHttpClient();
        var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/v3/api-docs")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        var doc = mapper.readTree(response.body());
        assertEquals("3.1.0", doc.path("openapi").asText());
        assertEquals(15, doc.path("paths").size());
        for(String path:java.util.List.of("/api/admin/auth/csrf","/api/admin/auth/login","/api/admin/auth/token/login","/api/admin/auth/me","/api/admin/auth/logout","/api/admin/auth/password","/api/admin/auth/logout-all","/api/admin/identity/users/{employeeId}/password","/api/admin/identity/users/{employeeId}/revoke-sessions"))assertTrue(doc.path("paths").has(path));
        assertEquals("__Secure-pet_staff_sid",doc.path("components").path("securitySchemes").path("StaffCookie").path("name").asText());
        assertEquals("X-Staff-Token",doc.path("components").path("securitySchemes").path("StaffToken").path("name").asText());
        for(String path:java.util.List.of("csrf","login","me","logout","password","logout-all"))assertTrue(doc.path("paths").has("/api/platform/auth/"+path));
        assertEquals("__Secure-pet_platform_sid",doc.path("components").path("securitySchemes").path("PlatformCookie").path("name").asText());
        assertFalse(doc.path("paths").toString().contains("__platform-test"));
        var platform=doc.path("components").path("schemas").path("PlatformCurrentIdentity");
        assertEquals("null",platform.path("properties").path("tenantId").path("type").asText());
        assertEquals("null",platform.path("properties").path("dataScope").path("type").asText());
        assertEquals("PLATFORM",platform.path("properties").path("principalType").path("enum").get(0).asText());
        assertFalse(doc.path("paths").toString().contains("__authentication-test"));
        var cleanup=doc.path("paths").path("/api/admin/auth/logout-all").path("post").path("responses").path("200").path("headers").path("X-Session-Cleanup").path("schema");
        assertEquals("string",cleanup.path("type").asText());assertEquals("COMPLETE",cleanup.path("enum").get(0).asText());assertEquals("PENDING",cleanup.path("enum").get(1).asText());
        var schemas = doc.path("components").path("schemas");
        assertTrue(schemas.size() >= 7);
        for(String input:java.util.List.of("ChangePasswordInput","ConfirmationInput","ResetPasswordInput","RevokeSessionsInput"))assertTrue(schemas.path(input).path("properties").path("currentPassword").path("writeOnly").asBoolean(),input);
        assertTrue(schemas.path("CurrentIdentity").path("required").toString().contains("passwordChangeRequired"));
        for(String schema:java.util.List.of("CurrentIdentity","SuccessCurrentIdentity","TokenLoginResult","SuccessTokenLoginResult","CsrfResult","SuccessCsrfResult"))assertTrue(schemas.has(schema),schema);
        for(var path:doc.path("paths"))for(var operation:path)assertTrue(operation.path("responses").has("200"));
        assertFalse(schemas.path("CurrentIdentity").path("properties").has("token"));
        assertTrue(schemas.path("CurrentIdentity").path("properties").has("dataScope"));
        assertFalse(response.body().contains("ScalarInput"));
        assertFalse(schemas.path("Failure").path("properties").has("data"));
        assertEquals("string", schemas.path("PageResponseFieldErrorDetail").path("properties").path("total").path("type").asText());
        assertFalse(schemas.path("SuccessFieldErrorDetail").path("additionalProperties").asBoolean(true));
        assertTrue(schemas.path("SuccessFieldErrorDetail").path("properties").path("success").path("const").asBoolean());
        assertEquals(2, schemas.path("SuccessFieldErrorDetail").path("properties").path("data").path("anyOf").size());
        var output = Path.of(System.getProperty("pet.contract.output", "target/openapi"));
        Files.createDirectories(output);
        var bytes = response.body().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(output.resolve("backend.openapi.json"), bytes);
        Files.writeString(output.resolve("backend.openapi.json.sha256"), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)) + "\n");
        for (String path : java.util.List.of("/__contracts/scalars", "/__protocol/success", "/v3/api-docs/test-contract")) {
            assertEquals(404, client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        }
    }
}
