package com.pet.platform.shared.exception;

/** 租户缺上下文或资源不可访问统一404，不泄露其他租户资源存在性。 */
public final class TenantAccessDeniedException extends ResourceNotFoundException { }
