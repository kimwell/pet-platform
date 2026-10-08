package com.pet.platform.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/** 单值入站校验；REQUEST 与同步 ERROR 分发复用请求属性，不记录非法原值。 */
public final class TraceFilter extends OncePerRequestFilter {
    private static final String ATTRIBUTE = TraceFilter.class.getName() + ".id";

    @Override
    protected boolean shouldNotFilterErrorDispatch() { return false; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String id = (String) request.getAttribute(ATTRIBUTE);
        if (id == null) {
            var headers = Collections.list(request.getHeaders(TraceContext.HEADER));
            id = headers.size() == 1 && headers.getFirst().matches("[0-9a-f]{32}")
                    ? headers.getFirst() : UUID.randomUUID().toString().replace("-", "");
            request.setAttribute(ATTRIBUTE, id);
        }
        String previousId = TraceContext.replace(id);
        String previousMdc = MDC.get(TraceContext.MDC_KEY);
        MDC.put(TraceContext.MDC_KEY, id);
        response.setHeader(TraceContext.HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            TraceContext.replace(previousId);
            if (previousMdc == null) { MDC.remove(TraceContext.MDC_KEY); }
            else { MDC.put(TraceContext.MDC_KEY, previousMdc); }
        }
    }

    @Override
    protected void doFilterNestedErrorDispatch(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain chain) throws ServletException, IOException {
        doFilterInternal(request, response, chain);
    }
}
