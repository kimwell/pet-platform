package com.pet.testing.contracts;

import com.pet.testing.PostgresIntegrationSupport;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.pet.platform.Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=false",
        "springdoc.packages-to-scan=com.pet.testing.contracts", "springdoc.paths-to-exclude=", "springdoc.paths-to-match=/__contracts/**"})
@ActiveProfiles("test")
@Import(ContractFixtures.class)
class ContractExportTest extends PostgresIntegrationSupport {
    @LocalServerPort int port;
    @Autowired JsonMapper mapper;
    final HttpClient client = HttpClient.newHttpClient();
    static final String INPUT = """
        {"id":"12345678-1234-4234-8234-123456789abc","amount":"12.3","currency":"CNY",
         "version":"9007199254740993","occurredAt":"2026-10-08T10:20:30.120+08:00",
         "date":"2026-10-08","state":"OPEN","enabled":true,"progress":80,"ratio":1.2345,
         "items":[],"requiredNullable":null}
        """;
    HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).build(), HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> post(String input) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/__contracts/scalars"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(input)).build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void actualHttpScalarsPreservePrecisionAndNullablePresence() throws Exception {
        var response = post(INPUT);
        assertEquals(200, response.statusCode(), response.body());
        var data = mapper.readTree(response.body()).path("data");
        assertEquals("12345678-1234-4234-8234-123456789abc", data.path("id").asText());
        assertEquals("12.30", data.path("amount").asText());
        assertTrue(data.path("amount").isString());
        assertEquals("9007199254740993", data.path("version").asText());
        assertEquals("2026-10-08T02:20:30.120Z", data.path("occurredAt").asText());
        assertEquals("2026-10-08", data.path("date").asText());
        assertEquals("OPEN", data.path("state").asText());
        assertTrue(data.path("enabled").isBoolean());
        assertTrue(data.path("progress").isNumber());
        assertTrue(data.path("ratio").isNumber(), "非金额BigDecimal仍为number");
        assertTrue(data.path("items").isArray()); assertEquals(0, data.path("items").size());
        assertTrue(data.has("requiredNullable")); assertTrue(data.path("requiredNullable").isNull());
        assertFalse(data.has("optional"));
        assertEquals("", mapper.readTree(post(INPUT.replace("\"requiredNullable\":null", "\"requiredNullable\":\"\"")).body()).path("data").path("requiredNullable").asText());
        assertEquals("", mapper.readTree(post(INPUT.replace("\"requiredNullable\":null", "\"requiredNullable\":null,\"optional\":\"\"")).body()).path("data").path("optional").asText());
        assertFalse(mapper.readTree(post(INPUT.replace("\"requiredNullable\":null", "\"requiredNullable\":null,\"optional\":null")).body()).path("data").has("optional"));
        var page = mapper.readTree(get("/__contracts/page").body()).path("data");
        assertEquals("9007199254740993", page.path("total").asText()); assertTrue(page.path("total").isString());
        var empty = mapper.readTree(get("/__contracts/null").body());
        assertTrue(empty.has("data")); assertTrue(empty.path("data").isNull());
    }
    @ParameterizedTest
    @ValueSource(strings = {"0", "12", "99999999999999999.99", "0.00"})
    void moneyNeverUsesFloatingPointAndHasTwoOutputDecimals(String value) throws Exception {
        var response = post(INPUT.replace("12.3", value));
        assertEquals(200, response.statusCode(), response.body());
        assertEquals(new java.math.BigDecimal(value).setScale(2).toPlainString(), mapper.readTree(response.body()).path("data").path("amount").asText());
    }
    @Test void dateTimeDoesNotShiftDateAndRejectsUnspecifiedPrecision() throws Exception {
        for (String input : List.of("2026-10-08T02:20:30Z", "2026-10-08T04:20:30+02:00", "2026-10-07T22:20:30-04:00")) {
            var response = post(INPUT.replace("2026-10-08T10:20:30.120+08:00", input));
            assertEquals(200, response.statusCode());
            assertEquals("2026-10-08T02:20:30.000Z", mapper.readTree(response.body()).path("data").path("occurredAt").asText());
            assertEquals("2026-10-08", mapper.readTree(response.body()).path("data").path("date").asText());
        }
        // 自然日/DST为JDK时区技术验证，不宣称已有业务筛选接口。
        var zone = ZoneId.of("America/New_York");
        assertEquals(23, Duration.between(LocalDate.of(2026, 3, 8).atStartOfDay(zone), LocalDate.of(2026, 3, 9).atStartOfDay(zone)).toHours());
        assertEquals(25, Duration.between(LocalDate.of(2026, 11, 1).atStartOfDay(zone), LocalDate.of(2026, 11, 2).atStartOfDay(zone)).toHours());
    }
    @Test void malformedTypesAndValuesAreSafe400AndSemanticErrors422() throws Exception {
        var inputs = new ArrayList<String>();
        for (String amount : List.of("12.30e0", "+12.30", "-0.00", "01.00", "", "TECHNICAL_SECRET_RAW_INPUT")) inputs.add(INPUT.replace("12.3", amount));
        inputs.add(INPUT.replace("\"12.3\"", "12.3"));
        inputs.add(INPUT.replace("\"OPEN\"", "\"INVALID_TECHNICAL_SECRET_RAW_INPUT\""));
        inputs.add(INPUT.replace("\"OPEN\"", "0"));
        inputs.add(INPUT.replace("true", "1")); inputs.add(INPUT.replace("true", "\"true\""));
        inputs.add(INPUT.replace("80", "2147483648")); inputs.add(INPUT.replace("80", "\"80\""));
        inputs.add(INPUT.replace("9007199254740993", "9223372036854775808"));
        inputs.add(INPUT.replace("\"9007199254740993\"", "9007199254740993"));
        inputs.add(INPUT.replace("2026-10-08T10:20:30.120+08:00", "2026-10-08T10:20:30"));
        inputs.add(INPUT.replace("2026-10-08T10:20:30.120+08:00", "2026-10-08T10:20:30.1201Z"));
        inputs.add(INPUT.replace("\"date\":\"2026-10-08\"", "\"date\":\"2026-02-30\""));
        inputs.add(INPUT.replace("\"date\":\"2026-10-08\"", "\"date\":\"2026-10-08T00:00:00Z\""));
        inputs.add(INPUT.replace(",\"requiredNullable\":null", ""));
        inputs.add(INPUT.replace("12345678-1234-4234-8234-123456789abc", "12345678-1234-4234-8234-123456789ABC"));
        for (String input : inputs) assertSafe(post(input), 400, "BAD_REQUEST");
        for (String amount : List.of("12.300", "100000000000000000.00")) assertSafe(post(INPUT.replace("12.3", amount)), 422, "AMOUNT_INVALID");
        assertSafe(post(INPUT.replace("80", "101")), 422, "VALIDATION_FAILED");
        assertSafe(post(INPUT.replace("\"items\":[]", "\"items\":null")), 422, "VALIDATION_FAILED");
        assertSafe(post(INPUT.replace("\"id\":\"12345678-1234-4234-8234-123456789abc\"", "\"id\":null")), 422, "VALIDATION_FAILED");
    }
    void assertSafe(HttpResponse<String> response, int status, String code) {
        assertEquals(status, response.statusCode(), response.body());
        assertFalse(response.body().contains("TECHNICAL_SECRET_RAW_INPUT"));
        assertFalse(response.body().contains("rejectedValue")); assertFalse(response.body().contains("java."));
        var json = mapper.readTree(response.body()); assertFalse(json.has("data"));
        assertEquals(code, json.path("error").path("code").asText());
    }
    @Test void actualHttpBodiesMatchConcreteOpenApiSchemas() throws Exception {
        var document = mapper.readTree(get("/v3/api-docs/test-contract").body());
        var schemas = document.path("components").path("schemas");
        for (var pair : Map.of("/__contracts/scalars", "SuccessScalarOutput", "/__contracts/page", "SuccessPageResponseScalarOutput", "/__contracts/null", "SuccessVoid", "/__contracts/error", "Failure").entrySet()) {
            var response = pair.getKey().endsWith("scalars") ? post(INPUT) : get(pair.getKey());
            SchemaChecks.matches(document, schemas.path(pair.getValue()), mapper.readTree(response.body()));
        }
        SchemaChecks.matches(document, schemas.path("ScalarInput"), mapper.readTree(INPUT));
        for (String model : List.of("ScalarInput", "ScalarOutput")) {
            assertEquals("string", schemas.path(model).path("properties").path("amount").path("type").asText());
            assertEquals("string", schemas.path(model).path("properties").path("version").path("type").asText());
            assertEquals("date-time", schemas.path(model).path("properties").path("occurredAt").path("format").asText());
            assertEquals("date", schemas.path(model).path("properties").path("date").path("format").asText());
        }
        assertTrue(schemas.path("ScalarOutput").path("properties").path("occurredAt").has("pattern"));
        assertFalse(schemas.path("ScalarOutput").path("required").valueStream().anyMatch(v -> v.asText().equals("optional")));
        assertEquals("application/json", document.path("paths").path("/__contracts/scalars").path("post").path("responses").path("200").path("content").propertyNames().iterator().next());
        assertSafe(post(INPUT.replace("80", "101")), 422, "VALIDATION_FAILED");
        SchemaChecks.matches(document, schemas.path("Failure"), mapper.readTree(post(INPUT.replace("80", "101")).body()));
    }
    @Test void exportsSeparateProductionAndTestDocumentsFromActualHttp() throws Exception {
        var directory = Path.of(System.getProperty("pet.contract.output", "target/openapi"));
        Files.createDirectories(directory);
        for (var entry : Map.of("test-contract.openapi.json", "/v3/api-docs/test-contract").entrySet()) {
            var response = get(entry.getValue()); assertEquals(200, response.statusCode(), response.body());
            var doc = mapper.readTree(response.body()); assertEquals("3.1.0", doc.path("openapi").asText());
            if (entry.getKey().startsWith("backend")) assertEquals(21, doc.path("paths").size(), response.body());
            else {
                assertEquals(4, doc.path("paths").size(), response.body());
                assertTrue(doc.path("paths").has("/__contracts/scalars"));
            }
            var bytes = response.body().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Files.write(directory.resolve(entry.getKey()), bytes);
            Files.writeString(directory.resolve(entry.getKey() + ".sha256"), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)) + "\n");
            var schemas = doc.path("components").path("schemas");
            assertEquals("string", schemas.path("PageResponseFieldErrorDetail").path("properties").path("total").path("type").asText());
            assertFalse(schemas.path("Failure").path("properties").has("data"));
        }
        assertEquals(404, get("/swagger-ui/index.html").statusCode(), "UI与文档启用独立");
        assertEquals(200, get("/actuator/health").statusCode());
        assertFalse(get("/actuator/health").body().contains("data"));
    }
}
