package com.pet.platform.shared.observability;

/** 仅当前同步分发线程可用；不向异步任务隐式继承。 */
public final class TraceContext {
    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TraceContext() { }

    public static String currentId() {
        String id = CURRENT.get();
        if (id == null) { throw new IllegalStateException("当前线程没有请求追踪上下文"); }
        return id;
    }

    static String replace(String id) {
        String previous = CURRENT.get();
        if (id == null) { CURRENT.remove(); } else { CURRENT.set(id); }
        return previous;
    }
}
