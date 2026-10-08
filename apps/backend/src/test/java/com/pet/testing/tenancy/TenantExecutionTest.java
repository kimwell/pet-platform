package com.pet.testing.tenancy;

import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.*;
import com.pet.platform.shared.security.*;
import com.pet.platform.shared.tenancy.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.servlet.HandlerExceptionResolver;
import static com.pet.testing.tenancy.TenancyFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class TenantExecutionTest {
    <T> T execute(CurrentPrincipal principal, String permission, java.util.function.Supplier<T> action) {
        try { return new TrustedTenantExecutor(() -> Optional.of(principal)).execute(permission, action); }
        finally { assertClean(); }
    }
    static void assertClean() {
        assertTrue(TenantContextHolder.current().isEmpty());
        for (String key : List.of("tenantId", "operatorId", "storeId")) assertNull(MDC.get(key), key);
    }
    @Test void defaultProviderIsAnonymousAndDefaultStoreReaderIsUnavailable() {
        try (var spring = new AnnotationConfigApplicationContext()) {
            spring.registerBean("handlerExceptionResolver", HandlerExceptionResolver.class, () -> (req, res, handler, exception) -> null);
            spring.register(TenancyConfiguration.class); spring.refresh();
            assertTrue(spring.getBean(CurrentPrincipalProvider.class).currentPrincipal().isEmpty());
            assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE, assertThrows(BusinessException.class,
                () -> spring.getBean(StoreOwnershipReader.class).findTenantId(STORE)).error().code());
            execute(staff(A), WRITE, () -> {
                assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE, assertThrows(BusinessException.class,
                    () -> spring.getBean(StoreScopeGuard.class).requireStore(STORE)).error().code()); return null;
            });
            assertEquals(ErrorCode.AUTH_REQUIRED, assertThrows(BusinessException.class,
                () -> spring.getBean(TrustedTenantExecutor.class).execute(READ, () -> true)).error().code());
        }
        assertClean();
    }
    @Test void noContextAlwaysRejectsTenantOperations() {
        assertThrows(TenantAccessDeniedException.class, TenantContextHolder::required);
        assertThrows(TenantAccessDeniedException.class, TenantScopeGuard::requireBusiness);
        assertThrows(TenantAccessDeniedException.class, () -> TenantExecutionScope.forPermission(READ));
    }
    @Test void staffAndCustomerAreDistinctTrustedTenantSubjects() {
        for (var p : List.of(staff(A), customer())) execute(p, READ, () -> {
            var c = TenantScopeGuard.requireTenant(A);
            assertEquals(p.principalType(), c.principalType()); assertEquals(OPERATOR, c.principalId());
            assertEquals(SESSION, c.sessionId()); assertEquals(p.authorizationVersion(), c.authorizationVersion()); return null;
        });
    }
    @Test void platformOnlyUsesExplicitControlPlaneGuard() {
        assertThrows(TenantAccessDeniedException.class, () -> execute(platform(), READ, () -> true));
        assertEquals(platform(), new PlatformScopeGuard(() -> Optional.of(platform())).requirePermission(READ));
        assertThrows(BusinessException.class, () -> new PlatformScopeGuard(() -> Optional.of(staff(A))).requirePermission(READ));
    }
    @Test void identityAndScopeDefensivelyCopyCollections() {
        var stores = new HashSet<>(Set.of(STORE)); var types = new HashSet<>(Set.of(DataScopeType.STORES));
        var s = scope(A, PrincipalType.STAFF, types, stores); stores.clear(); types.clear();
        assertEquals(Set.of(STORE), s.storeIds()); assertEquals(Set.of(DataScopeType.STORES), s.types());
        var codes = new HashSet<>(Set.of(WRITE)); var grants = new HashMap<>(Map.of(WRITE, s)); var upper = new HashSet<>(Set.of(STORE));
        var p = new CurrentPrincipal(PrincipalType.STAFF, OPERATOR, A, SESSION, 0, codes, upper, grants);
        codes.clear(); grants.clear(); upper.clear(); assertEquals(Set.of(WRITE), p.permissionCodes()); assertEquals(s, p.grants().get(WRITE));
        assertThrows(UnsupportedOperationException.class, () -> p.authorizedStoreIds().add(OTHER_STORE));
    }
    @Test void invalidPrincipalAndScopeCombinationsAreRejected() {
        assertThrows(NullPointerException.class, () -> new CurrentPrincipal(PrincipalType.STAFF, OPERATOR, null, SESSION, 0, Set.of(), Set.of(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new CurrentPrincipal(PrincipalType.PLATFORM, OPERATOR, A, SESSION, 0, Set.of(), Set.of(), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> scope(A, PrincipalType.PLATFORM, Set.of(DataScopeType.TENANT), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> scope(A, PrincipalType.STAFF, Set.of(), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> scope(A, PrincipalType.STAFF, Set.of(DataScopeType.SELF), Set.of(STORE)));
        assertThrows(IllegalArgumentException.class, () -> new CurrentPrincipal(PrincipalType.CUSTOMER, OPERATOR, A, SESSION, 0, Set.of(READ), Set.of(), Map.of(READ, scope(A, PrincipalType.CUSTOMER, Set.of(DataScopeType.TENANT), Set.of()))));
        assertThrows(IllegalArgumentException.class, () -> new CurrentPrincipal(PrincipalType.STAFF, OPERATOR, A, SESSION, 0, Set.of(WRITE), Set.of(), Map.of(WRITE, staff(A).grants().get(WRITE))));
    }
    @Test void nestedSameTenantRestoresOuterAndNeverWidens() {
        execute(staff(A), READ, () -> {
            var outer = TenantContextHolder.required();
            var limited = scope(A, PrincipalType.STAFF, Set.of(DataScopeType.STORES), Set.of(STORE));
            try (var inner = TenantExecutionScope.narrow(limited)) {
                assertEquals(limited, TenantScopeGuard.requireBusiness().dataScope());
                try (var same = TenantExecutionScope.forPermission(READ)) { assertEquals(limited, TenantScopeGuard.requireBusiness().dataScope()); }
            }
            assertSame(outer, TenantContextHolder.required()); return null;
        });
    }
    @Test void crossTenantSubjectAndScopeExpansionAreRejected() {
        execute(staff(A), WRITE, () -> {
            for (var s : List.of(scope(B, PrincipalType.STAFF, Set.of(DataScopeType.STORES), Set.of(STORE)),
                scope(A, PrincipalType.CUSTOMER, Set.of(DataScopeType.SELF), Set.of()),
                scope(A, PrincipalType.STAFF, Set.of(DataScopeType.TENANT), Set.of()),
                scope(A, PrincipalType.STAFF, Set.of(DataScopeType.STORES), Set.of(STORE, OTHER_STORE)))) {
                assertThrows(TenantAccessDeniedException.class, () -> TenantExecutionScope.narrow(s));
            }
            assertThrows(PermissionDeniedException.class, () -> TenantExecutionScope.forPermission(READ));
            assertThrows(PermissionDeniedException.class, () -> TenantScopeGuard.requirePermission(READ));
            assertEquals(WRITE, TenantScopeGuard.requirePermission(WRITE).permissionCode());
            assertThrows(TenantAccessDeniedException.class, () -> new TrustedTenantExecutor(() -> Optional.of(staff(B))).execute(READ, () -> true));
            assertThrows(TenantAccessDeniedException.class, () -> TenantScopeGuard.requireTenant(B)); return null;
        });
    }
    @Test void outOfOrderCloseRejectsWithoutChangingContextAndIsRecoverable() {
        execute(staff(A), WRITE, () -> {
            var outer = TenantExecutionScope.forPermission(WRITE); var inner = TenantExecutionScope.forPermission(WRITE);
            var c = TenantContextHolder.required(); assertThrows(IllegalStateException.class, outer::close); assertSame(c, TenantContextHolder.required());
            inner.close(); outer.close(); outer.close(); return null;
        });
    }
    @Test void closingOnOtherThreadRejectsAndOwnerCanStillClose() throws Exception {
        execute(staff(A), WRITE, () -> {
            var inner = TenantExecutionScope.forPermission(WRITE);
            try (var pool = Executors.newSingleThreadExecutor()) {
                assertTrue(pool.submit(() -> { assertClean(); assertThrows(IllegalStateException.class, inner::close); return true; }).get());
            } catch (Exception e) { throw new AssertionError(e); }
            assertEquals(A, TenantContextHolder.required().tenantId()); inner.close(); return null;
        });
    }
    @Test void exceptionAndLeakedInnerScopesBothCleanAtTrustedBoundary() {
        assertThrows(IllegalStateException.class, () -> execute(staff(A), READ, () -> { throw new IllegalStateException("技术异常"); }));
        assertThrows(IllegalStateException.class, () -> execute(staff(A), READ, () -> { TenantExecutionScope.forPermission(READ); return null; }));
        assertClean();
    }
    @Test void mdcOnlyRestoresOwnedFieldsAndStoreMustBeExplicit() {
        MDC.put("traceId", "outer-trace"); MDC.put("otherComponent", "retained");
        MDC.put("tenantId", "previous-tenant"); MDC.put("operatorId", "previous-operator"); MDC.put("storeId", "previous-store");
        try {
            new TrustedTenantExecutor(() -> Optional.of(staff(A))).execute(WRITE, () -> {
                assertEquals(A.toString(), MDC.get("tenantId")); assertEquals(OPERATOR.toString(), MDC.get("operatorId")); assertNull(MDC.get("storeId"));
                try (var store = new StoreScopeGuard(id -> Optional.of(A)).openStore(STORE)) { assertEquals(STORE.toString(), MDC.get("storeId")); }
                assertNull(MDC.get("storeId")); assertEquals("outer-trace", MDC.get("traceId")); return null;
            });
            assertTrue(TenantContextHolder.current().isEmpty()); assertEquals("previous-tenant", MDC.get("tenantId"));
            assertEquals("previous-operator", MDC.get("operatorId")); assertEquals("previous-store", MDC.get("storeId")); assertEquals("retained", MDC.get("otherComponent"));
        } finally { for (String key : List.of("traceId", "otherComponent", "tenantId", "operatorId", "storeId")) MDC.remove(key); }
    }
    @Test void concurrentThreadsDoNotShareAndChildThreadDoesNotInherit() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var barrier = new CyclicBarrier(2);
            var futures = new ArrayList<Future<UUID>>();
            for (UUID tenant : List.of(A, B)) futures.add(pool.submit(() -> execute(staff(tenant), READ, () -> {
                try { barrier.await(5, TimeUnit.SECONDS); } catch (Exception e) { throw new AssertionError(e); }
                assertEquals(tenant.toString(), MDC.get("tenantId")); return TenantScopeGuard.requireBusiness().tenantId();
            })));
            assertEquals(A, futures.get(0).get()); assertEquals(B, futures.get(1).get());
        }
        assertClean();
    }
    @Test void mergesOnlySamePermissionAndIntersectsStoreUpperBound() {
        var stores = scope(A, PrincipalType.STAFF, Set.of(DataScopeType.STORES), Set.of(STORE, OTHER_STORE));
        var self = scope(A, PrincipalType.STAFF, Set.of(DataScopeType.SELF), Set.of());
        var merged = ScopeGrant.mergeForPermission(WRITE, List.of(new ScopeGrant(WRITE, stores), new ScopeGrant(WRITE, self)), Set.of(STORE)).dataScope();
        assertEquals(Set.of(DataScopeType.STORES, DataScopeType.SELF), merged.types()); assertEquals(Set.of(STORE), merged.storeIds());
        assertThrows(IllegalArgumentException.class, () -> ScopeGrant.mergeForPermission(WRITE, List.of(new ScopeGrant(READ, stores)), Set.of(STORE)));
        assertThrows(IllegalArgumentException.class, () -> ScopeGrant.mergeForPermission(WRITE, List.of(new ScopeGrant(WRITE, stores), new ScopeGrant(WRITE, scope(B, PrincipalType.STAFF, Set.of(DataScopeType.SELF), Set.of()))), Set.of(STORE)));
        var tenant = ScopeGrant.mergeForPermission(WRITE, List.of(new ScopeGrant(WRITE, stores), new ScopeGrant(WRITE, staff(A).grants().get(READ))), Set.of(STORE));
        assertEquals(Set.of(DataScopeType.TENANT), tenant.dataScope().types()); assertEquals(Set.of(), tenant.dataScope().storeIds());
    }
    @Test void absentOperationPermissionRejectsIndependentlyOfTenantRange() {
        assertThrows(PermissionDeniedException.class, () -> execute(staff(A), "probe:item:delete", () -> true));
    }
    @Test void storeOwnershipAndPermissionAreBothRequired() {
        execute(staff(A), WRITE, () -> {
            var guard = new StoreScopeGuard(id -> id.equals(STORE) ? Optional.of(A) : Optional.of(B));
            guard.requireStore(STORE);
            assertThrows(TenantAccessDeniedException.class, () -> guard.requireStore(OTHER_STORE));
            assertThrows(TenantAccessDeniedException.class, () -> new StoreScopeGuard(id -> Optional.empty()).requireStore(STORE));
            assertThrows(TenantAccessDeniedException.class, () -> new StoreScopeGuard(id -> Optional.of(A)).requireStore(OTHER_STORE));
            assertThrows(TenantAccessDeniedException.class, () -> guard.requireStore(null)); return null;
        });
    }
    @Test void currentStoreCanOnlyBeBoundAfterOwnershipAndCannotSwitchNested() {
        execute(staff(A), READ, () -> {
            var guard = new StoreScopeGuard(id -> Optional.of(A));
            try (var scope = guard.openStore(STORE)) {
                assertEquals(STORE, TenantContextHolder.required().currentStoreId());
                assertThrows(TenantAccessDeniedException.class, () -> guard.openStore(OTHER_STORE));
            }
            assertNull(TenantContextHolder.required().currentStoreId()); return null;
        });
    }
    @Test void emptyStoresAndSelfNeverAuthorizeAWholeStore() {
        execute(staff(A), WRITE, () -> {
            try (var scope = TenantExecutionScope.narrow(scope(A, PrincipalType.STAFF, Set.of(DataScopeType.STORES), Set.of()))) {
                assertThrows(TenantAccessDeniedException.class, () -> new StoreScopeGuard(id -> Optional.of(A)).requireStore(STORE));
            } return null;
        });
        execute(customer(), READ, () -> { assertThrows(TenantAccessDeniedException.class, () -> new StoreScopeGuard(id -> Optional.of(A)).requireStore(STORE)); return null; });
    }
    @Test void tenantScopeStillChecksOwnershipAndIdentityStoreUpperBound() {
        execute(staff(A), READ, () -> {
            new StoreScopeGuard(id -> Optional.of(A)).requireStore(OTHER_STORE);
            assertThrows(TenantAccessDeniedException.class, () -> new StoreScopeGuard(id -> Optional.of(B)).requireStore(STORE));
            assertThrows(TenantAccessDeniedException.class, () -> new StoreScopeGuard(id -> Optional.of(A)).requireStore(UUID.randomUUID())); return null;
        });
    }
}
