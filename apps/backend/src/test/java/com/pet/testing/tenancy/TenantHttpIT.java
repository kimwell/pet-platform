package com.pet.testing.tenancy;

import com.pet.platform.Application;
import com.pet.platform.shared.observability.TraceContext;
import com.pet.testing.PostgresIntegrationSupport;
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

/** 真Tomcat自动注册Filter及ERROR分发；原PG/JPA自动配置保持启用，独立技术容器。 */
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test") @Import(TenancyFixtures.class)
class TenantHttpIT extends PostgresIntegrationSupport {
    @LocalServerPort int port;
    @Autowired JsonMapper mapper;
    final HttpClient client = HttpClient.newHttpClient();
    @Test void actualRegisteredRequestAndContainerErrorChainsKeepProtocol() throws Exception {
        for (var scenario : List.of(new Case("a/context", 200), new Case("customer/business", 200), new Case("platform/business", 404),
                new Case("anonymous/business", 404), new Case("a/unknown", 500), new Case("provider-fail/context", 503),
                new Case("provider-unknown/context", 500), new Case("a/container-error", 500), new Case("a/leak", 500))) {
            var trace = TenantMvcTest.TRACE;
            var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/__tenancy/" + scenario.path))
                    .header(TraceContext.HEADER, trace).header("X-Tenant-Id", TenancyFixtures.B.toString()).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(scenario.status, response.statusCode(), scenario.path);
            var json = mapper.readTree(response.body()); assertEquals(trace, json.path("traceId").asText());
            assertEquals(trace, response.headers().firstValue(TraceContext.HEADER).orElseThrow());
            assertFalse(response.body().contains("TECHNICAL_SECRET")); assertFalse(response.body().contains("java.lang"));
            if (scenario.status >= 400) { assertFalse(json.path("success").asBoolean()); assertFalse(json.has("data")); }
        }
    }
    record Case(String path, int status) { }
}
