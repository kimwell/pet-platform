package com.pet.platform.shared.observability;

import org.slf4j.MDC;

/** 显式进程内追踪范围，只管理traceId；不授予身份，也不复制任意MDC。 */
public final class TraceScope implements AutoCloseable {
    private final Thread owner = Thread.currentThread();
    private final String previous;
    private final String previousMdc;
    private boolean closed;
    private TraceScope(String traceId) {
        if (traceId != null && !traceId.matches("[0-9a-f]{32}")) throw new IllegalArgumentException("追踪标识格式不正确");
        previousMdc = MDC.get(TraceContext.MDC_KEY);
        previous = TraceContext.replace(traceId);
        if (traceId == null) MDC.remove(TraceContext.MDC_KEY); else MDC.put(TraceContext.MDC_KEY, traceId);
    }
    public static TraceScope open(String traceId) { return new TraceScope(traceId); }
    @Override public void close() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("追踪范围必须在创建线程关闭");
        if (closed) return;
        TraceContext.replace(previous);
        if (previousMdc == null) MDC.remove(TraceContext.MDC_KEY); else MDC.put(TraceContext.MDC_KEY, previousMdc);
        closed = true;
    }
}
