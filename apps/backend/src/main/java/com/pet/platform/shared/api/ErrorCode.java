package com.pet.platform.shared.api;

import org.springframework.http.HttpStatus;

/** 公共协议注册表；身份相关条目只定义映射，不实现认证或授权。 */
public enum ErrorCode {
    BAD_REQUEST(400, "请求格式或参数不正确"),
    PAGINATION_INVALID(400, "分页参数不正确"),
    SORT_INVALID(400, "排序参数不正确"),
    AUTH_CREDENTIAL_AMBIGUOUS(400, "认证凭据不能混用"),
    AUTH_REQUIRED(401, "请先登录"),
    SESSION_EXPIRED(401, "登录已过期，请重新登录"),
    SESSION_REVOKED(401, "登录已失效，请重新登录"),
    AUTH_DOMAIN_MISMATCH(401, "登录身份不匹配"),
    LOGIN_FAILED(401, "登录失败，请检查登录信息"),
    PERMISSION_DENIED(403, "没有执行此操作的权限"),
    CSRF_INVALID(403, "请求校验失败，请刷新后重试"),
    RESOURCE_NOT_FOUND(404, "资源不存在或不可访问"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    NOT_ACCEPTABLE(406, "不支持请求的响应格式"),
    BUSINESS_STATE_CONFLICT(409, "当前状态不允许此操作"),
    DUPLICATE_RESOURCE(409, "资源已存在"),
    VERSION_CONFLICT(409, "资源已更新，请重新加载后重试"),
    SESSION_LIMIT_REACHED(409, "登录设备数量已达上限"),
    IDEMPOTENCY_CONFLICT(409, "重复请求存在冲突"),
    PAYLOAD_TOO_LARGE(413, "请求内容超过大小限制"),
    UNSUPPORTED_MEDIA_TYPE(415, "请求内容类型不支持"),
    VALIDATION_FAILED(422, "请检查填写内容"),
    DATE_RANGE_INVALID(422, "日期范围不正确"),
    AMOUNT_INVALID(422, "金额不正确"),
    RESULT_TOO_LARGE(422, "查询范围过大，请调整查询条件"),
    RATE_LIMITED(429, "请求过于频繁，请稍后重试"),
    INTERNAL_ERROR(500, "系统暂时无法处理请求，请稍后重试"),
    DEPENDENCY_UNAVAILABLE(503, "服务暂时不可用，请稍后重试"),
    CAPABILITY_DISABLED(503, "当前能力尚未启用"),
    REQUEST_REJECTED(400, "请求无法处理");

    private final HttpStatus status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = HttpStatus.valueOf(status);
        this.message = message;
    }

    public HttpStatus status() { return status; }
    public String message() { return message; }

    /** 未登记的框架状态保留原 HTTP 数值，使用安全兜底代码。 */
    public static ErrorCode forHttpStatus(int status) {
        return switch (status) {
            case 400 -> BAD_REQUEST;
            case 401 -> AUTH_REQUIRED;
            case 403 -> PERMISSION_DENIED;
            case 404 -> RESOURCE_NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 406 -> NOT_ACCEPTABLE;
            case 409 -> BUSINESS_STATE_CONFLICT;
            case 413 -> PAYLOAD_TOO_LARGE;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            case 422 -> VALIDATION_FAILED;
            case 429 -> RATE_LIMITED;
            case 503 -> DEPENDENCY_UNAVAILABLE;
            default -> status >= 500 ? INTERNAL_ERROR : REQUEST_REJECTED;
        };
    }
}
