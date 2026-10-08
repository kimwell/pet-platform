package com.pet.platform.protocol;

import com.pet.testing.PostgresIntegrationSupport;
import com.pet.platform.shared.api.*;
import com.pet.platform.shared.observability.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@ActiveProfiles("test")
@Import(ProtocolFixtures.class)
class ApiProtocolTest extends PostgresIntegrationSupport {
    private static final String TRACE = "0123456789abcdef0123456789abcdef";
    @Autowired WebApplicationContext context;
    @Autowired JsonMapper mapper;
    @Autowired FilterRegistrationBean<TraceFilter> traceFilterRegistration;
    MockMvc mvc;

    @BeforeEach
    void prepare() {
        mvc = webAppContextSetup(context).addFilters(traceFilterRegistration.getFilter()).build();
    }

    private JsonNode json(MvcResult result, int status) throws Exception {
        assertEquals(status, result.getResponse().getStatus());
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        for (String forbidden : List.of("TECHNICAL_SECRET_RAW_INPUT", "private_table", "SELECT", "rejectedValue",
                "stackTrace", "java.lang", "com.pet.platform", "tools.jackson", "timestamp")) {
            assertFalse(body.contains(forbidden), () -> "响应泄露内部信息：" + forbidden);
        }
        JsonNode node = mapper.readTree(body);
        assertTrue(node.path("traceId").asText().matches("[0-9a-f]{32}"));
        assertEquals(result.getResponse().getHeader(TraceContext.HEADER), node.path("traceId").asText());
        if (status >= 400) {
            assertFalse(node.path("success").asBoolean());
            assertFalse(node.has("data"));
            assertEquals(Set.of("success", "error", "traceId"), node.propertyNames());
            assertTrue(node.path("error").path("message").asText().matches(".*[\\p{IsHan}].*"));
            assertEquals("no-store", result.getResponse().getHeader("Cache-Control"));
            if (node.path("error").has("fieldErrors")) {
                assertTrue(node.path("error").path("fieldErrors").isArray());
                assertFalse(node.path("error").path("fieldErrors").isEmpty());
            }
        } else {
            assertTrue(node.path("success").asBoolean());
            assertFalse(node.has("error"));
            assertEquals(Set.of("success", "data", "traceId"), node.propertyNames());
        }
        return node;
    }

    private JsonNode error(RequestBuilder request, int status, String code) throws Exception {
        JsonNode node = json(mvc.perform(request).andReturn(), status);
        assertEquals(code, node.path("error").path("code").asText());
        return node.path("error");
    }

    @Test
    void explicitSuccessNullAndResponseEntityKeepExactShape() throws Exception {
        assertEquals("协议检查", json(mvc.perform(get("/__protocol/success")).andReturn(), 200).path("data").path("message").asText());
        assertTrue(json(mvc.perform(get("/__protocol/null")).andReturn(), 200).path("data").isNull());
        var result = mvc.perform(get("/__protocol/created")).andReturn();
        json(result, 201);
        assertEquals("/__protocol/success", result.getResponse().getHeader("Location"));
        json(mvc.perform(post("/__protocol/validate").contentType("application/json")
                .content("{\"name\":\"有效名称\",\"count\":2,\"enabled\":true}")).andReturn(), 200);
    }

    @Test
    void registeredBusinessMappingsAndSafeUnknownErrors() throws Exception {
        for (var code : ErrorCode.values()) {
            error(get("/__protocol/business/" + code.name()), code.status().value(), code.name());
        }
        error(get("/__protocol/tenant-denied"), 404, "RESOURCE_NOT_FOUND");
        error(get("/__protocol/permission-denied"), 403, "PERMISSION_DENIED");
        error(get("/__protocol/conflict"), 409, "VERSION_CONFLICT");
        var unknown = error(get("/__protocol/unknown"), 500, "INTERNAL_ERROR");
        assertEquals("系统暂时无法处理请求，请稍后重试", unknown.path("message").asText());
        assertFalse(unknown.has("fieldErrors"));
        var limited = mvc.perform(get("/__protocol/business/RATE_LIMITED")).andReturn();
        json(limited, 429);
        assertEquals("12", limited.getResponse().getHeader("Retry-After"));
        error(get("/__protocol/framework-status"), 410, "REQUEST_REJECTED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"name\":\"有效名称\",\"count\":\"TECHNICAL_SECRET_RAW_INPUT\"}",
            "{\"name\":\"有效名称\",\"count\":1.5}", "{\"name\":\"有效名称\",\"enabled\":\"true\"}",
            "{\"name\":123}", "{\"name\":true}", "{\"name\":\"有效名称\",\"enabled\":1}",
            "{\"name\":\"有效名称\",\"extra\":\"TECHNICAL_SECRET_RAW_INPUT\"}",
            "{\"name\":\"有效名称\",\"name\":\"重复名称\"}", "{\"name\":\"有效名称\"} {}"})
    void rejectsMalformedUnknownDuplicateAndWrongTypesWithoutEcho(String body) throws Exception {
        error(post("/__protocol/validate").contentType("application/json").content(body), 400, "BAD_REQUEST");
    }

