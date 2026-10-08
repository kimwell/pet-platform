package com.pet.platform.shared.exception;

import com.pet.platform.shared.api.ApiError;
import com.pet.platform.shared.api.ApiResponse;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.observability.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pet.platform.shared.serialization.ScalarCodecs.AmountInputException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Object> business(BusinessException exception) {
        var headers = new HttpHeaders();
        if (exception.retryAfterSeconds() != null) {
            headers.set(HttpHeaders.RETRY_AFTER, exception.retryAfterSeconds().toString());
        }
        return failure(exception.error(), headers, exception.error().code().status());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception exception) {
        logInternal(exception);
        return failure(new ApiError(ErrorCode.INTERNAL_ERROR), new HttpHeaders(), ErrorCode.INTERNAL_ERROR.status());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // 只识别明确的金额语义异常；其他JSON语法/类型错误仍为400，不泄露cause。
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof AmountInputException) {
                return failure(new ApiError(ErrorCode.AMOUNT_INVALID), headers, ErrorCode.AMOUNT_INVALID.status());
            }
        }
        return handleExceptionInternal(exception, null, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return failure(new ApiError(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.message(),
                ValidationErrors.from(exception.getBindingResult())), headers, ErrorCode.VALIDATION_FAILED.status());
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (exception.isForReturnValue()) {
            return handleExceptionInternal(exception, null, headers, status, request);
        }
        return failure(new ApiError(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.message(),
                ValidationErrors.from(exception)), headers, ErrorCode.VALIDATION_FAILED.status());
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (status.is5xxServerError()) { logInternal(exception); }
        // 让父类继续处理已提交响应等框架边界，只替换公开错误体，保留 Allow 等响应头。
        var safeHeaders = new HttpHeaders();
        safeHeaders.putAll(headers);
        safeHeaders.set(HttpHeaders.CACHE_CONTROL, "no-store");
        safeHeaders.setContentType(MediaType.APPLICATION_JSON);
        return super.handleExceptionInternal(exception, ApiResponse.failure(new ApiError(
                ErrorCode.forHttpStatus(status.value()))), safeHeaders, status, request);
    }

    private ResponseEntity<Object> failure(ApiError error, HttpHeaders headers, HttpStatusCode status) {
        var safeHeaders = new HttpHeaders();
        safeHeaders.putAll(headers);
        safeHeaders.set(HttpHeaders.CACHE_CONTROL, "no-store");
        safeHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(ApiResponse.failure(error), safeHeaders, status);
    }

    private void logInternal(Exception exception) {
        // 不打印异常原文、cause、堆栈或请求值，保留 trace 与异常种类用于关联排查。
        LOG.error("请求处理失败，traceId={}，异常类型={}", TraceContext.currentId(), exception.getClass().getName());
    }
}
