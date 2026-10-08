package com.pet.platform.shared.api;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.pet.platform.shared.observability.TraceContext;
import java.util.Objects;

/** 显式信封：两个分支从结构上排除 data/error 同时存在。 */
@JsonPropertyOrder({"success", "data", "error", "traceId"})
public abstract sealed class ApiResponse<T> {
    private final String traceId;

    private ApiResponse() {
        this.traceId = TraceContext.currentId();
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("success")
    public abstract boolean success();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^[0-9a-f]{32}$")
    @JsonProperty("traceId")
    public final String traceId() {
        return traceId;
    }

    public static <T> Success<T> success(T data) {
        return new Success<>(data);
    }

    public static Failure failure(ApiError error) {
        return new Failure(Objects.requireNonNull(error));
    }

    public static final class Success<T> extends ApiResponse<T> {
        private final T data;

        private Success(T data) {
            this.data = data;
        }

        @Override
        public boolean success() { return true; }

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        @JsonProperty("data")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        public T data() { return data; }
    }

    public static final class Failure extends ApiResponse<Void> {
        private final ApiError error;

        private Failure(ApiError error) { this.error = error; }

        @Override
        public boolean success() { return false; }

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("error")
        public ApiError error() { return error; }
    }
}