    @Test
    void bodyValidationHasStableNestedPathsAndMultipleCodes() throws Exception {
        var detail = error(post("/__protocol/validate").contentType("application/json")
                .content("{\"name\":\"\",\"profile\":{\"name\":\"\"},\"items\":[{\"name\":\"\"}]}"), 422, "VALIDATION_FAILED");
        List<String> paths = new ArrayList<>();
        detail.path("fieldErrors").forEach(item -> paths.add(item.path("field").asText() + ":" + item.path("code").asText()));
        assertEquals(List.of("items[0].name:REQUIRED", "name:OUT_OF_RANGE", "name:REQUIRED", "profile.name:REQUIRED"), paths);
        assertEquals("请输入名称", detail.path("fieldErrors").get(0).path("message").asText());
        var input = "{\"name\":\"有效名称\",\"items\":[{\"name\":\"\"}]}";
        var method = error(post("/__protocol/method-body").param("limit", "0").contentType("application/json").content(input), 422, "VALIDATION_FAILED");
        assertEquals("items[0].name", method.path("fieldErrors").get(0).path("field").asText());
        assertEquals("limit", method.path("fieldErrors").get(1).path("field").asText());
    }

    @Test
    void parameterSyntaxAndMethodValidationHaveDifferentStatuses() throws Exception {
        error(get("/__protocol/parameter"), 400, "BAD_REQUEST");
        error(get("/__protocol/parameter").param("count", "TECHNICAL_SECRET_RAW_INPUT"), 400, "BAD_REQUEST");
        var validation = error(get("/__protocol/method").param("count", "0"), 422, "VALIDATION_FAILED");
        assertEquals("count", validation.path("fieldErrors").get(0).path("field").asText());
        assertEquals("OUT_OF_RANGE", validation.path("fieldErrors").get(0).path("code").asText());
        error(get("/__protocol/return-validation"), 500, "INTERNAL_ERROR");
        var object = error(post("/__protocol/object").contentType("application/json").content("{\"consistent\":false}"), 422, "VALIDATION_FAILED");
        assertFalse(object.has("fieldErrors"));
    }

    @Test
    void framework404405415AndAllowHeaderArePreserved() throws Exception {
        error(get("/__protocol/missing"), 404, "RESOURCE_NOT_FOUND");
        var method = mvc.perform(post("/__protocol/success")).andReturn();
        json(method, 405);
        assertTrue(method.getResponse().getHeader("Allow").contains("GET"));
        error(post("/__protocol/validate").contentType("text/plain").content("TECHNICAL_SECRET_RAW_INPUT"), 415, "UNSUPPORTED_MEDIA_TYPE");
        error(get("/__protocol/success").accept("application/xml"), 406, "NOT_ACCEPTABLE");
    }

    @Test
    void traceIsGeneratedOrPassedAndContextRestoresOnlyOwnedKeys() throws Exception {
        var result = mvc.perform(get("/__protocol/success").header(TraceContext.HEADER, TRACE)).andReturn();
        assertEquals(TRACE, json(result, 200).path("traceId").asText());
        String generated = json(mvc.perform(get("/__protocol/success")).andReturn(), 200).path("traceId").asText();
        assertNotEquals(TRACE, generated);
        for (Object[] invalid : List.of(new Object[] {"invalid"}, new Object[] {TRACE.toUpperCase()},
                new Object[] {TRACE + "a"}, new Object[] {TRACE + "\r\nInjected: value"},
                new Object[] {TRACE, TRACE}, new Object[] {TRACE + "," + TRACE})) {
            assertNotEquals(TRACE, json(mvc.perform(get("/__protocol/success").header(TraceContext.HEADER, invalid)).andReturn(), 200).path("traceId").asText());
        }
        MDC.put("otherComponent", "retained");
        MDC.put(TraceContext.MDC_KEY, "previous-component-context");
        try {
            error(get("/__protocol/unknown").header(TraceContext.HEADER, TRACE), 500, "INTERNAL_ERROR");
            assertEquals("retained", MDC.get("otherComponent"));
            assertEquals("previous-component-context", MDC.get(TraceContext.MDC_KEY));
            assertThrows(IllegalStateException.class, TraceContext::currentId);
        } finally {
            MDC.remove("otherComponent");
            MDC.remove(TraceContext.MDC_KEY);
        }
        mvc.perform(get("/__protocol/success")).andReturn();
        assertNull(MDC.get(TraceContext.MDC_KEY));
        assertThrows(IllegalStateException.class, TraceContext::currentId);
    }

