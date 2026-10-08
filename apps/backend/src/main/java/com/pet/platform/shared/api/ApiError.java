package com.pet.platform.shared.api;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Objects;

public record ApiError(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) ErrorCode code, @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String message,
                       @io.swagger.v3.oas.annotations.media.ArraySchema(minItems = 1) @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldErrorDetail> fieldErrors) {
    public ApiError {
        Objects.requireNonNull(code);
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("公开错误提示不能为空");
        }
        fieldErrors = fieldErrors == null ? List.of() : FieldErrorDetail.normalize(fieldErrors);
    }

    public ApiError(ErrorCode code) { this(code, code.message(), List.of()); }
}
