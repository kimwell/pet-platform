package com.pet.platform.shared.tenancy;

import com.pet.platform.shared.exception.*;
import com.pet.platform.shared.observability.*;
import com.pet.platform.shared.security.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;

/** 技术身份夹具；不安装测试登录，不用手工清理Holder掩盖执行器缺陷。 */
class TenantTaskExecutorTest {
    static final UUID A = UUID.randomUUID(), B = UUID.randomUUID(), OWNER = UUID.randomUUID(), STORE = UUID.randomUUID();
    static final String PERMISSION = "probe:task:execute", OTHER = "probe:task:other";
    static final String TRACE = "a".repeat(32);
    static CurrentPrincipal principal(UUID tenant, PrincipalType domain, Set<DataScopeType> types, Set<UUID> stores) {
        var s = new DataScope(tenant, domain, OWNER, types, stores);
        return new CurrentPrincipal(domain, OWNER, tenant, UUID.randomUUID(), 0, Set.of(PERMISSION, OTHER),
                Set.of(STORE), Map.of(PERMISSION, s, OTHER, s));
    }
    static CurrentPrincipal tenant(UUID id) { return principal(id, PrincipalType.STAFF, Set.of(DataScopeType.TENANT), Set.of()); }
    static <T> T scope(CurrentPrincipal p, Supplier<T> action) {
        try { return new TrustedTenantExecutor(() -> Optional.of(p)).execute(PERMISSION, action); }
        finally { assertClean(); }
    }
    static void assertClean() {
        assertTrue(TenantContextHolder.current().isEmpty());
        for (String k : List.of("tenantId", "operatorId", "storeId")) assertNull(MDC.get(k));
    }
    static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(5, TimeUnit.SECONDS)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }
    static <T> T result(Future<T> f) throws Exception { return f.get(5, TimeUnit.SECONDS); }
    static TenantTaskExecutor pool(int threads, int queue) {
        return new TenantTaskExecutor(threads, queue, Duration.ofSeconds(30), Duration.ofSeconds(2));
    }
    static long cleanProbe(TenantTaskExecutor p) throws Exception {
        return result(p.submitUnscoped(() -> {
            assertClean(); assertNull(MDC.get("traceId")); assertThrows(IllegalStateException.class, TraceContext::currentId);
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
            return Thread.currentThread().threadId();
        }));
    }
    @Test void sameWorkerRunsAThenBThenAnonymousWithoutResidue() throws Exception {
        try (var p = pool(1, 4)) {
            long worker = cleanProbe(p);
            for (UUID id : List.of(A, B, A, B)) {
                long observed = result(scope(tenant(id), () -> p.submit(() -> {
                    assertEquals(id, TenantScopeGuard.requireBusiness().tenantId());
                    assertEquals(id.toString(), MDC.get("tenantId"));
                    return Thread.currentThread().threadId();
                })));
                assertEquals(worker, observed); assertEquals(worker, cleanProbe(p));
            }
        }
    }
    @Test void concurrentTasksKeepIndependentIdentityAndTrace() throws Exception {
        try (var p = pool(2, 2)) {
            var barrier = new CyclicBarrier(2);
            var futures = new ArrayList<Future<UUID>>();
            for (UUID id : List.of(A, B)) futures.add(scope(tenant(id), () -> p.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                assertEquals(id.toString(), MDC.get("tenantId")); return TenantContextHolder.required().tenantId();
            })));
            assertEquals(A, result(futures.get(0))); assertEquals(B, result(futures.get(1)));
            cleanProbe(p);
        }
    }
    @Test void workerCreationDoesNotInheritThirdPartyThreadLocals() throws Exception {
        var thirdParty = new InheritableThreadLocal<String>(); thirdParty.set("仅调用线程技术夹具");
        try (var p = pool(1, 2)) {
            assertNull(result(scope(tenant(A), () -> p.submit(thirdParty::get))));
            assertEquals("仅调用线程技术夹具",thirdParty.get()); cleanProbe(p);
        } finally { thirdParty.remove(); }
    }
    @Test void immutableSnapshotSurvivesCallerBoundaryAndExcludesSessionAndOtherGrants() throws Exception {
        try (var p = pool(1, 2)) {
            var started = new CountDownLatch(1); var released = new CountDownLatch(1);
            var caller = tenant(A);
            var f = scope(caller, () -> p.submit(() -> {
                started.countDown(); await(released);
                var c = TenantScopeGuard.requireBusiness(); assertEquals(A, c.tenantId());
                assertNotEquals(caller.sessionId(), c.sessionId());
                assertThrows(UnsupportedOperationException.class, () -> c.authorizedStoreIds().clear());
                assertThrows(PermissionDeniedException.class, () -> TenantExecutionScope.forPermission(OTHER));
                return c.dataScope();
            }));
            try { await(started); assertClean(); } finally { released.countDown(); }
            assertEquals(caller.grants().get(PERMISSION), result(f)); cleanProbe(p);
        }
    }
    @Test void traceWhitelistDoesNotCopyOtherMdcOrDamageCaller() throws Exception {
        try (var p = pool(1, 2); var trace = TraceScope.open(TRACE)) {
            MDC.put("otherComponent", "仅调用线程");
            try {
                var f = scope(tenant(A), () -> p.submit(() -> {
                    assertEquals(TRACE, TraceContext.currentId()); assertEquals(TRACE, MDC.get("traceId"));
                    assertNull(MDC.get("otherComponent")); return true;
                }));
                assertTrue(result(f)); assertEquals(TRACE, TraceContext.currentId());
                assertEquals("仅调用线程", MDC.get("otherComponent")); cleanProbe(p);
            } finally { MDC.remove("otherComponent"); }
        }
        assertNull(MDC.get("traceId"));
    }
    @Test void exceptionIsObservableAndFollowingTaskIsClean() throws Exception {
        try (var p = pool(1, 2)) {
            long worker = cleanProbe(p);
            var f = scope(tenant(A), () -> p.submit(() -> { throw new IllegalArgumentException("任务异常夹具"); }));
            assertInstanceOf(IllegalArgumentException.class, assertThrows(ExecutionException.class, () -> result(f)).getCause());
            assertEquals(worker, cleanProbe(p));
        }
    }
    @Test void leakedNestedScopeIsRejectedAndCleanedByActualWorker() throws Exception {
        try (var p = pool(1, 2)) {
            long worker = cleanProbe(p);
            var f = scope(tenant(A), () -> p.submit(() -> { TenantExecutionScope.forPermission(PERMISSION); return true; }));
            assertInstanceOf(IllegalStateException.class, assertThrows(ExecutionException.class, () -> result(f)).getCause());
            assertEquals(worker, cleanProbe(p));
        }
    }
    @Test void emptyStoresRemainEmptyAndCannotRecoverTenantOrSwitchPermission() throws Exception {
        try (var p = pool(1, 2)) {
            var empty = principal(A, PrincipalType.STAFF, Set.of(DataScopeType.STORES), Set.of());
            assertTrue(result(scope(empty, () -> p.submit(() -> {
                assertTrue(TenantScopeGuard.requireBusiness().dataScope().storeIds().isEmpty());
                assertThrows(TenantAccessDeniedException.class, () -> TenantExecutionScope.narrow(tenant(A).grants().get(PERMISSION)));
                assertThrows(TenantAccessDeniedException.class, () -> new StoreScopeGuard(id -> Optional.of(A)).requireStore(STORE));
                assertThrows(PermissionDeniedException.class, () -> TenantExecutionScope.forPermission(OTHER)); return true;
            })))); cleanProbe(p);
        }
    }
    @Test void selfRetainsSubjectDomainAndOwnerAndRejectsExpansion() throws Exception {
        try (var p = pool(1, 2)) {
            for (PrincipalType type : List.of(PrincipalType.STAFF, PrincipalType.CUSTOMER)) {
                var self = principal(A, type, Set.of(DataScopeType.SELF), Set.of());
                assertEquals(type, result(scope(self, () -> p.submit(() -> {
                    var c = TenantScopeGuard.requireBusiness(); assertEquals(OWNER, c.dataScope().principalId());
                    assertEquals(Set.of(DataScopeType.SELF), c.dataScope().types());
                    assertThrows(TenantAccessDeniedException.class, () -> TenantExecutionScope.narrow(tenant(A).grants().get(PERMISSION)));
                    return c.principalType();
                }))));
            }
            cleanProbe(p);
        }
    }
    @Test void selectedStoreAndNarrowedScopeAreCaptured() throws Exception {
        try (var p = pool(1, 2)) {
            var f = scope(tenant(A), () -> {
                try (var store = new StoreScopeGuard(id -> Optional.of(A)).openStore(STORE);
                     var narrow = TenantExecutionScope.narrow(principal(A, PrincipalType.STAFF, Set.of(DataScopeType.STORES), Set.of(STORE)).grants().get(PERMISSION))) {
                    return p.submit(() -> { assertEquals(STORE, TenantContextHolder.required().currentStoreId());
                        assertEquals(STORE.toString(), MDC.get("storeId")); return TenantContextHolder.required().dataScope().types(); });
                }
            });
            assertEquals(Set.of(DataScopeType.STORES), result(f)); cleanProbe(p);
        }
    }
    @Test void noIdentityCannotSubmitAndTenantCannotSubmitUnscoped() throws Exception {
        try (var p = pool(1, 1)) {
            assertThrows(TenantAccessDeniedException.class, () -> p.submit(() -> true));
            scope(tenant(A), () -> { assertThrows(TenantAccessDeniedException.class, () -> p.submitUnscoped(() -> true)); return true; });
            cleanProbe(p);
        }
    }
    @Test void fullQueueRejectsWithoutCallerExecutionOrMdcPollution() throws Exception {
        try (var p = pool(1, 1); var trace = TraceScope.open(TRACE)) {
            var started = new CountDownLatch(1); var release = new CountDownLatch(1); var invoked = new AtomicBoolean();
            var running = scope(tenant(A), () -> p.submit(() -> { started.countDown(); await(release); return true; }));
            await(started);
            try {
                var queued = scope(tenant(B), () -> p.submit(() -> true));
                scope(tenant(A), () -> {
                    assertThrows(RejectedExecutionException.class, () -> p.submit(() -> { invoked.set(true); return true; }));
                    assertEquals(A, TenantContextHolder.required().tenantId()); assertEquals(TRACE, TraceContext.currentId()); return true;
                });
                assertFalse(invoked.get()); queued.cancel(false);
            } finally { release.countDown(); }
            assertTrue(result(running)); cleanProbe(p); assertEquals(TRACE, MDC.get("traceId"));
        }
    }
    @Test void queuedCancellationDoesNotInstallIdentityAndReleasesCapacity() throws Exception {
        try (var p = pool(1, 1)) {
            var started = new CountDownLatch(1); var release = new CountDownLatch(1); var invoked = new AtomicBoolean();
            var running = scope(tenant(A), () -> p.submit(() -> { started.countDown(); await(release); return true; })); await(started);
            Future<Boolean> next;
            try {
                var cancelled = scope(tenant(B), () -> p.submit(() -> { invoked.set(true); return true; }));
                assertTrue(cancelled.cancel(false)); assertThrows(CancellationException.class, () -> result(cancelled));
                next = scope(tenant(A), () -> p.submit(() -> TenantContextHolder.required().tenantId().equals(A)));
            } finally { release.countDown(); }
            assertTrue(result(running)); assertTrue(result(next)); assertFalse(invoked.get()); cleanProbe(p);
        }
    }
    @Test void runningCancellationRetainsScopeUntilActualCooperativeExit() throws Exception {
        try (var p = pool(1, 2)) {
            var started = new CountDownLatch(1); var interrupted = new CountDownLatch(1); var release = new CountDownLatch(1); var exited = new CountDownLatch(1);
            var f = scope(tenant(A), () -> p.submit(() -> {
                try {
                    started.countDown();
                    try { new CountDownLatch(1).await(); } catch (InterruptedException expected) { interrupted.countDown(); }
                    assertEquals(A, TenantScopeGuard.requireBusiness().tenantId());
                    await(release); return true;
                } finally { exited.countDown(); }
            }));
            try {
                await(started); assertTrue(f.cancel(true)); await(interrupted);
                assertEquals(1, exited.getCount()); assertTrue(f.isDone()); assertThrows(CancellationException.class, () -> result(f));
                var queuedProbe = p.submitUnscoped(() -> { assertClean(); return true; }); assertFalse(queuedProbe.isDone());
                release.countDown(); assertTrue(result(queuedProbe)); await(exited);
            } finally { release.countDown(); }
            cleanProbe(p);
        }
    }
    @Test void stopNowCancelsQueueAndInterruptsRunningTaskWithoutCallerPollution() throws Exception {
        var p = pool(1, 2);
        var started = new CountDownLatch(1); var exited = new CountDownLatch(1);
        var running = scope(tenant(A), () -> p.submit(() -> {
            started.countDown();
            try { new CountDownLatch(1).await(); return false; }
            catch (InterruptedException expected) { assertEquals(A, TenantContextHolder.required().tenantId()); return true; }
            finally { exited.countDown(); }
        }));
        await(started);
        var queued = scope(tenant(B), () -> p.submit(() -> true));
        p.stopNow(); assertThrows(CancellationException.class, () -> result(queued));
        assertTrue(result(running)); await(exited); p.close(); assertClean();
        scope(tenant(A), () -> { assertThrows(RejectedExecutionException.class, () -> p.submit(() -> true)); return true; });
    }
    @Test void gracefulCloseDrainsQueuedTasksAndRejectsFurtherSubmissions() throws Exception {
        var p = pool(1, 2);
        var f = scope(tenant(A), () -> p.submit(() -> true)); var g = scope(tenant(B), () -> p.submit(() -> true));
        p.close(); assertTrue(result(f)); assertTrue(result(g));
        assertThrows(RejectedExecutionException.class, () -> p.submitUnscoped(() -> true)); assertClean();
    }
    @Test void queuedSnapshotExpiresDeterministicallyBeforeExecution() throws Exception {
        var time = new AtomicLong();
        try (var p = new TenantTaskExecutor(1, 2, Duration.ofSeconds(1), Duration.ofSeconds(2), time::get)) {
            var started = new CountDownLatch(1); var release = new CountDownLatch(1); var invoked = new AtomicBoolean();
            var blocker = p.submitUnscoped(() -> { started.countDown(); await(release); return true; }); await(started);
            var f = scope(tenant(A), () -> p.submit(() -> { invoked.set(true); return true; }));
            time.set(TimeUnit.SECONDS.toNanos(2)); release.countDown(); assertTrue(result(blocker));
            assertInstanceOf(RejectedExecutionException.class, assertThrows(ExecutionException.class, () -> result(f)).getCause());
            assertFalse(invoked.get()); cleanProbe(p);
        }
    }
    @Test void runningExpiryBlocksGuardsAndNestedSubmissionCannotRenewSnapshot() throws Exception {
        var time = new AtomicLong();
        try (var p = new TenantTaskExecutor(1, 3, Duration.ofSeconds(1), Duration.ofSeconds(2), time::get)) {
            var nested = new AtomicReference<Future<Boolean>>();
            var f = scope(tenant(A), () -> p.submit(() -> {
                time.set(TimeUnit.MILLISECONDS.toNanos(900));
                nested.set(p.submit(() -> true));
                time.set(TimeUnit.MILLISECONDS.toNanos(1100));
                assertThrows(RejectedExecutionException.class, TenantScopeGuard::requireBusiness);
                assertThrows(RejectedExecutionException.class, () -> TenantExecutionScope.forPermission(PERMISSION)); return true;
            }));
            assertInstanceOf(RejectedExecutionException.class, assertThrows(ExecutionException.class, () -> result(f)).getCause());
            assertInstanceOf(RejectedExecutionException.class, assertThrows(ExecutionException.class, () -> result(nested.get())).getCause()); cleanProbe(p);
        }
    }
    @Test void activeTransactionOrSynchronizationRejectsSubmission() throws Exception {
        try (var p = pool(1, 2)) {
            TransactionSynchronizationManager.setActualTransactionActive(true);
            try { scope(tenant(A), () -> { assertThrows(IllegalStateException.class, () -> p.submit(() -> true)); return true; }); }
            finally { TransactionSynchronizationManager.setActualTransactionActive(false); }
            TransactionSynchronizationManager.initSynchronization();
            try { assertThrows(IllegalStateException.class, () -> p.submitUnscoped(() -> true)); }
            finally { TransactionSynchronizationManager.clearSynchronization(); }
            cleanProbe(p);
        }
    }
    @Test void residualWorkerIdentityIsRejectedAndThreadIsReplaced() throws Exception {
        try (var p = pool(1, 3)) {
            long previous = cleanProbe(p);
            var f = p.submitUnscoped(() -> { TenantExecutionScope.openIdentity(tenant(A)); return true; });
            assertInstanceOf(IllegalStateException.class, assertThrows(ExecutionException.class, () -> result(f)).getCause());
            assertNotEquals(previous, cleanProbe(p));
            assertEquals(B, result(scope(tenant(B), () -> p.submit(() -> TenantContextHolder.required().tenantId()))));
        }
    }
    @Test void resourceBoundsAndNonForgedSnapshotConstructors() {
        for (Duration age : List.of(Duration.ZERO, Duration.ofSeconds(61), Duration.ofSeconds(-1))) {
            assertThrows(IllegalArgumentException.class, () -> new TenantTaskExecutor(1, 1, age, Duration.ofSeconds(1)));
        }
        assertThrows(IllegalArgumentException.class, () -> pool(0, 1)); assertThrows(IllegalArgumentException.class, () -> pool(17, 1));
        assertThrows(IllegalArgumentException.class, () -> pool(1, 0)); assertThrows(IllegalArgumentException.class, () -> pool(1, 1025));
        for (var nested : TenantTaskExecutor.class.getDeclaredClasses()) if (nested.getSimpleName().equals("Snapshot")) {
            assertTrue(java.lang.reflect.Modifier.isPrivate(nested.getModifiers()));
            for (var constructor : nested.getDeclaredConstructors()) assertFalse(java.lang.reflect.Modifier.isPublic(constructor.getModifiers()));
        }
    }
    @Test void preexistingWorkerResidueRejectsNextTaskWithoutRunningIt() throws Exception {
        try (var p = pool(1, 3)) {
            long previous = cleanProbe(p);
            var field = TenantTaskExecutor.class.getDeclaredField("pool"); field.setAccessible(true);
            var raw = (ThreadPoolExecutor) field.get(p);
            var installed = new CountDownLatch(1);
            // 故意污染仅测试可见的内部池，验证任务进入前的拒绝策略；测试不清理Holder。
            raw.execute(() -> { TenantExecutionScope.openIdentity(tenant(A)); installed.countDown(); }); await(installed);
            var invoked = new AtomicBoolean();
            var rejected = scope(tenant(B), () -> p.submit(() -> { invoked.set(true); return true; }));
            assertInstanceOf(IllegalStateException.class, assertThrows(ExecutionException.class, () -> result(rejected)).getCause());
            assertFalse(invoked.get()); assertNotEquals(previous, cleanProbe(p));
        }
    }
    @Test void closeReportsNonCooperativeRunningTaskWithoutPrematureContextCleanup() throws Exception {
        var p = new TenantTaskExecutor(1, 2, Duration.ofSeconds(30), Duration.ofMillis(10));
        var started = new CountDownLatch(1); var release = new CountDownLatch(1); var interrupted = new CountDownLatch(1); var observed = new AtomicBoolean();
        var f = scope(tenant(A), () -> p.submit(() -> {
            started.countDown();
            while (release.getCount() > 0) {
                try { release.await(); } catch (InterruptedException request) { observed.set(TenantScopeGuard.requireBusiness().tenantId().equals(A)); interrupted.countDown(); }
            }
            return true;
        }));
        try {
            await(started); assertThrows(IllegalStateException.class, p::close);
            assertFalse(f.isDone()); await(interrupted); assertTrue(observed.get()); assertClean();
        } finally { release.countDown(); }
        assertTrue(result(f)); p.close();
    }
}
