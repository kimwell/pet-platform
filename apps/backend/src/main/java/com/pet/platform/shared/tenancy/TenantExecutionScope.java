package com.pet.platform.shared.tenancy;

import com.pet.platform.shared.exception.PermissionDeniedException;
import com.pet.platform.shared.exception.TenantAccessDeniedException;
import com.pet.platform.shared.observability.TraceContext;
import com.pet.platform.shared.security.CurrentPrincipal;
import com.pet.platform.shared.security.PrincipalType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;

/** try-with-resources负责LIFO关闭；入口只在可信Provider边界内部开放。 */
public final class TenantExecutionScope implements AutoCloseable {
    private static final String[] MDC_KEYS = {"tenantId", "operatorId", "storeId"};
    private final Thread owner = Thread.currentThread();
    private final TenantExecutionScope previous;
    private final TenantContext context;
    private final Map<String, DataScope> grants;
    private final TaskDeadline deadline;
    private final Map<String, String> previousMdc = new HashMap<>();
    private boolean closed;
    private boolean sessionBacked;

    private TenantExecutionScope(TenantContext context, Map<String, DataScope> grants) {
        this(context, grants, TenantContextHolder.frame() == null ? null : TenantContextHolder.frame().deadline);
    }

    private TenantExecutionScope(TenantContext context, Map<String, DataScope> grants, TaskDeadline deadline) {
        this.previous = TenantContextHolder.frame(); this.context = context; this.grants = grants;
        this.deadline = deadline;
        this.sessionBacked = previous != null && previous.sessionBacked;
        for (String key : MDC_KEYS) previousMdc.put(key, MDC.get(key));
        TenantContextHolder.replace(this);
        MDC.put("tenantId", context.tenantId().toString()); MDC.put("operatorId", context.principalId().toString());
        if (context.currentStoreId() == null) MDC.remove("storeId"); else MDC.put("storeId", context.currentStoreId().toString());
    }

    static TenantExecutionScope openIdentity(CurrentPrincipal principal) {
        if (principal.principalType() == PrincipalType.PLATFORM || TenantContextHolder.current().isPresent()) throw new TenantAccessDeniedException();
        String trace;
        try { trace = TraceContext.currentId(); } catch (IllegalStateException absent) { trace = null; }
        return new TenantExecutionScope(new TenantContext(principal.tenantId(), principal.principalType(), principal.principalId(),
                principal.sessionId(), principal.authorizationVersion(), null, null, principal.authorizedStoreIds(), null,
                trace, TenantPurpose.AUTHORITY_READ), principal.grants());
    }

    static TenantExecutionScope openSessionIdentity(CurrentPrincipal principal) {
        var frame=openIdentity(principal);frame.sessionBacked=true;return frame;
    }

    public static TenantExecutionScope forPermission(String permissionCode) {
        var outer = TenantContextHolder.frame();
        if (outer == null) throw new TenantAccessDeniedException();
        outer.context(); // 异步期限也约束选择嵌套权限。
        var scope = outer.grants.get(permissionCode);
        if (scope == null) throw new PermissionDeniedException();
        if (outer.context.purpose() == TenantPurpose.BUSINESS) {
            if (!permissionCode.equals(outer.context.permissionCode())) throw new PermissionDeniedException();
            scope = outer.context.dataScope();
        }
        return openBusiness(outer, permissionCode, scope, outer.context.currentStoreId());
    }

    /** 仅从当前业务范围收窄；currentStoreId只可经StoreScopeGuard绑定。 */
    public static TenantExecutionScope narrow(DataScope scope) {
        var outer = TenantContextHolder.frame(); TenantScopeGuard.requireBusiness();
        if (!scope.isSubsetOf(outer.context.dataScope())) throw new TenantAccessDeniedException();
        return openBusiness(outer, outer.context.permissionCode(), scope, outer.context.currentStoreId());
    }

    static TenantExecutionScope withVerifiedStore(UUID storeId) {
        var outer = TenantContextHolder.frame(); TenantScopeGuard.requireBusiness();
        if (outer.context.currentStoreId() != null && !outer.context.currentStoreId().equals(storeId)) throw new TenantAccessDeniedException();
        return openBusiness(outer, outer.context.permissionCode(), outer.context.dataScope(), storeId);
    }

    private static TenantExecutionScope openBusiness(TenantExecutionScope outer, String permission, DataScope scope, UUID store) {
        var c = outer.context;
        return new TenantExecutionScope(new TenantContext(c.tenantId(), c.principalType(), c.principalId(), c.sessionId(),
                c.authorizationVersion(), permission, scope, c.authorizedStoreIds(), store, c.traceId(), TenantPurpose.BUSINESS), outer.grants);
    }
    static TenantExecutionScope openTask(TenantContext context, TaskDeadline deadline) {
        if (hasWorkerContext()) throw new TenantAccessDeniedException();
        deadline.verify();
        return new TenantExecutionScope(context, Map.of(context.permissionCode(), context.dataScope()), deadline);
    }

    static TaskDeadline captureTaskDeadline(long maxAgeNanos, java.util.function.LongSupplier ticker) {
        var frame = TenantContextHolder.frame(); TenantScopeGuard.requireBusiness();
        if(frame.sessionBacked) throw new PermissionDeniedException();
        long remaining = frame.deadline == null ? maxAgeNanos : Math.min(maxAgeNanos, frame.deadline.remaining());
        var result = new TaskDeadline(ticker.getAsLong(), remaining, ticker);
        result.verify(); return result;
    }
    static boolean hasWorkerContext() { return TenantContextHolder.frame() != null; }

    TenantContext context() { if (deadline != null) deadline.verify(); return context; }

    @Override public void close() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("执行范围必须在创建线程关闭");
        if (closed) return;
        if (TenantContextHolder.frame() != this) throw new IllegalStateException("执行范围必须按后进先出顺序关闭");
        restore();
    }

    /** HTTP/同步后台根边界最终兜底：先移除遗漏的内层范围，再报告生命周期错误。 */
    void finishBoundary() {
        if (Thread.currentThread() != owner || previous != null) throw new IllegalStateException("只有创建线程的根边界可结束");
        if (closed && TenantContextHolder.frame() == null) return;
        boolean leaked = TenantContextHolder.frame() != this;
        restore();
        if (leaked) throw new IllegalStateException("内层执行范围未正确关闭");
    }
    private void restore() {
        TenantContextHolder.replace(previous);
        for (String key : MDC_KEYS) {
            String value = previousMdc.get(key); if (value == null) MDC.remove(key); else MDC.put(key, value);
        }
        closed = true;
    }
}
