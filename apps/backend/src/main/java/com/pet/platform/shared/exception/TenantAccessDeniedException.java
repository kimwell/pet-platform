package com.pet.platform.shared.exception;

/** 只定义不可见资源映射，未实现可信租户上下文或隔离机制。 */
public final class TenantAccessDeniedException extends ResourceNotFoundException { }
