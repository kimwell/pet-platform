package com.pet.platform.shared.tenancy;

import com.pet.platform.shared.observability.TraceContext;
import com.pet.platform.shared.observability.TraceScope;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 显式短期进程内任务；不继承事务，不提供任意租户快照安装或持久化投递。 */
public final class TenantTaskExecutor implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TenantTaskExecutor.class);
    private final WorkerPool pool;
    private com.pet.platform.shared.security.TaskAuthority authority;
    private final long maxAgeNanos;
    private final Duration shutdownWait;
    private final LongSupplier ticker;
    private final ThreadLocal<Boolean> quarantined = new ThreadLocal<>();

    private record Snapshot(TenantContext context, TaskDeadline deadline, com.pet.platform.shared.security.TaskAuthority.Proof proof) { }

    public TenantTaskExecutor(int threads, int capacity, Duration maxAge, Duration shutdownWait) {
        this(threads, capacity, maxAge, shutdownWait, System::nanoTime);
    }
    public TenantTaskExecutor(int threads,int capacity,Duration maxAge,Duration shutdownWait,com.pet.platform.shared.security.TaskAuthority authority) {
        this(threads,capacity,maxAge,shutdownWait);this.authority=authority;
    }
    TenantTaskExecutor(int threads, int capacity, Duration maxAge, Duration shutdownWait, LongSupplier ticker) {
        if (threads < 1 || threads > 16 || capacity < 1 || capacity > 1024
                || maxAge == null || maxAge.isZero() || maxAge.isNegative() || maxAge.compareTo(Duration.ofSeconds(60)) > 0
                || shutdownWait == null || shutdownWait.isZero() || shutdownWait.isNegative() || shutdownWait.compareTo(Duration.ofSeconds(30)) > 0) {
            throw new IllegalArgumentException("异步线程、队列、快照时效或停止等待配置超出有限资源范围");
        }
        this.maxAgeNanos = maxAge.toNanos(); this.shutdownWait = shutdownWait; this.ticker = Objects.requireNonNull(ticker);
        var sequence = new AtomicInteger();
        ThreadFactory factory = action -> {
            var thread = new Thread(null, action, "pet-tenant-task-" + sequence.incrementAndGet(), 0, false);
            thread.setUncaughtExceptionHandler((worker, failure) -> LOG.error("受控任务工作线程因状态残留退出"));
            return thread;
        };
        pool = new WorkerPool(threads, capacity, factory);
    }

    public <T> Future<T> submit(Callable<T> action) {
        var c = TenantScopeGuard.requireBusiness();
        requireOutsideTransaction();
        Objects.requireNonNull(action);
        var proof=TenantExecutionScope.sessionBacked()?(authority==null?missingAuthority():authority.capture(c)):null;
        var deadline = TenantExecutionScope.captureTaskDeadline(maxAgeNanos, ticker);
        // 用taskId替代调用方sessionId；只保存当前permission/range，不保存身份根的全部grants。
        var task = new TenantContext(c.tenantId(), c.principalType(), c.principalId(), UUID.randomUUID(),
                c.authorizationVersion(), c.permissionCode(), c.dataScope(), c.authorizedStoreIds(),
                c.currentStoreId(), c.traceId(), TenantPurpose.BUSINESS);
        return enqueue(new Snapshot(task, deadline,proof), action);
    }

    private static com.pet.platform.shared.security.TaskAuthority.Proof missingAuthority(){throw new com.pet.platform.shared.exception.PermissionDeniedException();}

    /** 仅供无身份技术工作；不能用于业务租户操作或从租户边界提交。 */
    public <T> Future<T> submitUnscoped(Callable<T> action) {
        if (TenantContextHolder.current().isPresent()) throw new com.pet.platform.shared.exception.TenantAccessDeniedException();
        requireOutsideTransaction();
        return enqueue(null, Objects.requireNonNull(action));
    }

    private <T> Future<T> enqueue(Snapshot snapshot, Callable<T> action) {
        var future = new FutureTask<T>(() -> run(snapshot, action)) {
            @Override protected void done() { if (isCancelled()) pool.remove(this); }
        };
        pool.execute(future); // AbortPolicy抛明确拒绝；不会在调用线程回退执行。
        return future;
    }

    private <T> T run(Snapshot snapshot, Callable<T> action) throws Exception {
        try { return runBoundary(snapshot, action); }
        catch (Exception | Error failure) {
            // cancel后的Future无法再承载执行异常；固定日志仍记录失败，不输出异常消息/堆栈。
            LOG.warn("受控任务执行失败，任务={}，异常类型={}", snapshot == null ? "无身份技术任务" : snapshot.context().sessionId(), failure.getClass().getSimpleName());
            throw failure;
        }
    }

    private <T> T runBoundary(Snapshot snapshot, Callable<T> action) throws Exception {
        requireCleanWorker();
        if (snapshot != null) snapshot.deadline().verify();
        try (var trace = TraceScope.open(snapshot == null ? null : snapshot.context().traceId())) {
            var validated=snapshot==null?null:snapshot.proof()==null?snapshot.context():authority.revalidate(snapshot.context(),snapshot.proof());
            var boundary = snapshot == null ? null : TenantExecutionScope.openTask(validated, snapshot.deadline(),snapshot.proof()!=null);
            try {
                T result = action.call();
                if (snapshot != null) snapshot.deadline().verify();
                return result;
            }
            finally { if (boundary != null) boundary.finishBoundary(); }
        } finally { requireCleanWorker(); }
    }

    private void requireCleanWorker() {
        boolean tracePresent;
        try { TraceContext.currentId(); tracePresent = true; } catch (IllegalStateException absent) { tracePresent = false; }
        if (TenantExecutionScope.hasWorkerContext() || tracePresent
                || java.util.List.of("tenantId", "operatorId", "storeId", "traceId").stream().anyMatch(k -> MDC.get(k) != null)
                || TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.getResourceMap().isEmpty()) {
            quarantined.set(true);
            LOG.error("受控任务检测到工作线程状态残留，拒绝并废弃该线程");
            throw new IllegalStateException("受控任务工作线程存在状态残留");
        }
    }

    private static void requireOutsideTransaction() {
        if (TransactionSynchronizationManager.isActualTransactionActive() || TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("租户异步任务须在调用方应用事务完成后提交");
        }
    }

    private final class WorkerPool extends ThreadPoolExecutor {
        WorkerPool(int threads, int capacity, ThreadFactory factory) {
            super(threads, threads, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(capacity), factory, new AbortPolicy());
        }
        @Override protected void afterExecute(Runnable task, Throwable error) {
            super.afterExecute(task, error);
            if (Boolean.TRUE.equals(quarantined.get())) {
                quarantined.remove();
                // Future已记录失败；当前实际执行线程退出，不在其他线程强行清理身份/事务。
                throw new IllegalStateException("废弃存在状态残留的受控工作线程");
            }
        }
    }

    /** 中止排队任务并请求运行线程中断；运行任务仍由自己的finally负责清理。 */
    public void stopNow() {
        for (Runnable queued : pool.shutdownNow()) if (queued instanceof Future<?> future) future.cancel(false);
    }
    @Override public void close() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(shutdownWait.toNanos(), TimeUnit.NANOSECONDS)) {
                stopNow();
                if (!pool.awaitTermination(shutdownWait.toNanos(), TimeUnit.NANOSECONDS)) {
                    throw new IllegalStateException("受控任务尚未协作退出，执行上下文仍由运行线程负责清理");
                }
            }
        } catch (InterruptedException failure) {
            stopNow(); Thread.currentThread().interrupt();
            throw new IllegalStateException("等待受控任务停止被中断");
        }
    }
}
