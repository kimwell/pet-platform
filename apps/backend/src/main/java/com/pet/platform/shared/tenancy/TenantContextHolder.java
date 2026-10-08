package com.pet.platform.shared.tenancy;

import com.pet.platform.shared.exception.TenantAccessDeniedException;
import java.util.Optional;

/** 只有执行范围实现可修改底层存储；公开API只读。不隐式传播到子线程。 */
public final class TenantContextHolder {
    private static final ThreadLocal<TenantExecutionScope> CURRENT = new ThreadLocal<>();
    private TenantContextHolder() { }
    public static Optional<TenantContext> current() { return Optional.ofNullable(CURRENT.get()).map(TenantExecutionScope::context); }
    public static TenantContext required() { return current().orElseThrow(TenantAccessDeniedException::new); }
    static TenantExecutionScope frame() { return CURRENT.get(); }
    static void replace(TenantExecutionScope scope) { if (scope == null) CURRENT.remove(); else CURRENT.set(scope); }
}
