package com.pet.testing.tenancy;

import com.pet.platform.shared.api.ApiResponse;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.security.*;
import com.pet.platform.shared.tenancy.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.*;

/** 仅测试源码的技术身份与端点；路径选择身份模拟可信认证，不进入生产扫描或JAR。 */
@Configuration(proxyBeanMethods = false)
@Import(TenancyFixtures.Controller.class)
public class TenancyFixtures {
    static final UUID A = UUID.fromString("11111111-1111-4111-8111-111111111111");
    static final UUID B = UUID.fromString("22222222-2222-4222-8222-222222222222");
    static final UUID OPERATOR = UUID.fromString("33333333-3333-4333-8333-333333333333");
    static final UUID STORE = UUID.fromString("44444444-4444-4444-8444-444444444444");
    static final UUID OTHER_STORE = UUID.fromString("55555555-5555-4555-8555-555555555555");
    static final UUID SESSION = UUID.fromString("66666666-6666-4666-8666-666666666666");
    static final String READ = "probe:item:list";
    static final String WRITE = "probe:item:update";
    static DataScope scope(UUID tenant, PrincipalType type, Set<DataScopeType> types, Set<UUID> stores) {
        return new DataScope(tenant, type, OPERATOR, types, stores);
    }
    static CurrentPrincipal staff(UUID tenant) {
        return new CurrentPrincipal(PrincipalType.STAFF, OPERATOR, tenant, SESSION, 3, Set.of(READ, WRITE),
                Set.of(STORE, OTHER_STORE), Map.of(READ, scope(tenant, PrincipalType.STAFF, Set.of(DataScopeType.TENANT), Set.of()),
                    WRITE, scope(tenant, PrincipalType.STAFF, Set.of(DataScopeType.STORES), Set.of(STORE))));
    }
    static CurrentPrincipal customer() {
        return new CurrentPrincipal(PrincipalType.CUSTOMER, OPERATOR, A, SESSION, 2, Set.of(READ), Set.of(),
                Map.of(READ, scope(A, PrincipalType.CUSTOMER, Set.of(DataScopeType.SELF), Set.of())));
    }
    static CurrentPrincipal platform() {
        return new CurrentPrincipal(PrincipalType.PLATFORM, OPERATOR, null, SESSION, 1, Set.of(READ), Set.of(), Map.of());
    }
    @Bean @Primary TestPrincipalProvider testPrincipalProvider() { return new TestPrincipalProvider(); }
    @Bean FilterRegistrationBean<FixtureAuthenticationFilter> fixtureAuthenticationRegistration(TestPrincipalProvider provider) {
        var bean = new FilterRegistrationBean<>(new FixtureAuthenticationFilter(provider));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 5); bean.setDispatcherTypes(DispatcherType.REQUEST);
        return bean;
    }
    static final class TestPrincipalProvider implements CurrentPrincipalProvider {
        final ThreadLocal<String> actor = new ThreadLocal<>();
        int reads;
        @Override public Optional<CurrentPrincipal> currentPrincipal() {
            reads++;
            return switch (Optional.ofNullable(actor.get()).orElse("anonymous")) {
                case "a" -> Optional.of(staff(A)); case "b" -> Optional.of(staff(B));
                case "customer" -> Optional.of(customer()); case "platform" -> Optional.of(platform());
                case "provider-fail" -> throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);
                case "provider-unknown" -> throw new IllegalStateException("TECHNICAL_SECRET");
                default -> Optional.empty();
            };
        }
    }
    static final class FixtureAuthenticationFilter implements Filter {
        final TestPrincipalProvider provider;
        FixtureAuthenticationFilter(TestPrincipalProvider provider) { this.provider = provider; }
        @Override public void doFilter(ServletRequest raw, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            var request = (HttpServletRequest) raw; String[] parts = request.getRequestURI().split("/");
            provider.actor.set(parts.length > 2 ? parts[2] : "anonymous");
            try { chain.doFilter(request, response); } finally { provider.actor.remove(); }
        }
    }
    record Input(@NotBlank(message = "请输入名称") String name, UUID tenantId) { }
    @RestController
    @RequestMapping("/__tenancy/{actor}")
    static class Controller {
        @GetMapping("/context") ApiResponse<Map<String, String>> context() { return snapshot(); }
        @PostMapping("/context") ApiResponse<Map<String, String>> input(@Valid @RequestBody Input input) { return snapshot(); }
        @GetMapping("/root-business") ApiResponse<String> rootBusiness() {
            return ApiResponse.success(TenantScopeGuard.requireBusiness().tenantId().toString());
        }
        @GetMapping("/business") ApiResponse<String> business() {
            try (var scope = TenantExecutionScope.forPermission(READ)) {
                return ApiResponse.success(TenantScopeGuard.requireBusiness().tenantId().toString());
            }
        }
        @GetMapping("/unknown") ApiResponse<Void> unknown() {
            try (var scope = TenantExecutionScope.forPermission(READ)) { throw new IllegalStateException("TECHNICAL_SECRET"); }
        }
        @GetMapping("/container-error") void container(HttpServletResponse response) throws IOException { response.sendError(500, "TECHNICAL_SECRET"); }
        @GetMapping("/leak") void leak() { TenantExecutionScope.forPermission(READ); }
        private ApiResponse<Map<String, String>> snapshot() {
            var c = TenantContextHolder.current();
            return ApiResponse.success(Map.of("tenant", c.map(v -> v.tenantId().toString()).orElse("none"),
                "type", c.map(v -> v.principalType().name()).orElse("none"), "mdcTenant", Optional.ofNullable(MDC.get("tenantId")).orElse("none"),
                "store", Optional.ofNullable(MDC.get("storeId")).orElse("none"), "thread", Long.toString(Thread.currentThread().threadId())));
        }
    }
}
