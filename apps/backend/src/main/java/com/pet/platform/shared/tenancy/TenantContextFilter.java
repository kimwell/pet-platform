package com.pet.platform.shared.tenancy;

import com.pet.platform.shared.security.CurrentPrincipal;
import com.pet.platform.shared.security.CurrentPrincipalProvider;
import com.pet.platform.shared.security.PrincipalType;
import com.pet.platform.shared.exception.TenantAccessDeniedException;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** 位于TraceFilter之后，仅同步REQUEST/ERROR；ERROR复用服务端身份快照，不重新读取客户端载体。 */
public final class TenantContextFilter extends OncePerRequestFilter {
    private static final String PRINCIPAL_ATTRIBUTE = TenantContextFilter.class.getName() + ".principal";
    private final CurrentPrincipalProvider provider;
    private final HandlerExceptionResolver resolver;
    public TenantContextFilter(CurrentPrincipalProvider provider, HandlerExceptionResolver resolver) {
        this.provider = provider; this.resolver = resolver;
    }
    @Override protected boolean shouldNotFilterErrorDispatch() { return false; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            if (TenantContextHolder.current().isPresent()) throw new TenantAccessDeniedException();
            CurrentPrincipal principal;
            if (request.getDispatcherType() == DispatcherType.ERROR) {
                principal = (CurrentPrincipal) request.getAttribute(PRINCIPAL_ATTRIBUTE);
            } else {
                principal = provider.currentPrincipal().orElse(null);
                request.setAttribute(PRINCIPAL_ATTRIBUTE, principal);
            }
            if (principal == null || principal.principalType() == PrincipalType.PLATFORM) {
                chain.doFilter(request, response);
            } else {
                var boundary = TenantExecutionScope.openIdentity(principal);
                try { chain.doFilter(request, response); }
                finally { boundary.finishBoundary(); }
            }
        } catch (Exception exception) {
            if (response.isCommitted() || resolver.resolveException(request, response, null, exception) == null) {
                if (exception instanceof ServletException servlet) throw servlet;
                if (exception instanceof IOException io) throw io;
                throw new ServletException("请求执行边界失败", exception);
            }
        } finally {
            // 异步启动后不保留身份供后续ERROR重绑，不能把同步快照当异步传播协议。
            if (request.isAsyncStarted()) request.removeAttribute(PRINCIPAL_ATTRIBUTE);
        }
    }
    @Override protected void doFilterNestedErrorDispatch(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 同步嵌套ERROR沿用当前范围，不打开更大的根范围。
        chain.doFilter(request, response);
    }
}
