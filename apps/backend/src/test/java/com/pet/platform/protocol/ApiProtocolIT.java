package com.pet.platform.protocol;

import com.pet.testing.PostgresIntegrationSupport;
import com.pet.platform.shared.observability.TraceContext;
import java.net.URI;
import java.net.http.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

/** 随机端口真实 Tomcat，验证 MockMvc 无法证明的 ERROR 再分发。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ProtocolFixtures.class)
class ApiProtocolIT extends PostgresIntegrationSupport {
    @LocalServerPort int port;
    @Autowired JsonMapper mapper;
    private final HttpClient client = HttpClient.newHttpClient();
    private static final String TRACE = "0123456789abcdef0123456789abcdef";

    @Test
    void realHttpMvcAndContainerErrorsKeepSafeEnvelopeAndTrace() throws Exception {
        for (var scenario : List.of(new Scenario("success", 200), new Scenario("missing", 404),
                new Scenario("unknown", 500), new Scenario("container-error", 500))) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/__protocol/" + scenario.path()))
                    .header(TraceContext.HEADER, TRACE).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(scenario.status(), response.statusCode());
            var body = mapper.readTree(response.body());
            assertEquals(TRACE, body.path("traceId").asText());
            assertEquals(TRACE, response.headers().firstValue(TraceContext.HEADER).orElseThrow());
            assertEquals(scenario.status() < 400, body.path("success").asBoolean());
            assertFalse(response.body().contains("TECHNICAL_SECRET_RAW_INPUT"));
            assertFalse(response.body().contains("timestamp"));
            assertFalse(response.body().contains("java.lang"));
            if (scenario.status() == 500) {
                assertEquals("INTERNAL_ERROR", body.path("error").path("code").asText());
                assertEquals("系统暂时无法处理请求，请稍后重试", body.path("error").path("message").asText());
            }
        }
    }

    @Test
    void realServerRejectsMultipleTraceHeadersAndKeepsFileBytes() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/__protocol/success"))
                .header(TraceContext.HEADER, TRACE).header(TraceContext.HEADER, TRACE).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String trace = response.headers().firstValue(TraceContext.HEADER).orElseThrow();
        assertNotEquals(TRACE, trace);
        assertEquals(trace, mapper.readTree(response.body()).path("traceId").asText());
        var file = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/__protocol/file")).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, file.statusCode());
        assertArrayEquals(new byte[] {0, 1, 2, 3}, file.body());
    }

    record Scenario(String path, int status) { }
}
