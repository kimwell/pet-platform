package com.pet.platform.shared.exception;

import com.pet.platform.shared.api.ApiError;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.api.FieldErrorDetail;
import java.util.List;

/** message 只能是用例明确允许公开的中文信息，禁止传入底层异常 message。 */
public class BusinessException extends RuntimeException {
    private final ApiError error;
    private final Long retryAfterSeconds;

    public BusinessException(ErrorCode code) { this(new ApiError(code), null); }

    public BusinessException(ErrorCode code, String publicMessage) {
        this(new ApiError(code, publicMessage, List.of()), null);
    }

    public BusinessException(ErrorCode code, List<FieldErrorDetail> fieldErrors) {
        this(new ApiError(code, code.message(), fieldErrors), null);
    }

    private BusinessException(ApiError error, Long retryAfterSeconds) {
        super(error.message());
        this.error = error;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static BusinessException rateLimited(long seconds) {
        if (seconds < 1) { throw new IllegalArgumentException("重试等待秒数必须为正整数"); }
        return new BusinessException(new ApiError(ErrorCode.RATE_LIMITED), seconds);
    }

    public ApiError error() { return error; }
    public Long retryAfterSeconds() { return retryAfterSeconds; }
}
