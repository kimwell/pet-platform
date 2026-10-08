package com.pet.platform.protocol;

import com.pet.platform.shared.api.*;
import com.pet.platform.shared.exception.*;
import com.pet.platform.shared.observability.TraceContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.*;
import java.lang.annotation.*;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** 纯协议技术夹具，仅由测试配置注册，未代表行业业务或真实数据。 */
@TestConfiguration(proxyBeanMethods = false)
@Import(ProtocolFixtures.ProtocolController.class)
public class ProtocolFixtures {
    @Bean
    FilterRegistrationBean<Filter> containerErrorFixture() {
        Filter filter = (request, response, chain) -> {
            if (((HttpServletRequest) request).getRequestURI().equals("/__protocol/container-error")) {
                ((HttpServletResponse) response).sendError(500, "TECHNICAL_SECRET_RAW_INPUT");
            } else { chain.doFilter(request, response); }
        };
        var registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        return registration;
    }

    public record Profile(@NotBlank(message = "请输入名称") String name) { }
    public record Item(@NotBlank(message = "请输入名称") String name) { }
    public record Input(@NotBlank(message = "请输入名称") @Size(min = 3, message = "名称至少三个字符") String name,
                        @Valid Profile profile, @Valid List<Item> items, Integer count, Boolean enabled) { }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = CombinationValidator.class)
    public @interface ValidCombination {
        String message() default "组合条件不正确";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    public static class CombinationValidator implements ConstraintValidator<ValidCombination, ObjectInput> {
        @Override
        public boolean isValid(ObjectInput input, ConstraintValidatorContext context) {
            return input == null || input.consistent();
        }
    }

    @ValidCombination
    public record ObjectInput(boolean consistent) { }

    @TestComponent
    @RestController
    @RequestMapping("/__protocol")
    public static class ProtocolController {
        private static final SortWhitelist SORTS = new SortWhitelist(
                Map.of("createdAt", "createdAt", "name", "displayName", "id", "id"),
                "createdAt", SortRule.Direction.DESC, "id");

        @GetMapping("/success")
        ApiResponse<Map<String, String>> success() {
            if (!TraceContext.currentId().equals(MDC.get(TraceContext.MDC_KEY))) {
                throw new IllegalStateException("追踪上下文不一致");
            }
            return ApiResponse.success(Map.of("message", "协议检查"));
        }

        @GetMapping("/null")
        ApiResponse<Void> empty() { return ApiResponse.success(null); }

        @GetMapping("/created")
        ResponseEntity<ApiResponse<Map<String, String>>> created() {
            return ResponseEntity.status(201).header("Location", "/__protocol/success")
                    .body(ApiResponse.success(Map.of("id", "technical-id")));
        }

        @GetMapping("/business/{code}")
        ApiResponse<Void> business(@PathVariable ErrorCode code) {
            if (code == ErrorCode.RATE_LIMITED) { throw BusinessException.rateLimited(12); }
            throw new BusinessException(code);
        }

        @GetMapping("/tenant-denied")
        ApiResponse<Void> tenantDenied() { throw new TenantAccessDeniedException(); }

        @GetMapping("/permission-denied")
        ApiResponse<Void> permissionDenied() { throw new PermissionDeniedException(); }

        @GetMapping("/conflict")
        ApiResponse<Void> conflict() { throw new ConflictException(ErrorCode.VERSION_CONFLICT); }

        @GetMapping("/unknown")
        ApiResponse<Void> unknown() { throw new IllegalArgumentException("TECHNICAL_SECRET_RAW_INPUT SELECT password FROM private_table"); }

        @GetMapping("/framework-status")
        ApiResponse<Void> frameworkStatus() { throw new ResponseStatusException(HttpStatus.GONE, "TECHNICAL_SECRET_RAW_INPUT"); }

        @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<Void> validate(@Valid @RequestBody Input input) { return ApiResponse.success(null); }

        @PostMapping(value = "/method-body", consumes = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<Void> methodBody(@Valid @RequestBody Input input, @RequestParam @Min(1) int limit) {
            return ApiResponse.success(null);
        }

        @PostMapping(value = "/object", consumes = MediaType.APPLICATION_JSON_VALUE)
        ApiResponse<Void> object(@Valid @RequestBody ObjectInput input) { return ApiResponse.success(null); }

        @GetMapping("/parameter")
        ApiResponse<Void> parameter(@RequestParam int count) { return ApiResponse.success(null); }

        @GetMapping("/method")
        ApiResponse<Void> method(@RequestParam @Min(1) int count) { return ApiResponse.success(null); }

        @GetMapping("/return-validation")
        @NotNull
        ApiResponse<Void> invalidReturn() { return null; }

        @GetMapping("/page")
        ApiResponse<PageResponse<String>> page(HttpServletRequest request) {
            var query = PageQuery.from(request.getParameterMap());
            SORTS.resolve(request.getParameterMap());
            return ApiResponse.success(PageResponse.of(List.of(), query, 0));
        }

        @GetMapping("/sort")
        ApiResponse<List<SortRule>> sort(HttpServletRequest request) {
            return ApiResponse.success(SORTS.resolve(request.getParameterMap()));
        }

        @GetMapping("/file")
        ResponseEntity<byte[]> file() {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=protocol.bin")
                    .body(new byte[] {0, 1, 2, 3});
        }
    }
}
