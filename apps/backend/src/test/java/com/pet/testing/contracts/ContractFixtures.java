package com.pet.testing.contracts;

import com.fasterxml.jackson.annotation.*;
import com.pet.platform.shared.api.*;
import com.pet.platform.shared.exception.BusinessException;
import com.pet.platform.shared.serialization.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;

/** 仅生成器和HTTP验收用的技术契约，无业务语义，绝不进入生产扫描/产物。 */
@TestConfiguration(proxyBeanMethods = false)
@Import(ContractFixtures.ContractController.class)
public class ContractFixtures {
    @Bean GroupedOpenApi testContractGroup() {
        return GroupedOpenApi.builder().group("test-contract").packagesToScan("com.pet.testing.contracts")
                .pathsToMatch("/__contracts/**").build();
    }
    public enum ProbeCode { OPEN, CLOSED }
    public record ScalarInput(
            @NotNull(message = "请输入ID") UUID id,
            @NotNull(message = "请输入金额") @CnyAmount(input = true) BigDecimal amount,
            @NotNull(message = "请选择币种") CurrencyCode currency,
            @NotNull(message = "请输入计数") @DecimalCounter Long version,
            @NotNull(message = "请输入时间点") Instant occurredAt,
            @NotNull(message = "请输入日期") LocalDate date,
            @NotNull(message = "请选择状态") ProbeCode state,
            @NotNull(message = "请选择开关") Boolean enabled,
            @NotNull(message = "请输入进度") @Min(value = 0, message = "进度不能小于0") @Max(value = 100, message = "进度不能大于100") Integer progress,
            @NotNull(message = "请输入比例") BigDecimal ratio,
            @NotNull(message = "集合不能为null") List<String> items,
            @JsonProperty(required = true) @Schema(types = {"string", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) String requiredNullable,
            @JsonInclude(JsonInclude.Include.NON_NULL) @Schema(types = {"string", "null"}, requiredMode = Schema.RequiredMode.NOT_REQUIRED) String optional) { }
    public record ScalarOutput(
            @NotNull(message = "请输入ID") UUID id,
            @NotNull(message = "请输入金额") @CnyAmount BigDecimal amount,
            @NotNull(message = "请选择币种") CurrencyCode currency,
            @NotNull(message = "请输入计数") @DecimalCounter Long version,
            @NotNull(message = "请输入时间点") @Schema(pattern = ScalarCodecs.INSTANT_PATTERN) Instant occurredAt,
            @NotNull(message = "请输入日期") LocalDate date,
            @NotNull(message = "请选择状态") ProbeCode state,
            @NotNull(message = "请选择开关") Boolean enabled,
            @NotNull(message = "请输入进度") @Min(value = 0, message = "进度不能小于0") @Max(value = 100, message = "进度不能大于100") Integer progress,
            @NotNull(message = "请输入比例") BigDecimal ratio,
            @NotNull(message = "集合不能为null") List<String> items,
            @JsonProperty(required = true) @Schema(types = {"string", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) String requiredNullable,
            @JsonInclude(JsonInclude.Include.NON_NULL) @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String optional) { }
    public enum CurrencyCode { CNY }

    @RestController
    @RequestMapping(value = "/__contracts", produces = "application/json")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求格式错误", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.Failure.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "输入语义校验失败", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.Failure.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "安全内部错误", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.Failure.class)))})
    public static class ContractController {
        @PostMapping("/scalars") @Operation(operationId = "testEchoScalars")
        public ApiResponse<ScalarOutput> scalars(@Valid @RequestBody ScalarInput input) {
            return ApiResponse.success(new ScalarOutput(input.id(), input.amount(), input.currency(), input.version(),
                    input.occurredAt(), input.date(), input.state(), input.enabled(), input.progress(), input.ratio(),
                    input.items(), input.requiredNullable(), input.optional()));
        }
        @GetMapping("/page") @Operation(operationId = "testScalarPage")
        public ApiResponse<PageResponse<ScalarOutput>> page() {
            return ApiResponse.success(PageResponse.of(List.of(), new PageQuery(1, 20), 9007199254740993L));
        }
        @GetMapping("/null") @Operation(operationId = "testNullSuccess")
        public ApiResponse<Void> empty() { return ApiResponse.success(null); }
        @GetMapping("/error") @Operation(operationId = "testSafeError")
        public ApiResponse<Void> error() { throw new BusinessException(ErrorCode.VALIDATION_FAILED); }
    }
}
