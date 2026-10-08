package com.pet.platform.shared.tenancy;

import java.util.function.LongSupplier;

/** 非序列化的进程内单调时钟期限；测试时钟只经包内构造注入。 */
record TaskDeadline(long started, long budget, LongSupplier ticker) {
    long remaining() { return budget - (ticker.getAsLong() - started); }
    void verify() {
        if (remaining() <= 0) throw new java.util.concurrent.RejectedExecutionException("进程内任务授权快照已超过支持时效");
    }
}
