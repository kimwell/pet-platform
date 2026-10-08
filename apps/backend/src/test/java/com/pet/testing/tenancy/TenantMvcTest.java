package com.pet.testing.tenancy;

import com.pet.platform.shared.api.ProtocolErrorController;
import com.pet.platform.shared.config.ApiConfiguration;
import com.pet.platform.shared.exception.GlobalExceptionHandler;
import com.pet.platform.shared.observability.*;
import com.pet.platform.shared.tenancy.*;
import jakarta.servlet.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.slf4j.MDC;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.HandlerExceptionResolver;
import tools.jackson.databind.json.JsonMapper;
import static com.pet.testing.tenancy.TenancyFixtures.*;
import static com.pet.testing.tenancy.TenantExecutionTest.assertClean;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** 真实MVC映射/校验/异常解析链；独立Web上下文不需要数据库。 */
class TenantMvcTest {
    static final String TRACE = "0123456789abcdef0123456789abcdef";
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;
    TenancyFixtures.TestPrincipalProvider provider;
    final JsonMapper mapper = JsonMapper.builder().build();
    @Configuration(proxyBeanMethods = false) @EnableWebMvc
    @Import({ApiConfiguration.class, TenancyConfiguration.class, GlobalExceptionHandler.class, ProtocolErrorController.class, TenancyFixtures.class})
    static class WebSlice { }
    @BeforeEach void prepare() {
        assertClean();
        context = new AnnotationConfigWebApplicationContext(); context.setServletContext(new MockServletContext());
        context.register(WebSlice.class); context.refresh();
        provider = context.getBean(TenancyFixtures.TestPrincipalProvider.class);
        mvc = webAppContextSetup(context).addFilters(new TraceFilter(), new TenancyFixtures.FixtureAuthenticationFilter(provider),
            new TenantContextFilter(provider, context.getBean("handlerExceptionResolver", HandlerExceptionResolver.class))).build();
    }
    @AfterEach void verifyCleanup() {
        // 先验证待测请求链已清理，不用set/clear掩盖遗漏。
        try { assertClean(); assertNull(MDC.get("traceId")); assertThrows(IllegalStateException.class, TraceContext::currentId); }
        finally { context.close(); }
    }
    tools.jackson.databind.JsonNode perform(RequestBuilder request, int status) throws Exception {
        var result = mvc.perform(request).andReturn(); assertEquals(status, result.getResponse().getStatus());
        var body = result.getResponse().getContentAsString(StandardCharsets.UTF_8); var node = mapper.readTree(body);
        assertEquals(result.getResponse().getHeader(TraceContext.HEADER), node.path("traceId").asText());
        assertTrue(node.path("traceId").asText().matches("[0-9a-f]{32}")); assertFalse(body.contains("TECHNICAL_SECRET"));
        if (status >= 400) { assertFalse(node.path("success").asBoolean()); assertFalse(node.has("data")); assertEquals("no-store", result.getResponse().getHeader("Cache-Control")); }
        assertClean(); return node;
    }
    @Test void anonymousPublicRequestHasNoTenantAndProtectedOperationDenies() throws Exception {
        assertEquals("none", perform(get("/__tenancy/anonymous/context"), 200).path("data").path("tenant").asText());
        assertEquals("RESOURCE_NOT_FOUND", perform(get("/__tenancy/anonymous/business"), 404).path("error").path("code").asText());
    }
    @Test void trustedStaffCustomerAndPlatformHaveExplicitDifferentBoundaries() throws Exception {
        assertEquals(A.toString(), perform(get("/__tenancy/a/context"), 200).path("data").path("tenant").asText());
        assertEquals("CUSTOMER", perform(get("/__tenancy/customer/context"), 200).path("data").path("type").asText());
        assertEquals("none", perform(get("/__tenancy/platform/context"), 200).path("data").path("tenant").asText());
        perform(get("/__tenancy/a/root-business"), 404);
        perform(get("/__tenancy/platform/business"), 404); perform(get("/__tenancy/a/business"), 200); perform(get("/__tenancy/customer/business"), 200);
    }
    @Test void headerTenantAndClientAuthorityCannotCreateOrOverrideIdentity() throws Exception {
        assertEquals(A.toString(), perform(get("/__tenancy/a/context").header("tenantId", B).header("X-Tenant-Id", B)
            .header("X-Role", "admin").header("X-Store-Ids", OTHER_STORE), 200).path("data").path("tenant").asText());
        assertEquals("none", perform(get("/__tenancy/anonymous/context").header("X-Tenant-Id", A).header("X-Role", "admin"), 200).path("data").path("tenant").asText());
    }
    @Test void queryTenantCannotCreateOrOverrideIdentity() throws Exception {
        assertEquals(A.toString(), perform(get("/__tenancy/a/context").param("tenantId", B.toString()), 200).path("data").path("tenant").asText());
        assertEquals("none", perform(get("/__tenancy/anonymous/context").param("tenantId", A.toString()), 200).path("data").path("tenant").asText());
    }
    @Test void bodyTenantIsOnlyTestIntentAndCannotCreateOrOverrideIdentity() throws Exception {
        var body = "{\"name\":\"技术输入\",\"tenantId\":\"" + B + "\"}";
        assertEquals(A.toString(), perform(post("/__tenancy/a/context").contentType("application/json").content(body), 200).path("data").path("tenant").asText());
        assertEquals("none", perform(post("/__tenancy/anonymous/context").contentType("application/json").content(body), 200).path("data").path("tenant").asText());
    }
    @Test void controllerExceptionAndValidationErrorCleanWithTrace() throws Exception {
        assertEquals(TRACE, perform(get("/__tenancy/a/unknown").header(TraceContext.HEADER, TRACE), 500).path("traceId").asText());
        assertEquals("VALIDATION_FAILED", perform(post("/__tenancy/a/context").contentType("application/json").content("{\"name\":\"\"}"), 422).path("error").path("code").asText());
    }
    @Test void filterFailureUsesExistingJsonResolverAndTrace() throws Exception {
        assertEquals("DEPENDENCY_UNAVAILABLE", perform(get("/__tenancy/provider-fail/context").header(TraceContext.HEADER, TRACE), 503).path("error").path("code").asText());
        assertEquals("INTERNAL_ERROR", perform(get("/__tenancy/provider-unknown/context"), 500).path("error").path("code").asText());
    }
    @Test void leakedApplicationScopeIsRejectedAndBoundaryStillCleans() throws Exception { perform(get("/__tenancy/a/leak"), 500); }
    @Test void exactSameWorkerHandlesAThenBThenAnonymousWithoutResidue() throws Exception {
        try (var worker = Executors.newSingleThreadExecutor()) {
            var ids = new HashSet<Long>();
            for (String actor : List.of("a", "b", "anonymous", "a", "anonymous")) {
                ids.add(worker.submit(() -> {
                    assertClean(); assertNull(MDC.get("traceId"));
                    var data = perform(get("/__tenancy/" + actor + "/context"), 200).path("data");
                    assertEquals(actor.equals("a") ? A.toString() : actor.equals("b") ? B.toString() : "none", data.path("tenant").asText());
                    assertEquals(data.path("tenant").asText(), data.path("mdcTenant").asText()); assertEquals("none", data.path("store").asText());
                    assertEquals(Thread.currentThread().threadId(), Long.parseLong(data.path("thread").asText()));
                    assertNull(MDC.get("traceId")); assertClean(); return Thread.currentThread().threadId();
                }).get());
            }
            assertEquals(1, ids.size());
        }
    }
    @Test void synchronousErrorRedispatchUsesServerSnapshotWithoutProviderReread() throws Exception {
        var first = mvc.perform(get("/__tenancy/a/context").header(TraceContext.HEADER, TRACE)).andReturn();
        var request = first.getRequest(); int reads = provider.reads;
        request.setDispatcherType(DispatcherType.ERROR); request.setRequestURI("/error"); request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        var result = mvc.perform(get("/error").with(req -> request)).andReturn();
        assertEquals(500, result.getResponse().getStatus()); assertEquals(TRACE, mapper.readTree(result.getResponse().getContentAsString()).path("traceId").asText());
        assertEquals(reads, provider.reads); assertClean();
    }
    @Test void nestedErrorDispatchRetainsNarrowedScopeAndTrace() throws Exception {
        var filter = new TenantContextFilter(provider, context.getBean("handlerExceptionResolver", HandlerExceptionResolver.class));
        var request = new org.springframework.mock.web.MockHttpServletRequest(); var response = new org.springframework.mock.web.MockHttpServletResponse();
        provider.actor.set("a");
        try {
            new TraceFilter().doFilter(request, response, (req, res) -> filter.doFilter(req, res, (outerReq, outerRes) -> {
                try (var scope = TenantExecutionScope.forPermission(WRITE)) {
                    var outer = TenantContextHolder.required(); request.setDispatcherType(DispatcherType.ERROR);
                    filter.doFilter(request, response, (errorReq, errorRes) -> {
                        assertSame(outer, TenantContextHolder.required()); assertEquals(TraceContext.currentId(), MDC.get("traceId"));
                    });
                }
            }));
            assertClean(); assertNull(MDC.get("traceId"));
        } finally { provider.actor.remove(); }
    }
    @Test void requestStartingAsyncDoesNotRebindIdentityOnLaterError() throws Exception {
        var request = new org.springframework.mock.web.MockHttpServletRequest(); request.setAsyncSupported(true);
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        var filter = new TenantContextFilter(provider, context.getBean("handlerExceptionResolver", HandlerExceptionResolver.class));
        provider.actor.set("a");
        try {
            new TraceFilter().doFilter(request, response, (req, res) -> filter.doFilter(req, res, (innerReq, innerRes) -> {
                assertEquals(A, TenantContextHolder.required().tenantId()); request.startAsync();
            }));
            assertClean(); request.setAsyncStarted(false); request.setDispatcherType(DispatcherType.ERROR);
            int reads = provider.reads;
            new TraceFilter().doFilter(request, response, (req, res) -> filter.doFilter(req, res, (innerReq, innerRes) -> {
                assertClean(); assertEquals(TraceContext.currentId(), MDC.get("traceId"));
            }));
            assertEquals(reads, provider.reads); assertClean();
        } finally { provider.actor.remove(); }
    }
    @Test void asyncRedispatchDoesNotAutomaticallyBindIdentityOrBlockPublicWork() throws Exception {
        var request = new org.springframework.mock.web.MockHttpServletRequest(); request.setDispatcherType(DispatcherType.ASYNC);
        var response = new org.springframework.mock.web.MockHttpServletResponse(); response.setCharacterEncoding("UTF-8"); int reads = provider.reads;
        var filter = new TenantContextFilter(provider, context.getBean("handlerExceptionResolver", HandlerExceptionResolver.class));
        filter.doFilter(request, response, (req, res) -> {
            assertClean(); assertThrows(com.pet.platform.shared.exception.TenantAccessDeniedException.class, TenantScopeGuard::requireBusiness);
            res.getWriter().write("公共流继续");
        });
        assertEquals("公共流继续", response.getContentAsString()); assertEquals(reads, provider.reads); assertClean();
    }
}