    @Test
    void paginationDefaultBoundaryEmptyAndTotalPrecision() throws Exception {
        var data = json(mvc.perform(get("/__protocol/page")).andReturn(), 200).path("data");
        assertEquals(Set.of("items", "page", "pageSize", "total"), data.propertyNames());
        assertEquals(1, data.path("page").asInt());
        assertEquals(20, data.path("pageSize").asInt());
        assertEquals("0", data.path("total").asText());
        assertTrue(data.path("total").isString());
        assertTrue(data.path("items").isEmpty());
        var maximum = json(mvc.perform(get("/__protocol/page").param("page", "2147483647").param("pageSize", "100")).andReturn(), 200).path("data");
        assertEquals(2147483647, maximum.path("page").asInt());
        assertEquals(214748364600L, new PageQuery(Integer.MAX_VALUE, 100).offset());
        assertEquals(1, json(mvc.perform(get("/__protocol/page").param("page", "000000000001")).andReturn(), 200).path("data").path("page").asInt());
        assertEquals("9223372036854775807", PageResponse.of(List.of(), new PageQuery(1, 20), Long.MAX_VALUE).total());
        assertThrows(IllegalArgumentException.class, () -> PageResponse.of(List.of(), new PageQuery(1, 20), -1));
        assertThrows(NullPointerException.class, () -> PageResponse.of(null, new PageQuery(1, 20), 0));
        assertThrows(IllegalArgumentException.class, () -> new PageResponse<>(List.of(), 0, 20, "0"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "-1", "1.2", "1e2", "+1", " 1", "1 ", "2147483648", "99999999999999999999999999"})
    void rejectsIllegalPagination(String value) throws Exception {
        error(get("/__protocol/page").param("page", value), 400, "PAGINATION_INVALID");
        error(get("/__protocol/page").param("pageSize", value), 400, "PAGINATION_INVALID");
    }

    @Test
    void rejectsDuplicatePaginationAndPageSizeLimit() throws Exception {
        error(get("/__protocol/page").param("pageSize", "101"), 400, "PAGINATION_INVALID");
        error(get("/__protocol/page").param("page", "1", "2"), 400, "PAGINATION_INVALID");
        error(get("/__protocol/page").param("pageSize", "20", "30"), 400, "PAGINATION_INVALID");
    }

    @Test
    void sortWhitelistDefaultDirectionAndUniqueTieBreakAreStable() throws Exception {
        var defaults = json(mvc.perform(get("/__protocol/sort")).andReturn(), 200).path("data");
        assertEquals("createdAt", defaults.get(0).path("property").asText());
        assertEquals("DESC", defaults.get(0).path("direction").asText());
        assertEquals("id", defaults.get(1).path("property").asText());
        var custom = json(mvc.perform(get("/__protocol/sort").param("sortBy", "name").param("sortOrder", "asc")).andReturn(), 200).path("data");
        assertEquals("displayName", custom.get(0).path("property").asText());
        assertEquals("ASC", custom.get(1).path("direction").asText());
        var unique = json(mvc.perform(get("/__protocol/sort").param("sortBy", "id")).andReturn(), 200).path("data");
        assertEquals(1, unique.size());
        error(get("/__protocol/sort").param("sortOrder", "asc"), 400, "SORT_INVALID");
        error(get("/__protocol/sort").param("sortBy", "id", "name"), 400, "SORT_INVALID");
        error(get("/__protocol/sort").param("sortBy", "id").param("sortOrder", "asc", "desc"), 400, "SORT_INVALID");
        for (String invalid : List.of("", " id", "id ", "id,name", "id:asc", "id;;name", "owner.id", "lower(name)", "displayName")) {
            error(get("/__protocol/sort").param("sortBy", invalid), 400, "SORT_INVALID");
        }
        for (String invalid : List.of("", "ASC", "desc,asc", " asc", "asc ")) {
            error(get("/__protocol/sort").param("sortBy", "id").param("sortOrder", invalid), 400, "SORT_INVALID");
        }
    }

    @Test
    void fileBytesAndDispositionAreNotWrapped() throws Exception {
        var result = mvc.perform(get("/__protocol/file")).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        assertArrayEquals(new byte[] {0, 1, 2, 3}, result.getResponse().getContentAsByteArray());
        assertEquals("application/octet-stream", result.getResponse().getContentType());
        assertTrue(result.getResponse().getHeader("Content-Disposition").contains("protocol.bin"));
        assertTrue(result.getResponse().getHeader(TraceContext.HEADER).matches("[0-9a-f]{32}"));
    }
}
