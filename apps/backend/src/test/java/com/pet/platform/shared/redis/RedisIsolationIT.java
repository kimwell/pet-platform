package com.pet.platform.shared.redis;

import com.pet.platform.shared.api.ErrorCode;
import com.pet.platform.shared.exception.*;
import com.pet.platform.shared.security.*;
import com.pet.platform.shared.tenancy.*;
import com.pet.testing.RedisTestSupport;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.slf4j.MDC;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/** 实际认证Redis、动态独立端口与每测命名空间；不执行KEYS/FLUSHDB或用户Redis操作。 */
class RedisIsolationIT {
    static final UUID A = UUID.randomUUID(), B = UUID.randomUUID(), OWNER = UUID.randomUUID(), A1 = UUID.randomUUID(), A2 = UUID.randomUUID(), B1 = UUID.randomUUID();
    static final String PERMISSION = "probe:resource:read";
    static LettuceConnectionFactory factory;
    final AtomicReference<CurrentPrincipal> platformIdentity = new AtomicReference<>();
    RedisKeyBuilder keys;
    TenantRedisAccess tenant;
    PlatformRedisAccess platform;
    StoreScopeGuard stores;
    RedisValueStore values;
    @BeforeAll static void connect() {
        var conf = new RedisStandaloneConfiguration(RedisTestSupport.REDIS.getHost(), RedisTestSupport.REDIS.getMappedPort(6379));
        conf.setPassword(RedisTestSupport.password());
        factory = new LettuceConnectionFactory(conf, LettuceClientConfiguration.builder().commandTimeout(Duration.ofMillis(300)).build());
        factory.afterPropertiesSet(); factory.start();
    }
    @AfterAll static void disconnect() { factory.destroy(); }
    @BeforeEach void namespace() {
        assertClean();
        stores = new StoreScopeGuard(id -> Optional.ofNullable(Map.of(A1,A,A2,A,B1,B).get(id)));
        keys = new RedisKeyBuilder("probe-" + UUID.randomUUID().toString().substring(0,8), "test", stores);
        values = new RedisValueStore(factory); tenant = new TenantRedisAccess(keys, values, stores);
        platform = new PlatformRedisAccess(keys, values, new PlatformScopeGuard(() -> Optional.ofNullable(platformIdentity.get())));
    }
    @AfterEach void clean() { assertClean(); }
    static void assertClean() {
        assertTrue(TenantContextHolder.current().isEmpty());
        for (String k : List.of("tenantId", "operatorId", "storeId")) assertNull(MDC.get(k));
    }
    CurrentPrincipal principal(UUID id, Set<DataScopeType> types, Set<UUID> storeIds) {
        var scope = new DataScope(id, PrincipalType.STAFF, OWNER, types, storeIds);
        return new CurrentPrincipal(PrincipalType.STAFF, OWNER, id, UUID.randomUUID(), 0, Set.of(PERMISSION),
                id.equals(A) ? Set.of(A1,A2) : Set.of(B1), Map.of(PERMISSION,scope));
    }
    CurrentPrincipal p(UUID id) { return principal(id, Set.of(DataScopeType.TENANT), Set.of()); }
    <T> T run(CurrentPrincipal p, Supplier<T> work) {
        try { return new TrustedTenantExecutor(() -> Optional.of(p)).execute(PERMISSION, work); }
        finally { assertClean(); }
    }
    @Test void sameBusinessKeyInTwoTenantsReadsIndependentRealValues() {
        for (UUID id : List.of(A,B)) run(p(id), () -> { tenant.write(keys.tenantResource("probe","same"), id.equals(A) ? "租户A" : "租户B", Duration.ofMinutes(1)); return true; });
        assertEquals("租户A",run(p(A), () -> tenant.read(keys.tenantResource("probe","same")).orElseThrow()));
        assertEquals("租户B",run(p(B), () -> tenant.read(keys.tenantResource("probe","same")).orElseThrow()));
    }
    @Test void sameTenantDifferentStoresHaveIndependentValues() {
        run(p(A), () -> {
            tenant.write(keys.storeResource(A1,"probe","same"),"门店A1",Duration.ofMinutes(1));
            tenant.write(keys.storeResource(A2,"probe","same"),"门店A2",Duration.ofMinutes(1));
            assertEquals("门店A1",tenant.read(keys.storeResource(A1,"probe","same")).orElseThrow());
            assertEquals("门店A2",tenant.read(keys.storeResource(A2,"probe","same")).orElseThrow()); return true;
        });
    }
    @Test void noContextRejectsConstructionAndAllOperations() {
        RedisKey key = run(p(A), () -> keys.tenantResource("probe","value"));
        assertThrows(TenantAccessDeniedException.class, () -> keys.tenantResource("probe","value"));
        assertThrows(TenantAccessDeniedException.class, () -> tenant.read(key));
        assertThrows(TenantAccessDeniedException.class, () -> tenant.write(key,"值",Duration.ofSeconds(1)));
        assertThrows(TenantAccessDeniedException.class, () -> tenant.delete(key));
    }
    @Test void foreignUnauthorizedEmptyAndSelfStoreScopesAreDenied() {
        for (var p : List.of(principal(A,Set.of(DataScopeType.STORES),Set.of(A1)), principal(A,Set.of(DataScopeType.STORES),Set.of()), principal(A,Set.of(DataScopeType.SELF),Set.of()))) {
            run(p, () -> {
                assertThrows(TenantAccessDeniedException.class, () -> keys.storeResource(A2,"probe","same"));
                assertThrows(TenantAccessDeniedException.class, () -> keys.storeResource(B1,"probe","same")); return true;
            });
        }
    }
    @Test void missingStoreFactsNeverAllowAddressOrOperation() {
        var missing = new StoreScopeGuard(id -> { throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE); });
        run(p(A), () -> {
            var key = keys.storeResource(A1,"probe","same");
            assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE, assertThrows(BusinessException.class,
                    () -> new RedisKeyBuilder("pet","test",missing).storeResource(A1,"probe","same")).error().code());
            var unavailable = new TenantRedisAccess(keys, values, missing);
            assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE, assertThrows(BusinessException.class, () -> unavailable.read(key)).error().code()); return true;
        });
    }
    @Test void storeFactsAreRecheckedWhenAddressIsUsed() {
        var fact = new AtomicReference<>(A); var guard = new StoreScopeGuard(id -> Optional.of(fact.get()));
        var builder = new RedisKeyBuilder("pet","test",guard); var access = new TenantRedisAccess(builder,values,guard);
        run(p(A), () -> { var key = builder.storeResource(A1,"probe","same"); fact.set(B);
            assertThrows(TenantAccessDeniedException.class, () -> access.write(key,"值",Duration.ofSeconds(1))); return true; });
    }
    @Test void invalidPrefixModuleLengthAndUnicodeAreRejected() {
        for (String bad : List.of("", "Pet", "a:b", "a*", "a?", "a{b}", "a".repeat(33))) {
            assertThrows(IllegalArgumentException.class, () -> new RedisKeyBuilder(bad,"test",stores));
            run(p(A), () -> { assertThrows(IllegalArgumentException.class, () -> keys.tenantResource(bad,"value")); return true; });
        }
        run(p(A), () -> {
            for (String bad : List.of("", "a".repeat(129), "\n", "\uD800")) assertThrows(IllegalArgumentException.class, () -> keys.tenantResource("probe",bad));
            assertThrows(IllegalArgumentException.class, () -> keys.tenantResource("probe",null)); return true;
        });
    }
    @Test void delimiterWildcardAndUnicodeEncodingIsRepeatableAndCollisionFree() {
        run(p(A), () -> {
            var originals = List.of("a:b", "a-b", "a_b", "a*b", "a?b", "a{b}", "汉字", "é", "e\u0301");
            var encoded = new HashSet<String>();
            for (String original : originals) {
                var key = keys.tenantResource("probe",original);
                assertTrue(key.encoded.matches("[A-Za-z0-9:_-]+")); assertFalse(key.encoded.contains("{"));
                assertEquals(key.encoded,keys.tenantResource("probe",original).encoded);
                String part = key.encoded.substring(key.encoded.lastIndexOf(':')+2);
                assertEquals(original,new String(Base64.getUrlDecoder().decode(part),java.nio.charset.StandardCharsets.UTF_8));
                tenant.write(key,original,Duration.ofSeconds(2)); assertEquals(original,tenant.read(key).orElseThrow());
                encoded.add(key.encoded);
            }
            assertEquals(originals.size(),encoded.size()); return true;
        });
    }
    @Test void realReadWriteDeleteAndEmptyValueHaveDefinedSemantics() {
        run(p(A), () -> {
            var key = keys.tenantResource("probe","crud"); assertTrue(tenant.read(key).isEmpty()); assertFalse(tenant.delete(key));
            tenant.write(key,"",Duration.ofSeconds(2)); assertEquals(Optional.of(""),tenant.read(key));
            tenant.write(key,"真实值",Duration.ofSeconds(2)); assertEquals("真实值",tenant.read(key).orElseThrow());
            assertTrue(tenant.delete(key)); assertTrue(tenant.read(key).isEmpty()); return true;
        });
    }
    @Test void realTtlExpiresWithBoundedPolling() {
        run(p(A), () -> {
            var key = keys.tenantResource("probe","ttl"); tenant.write(key,"短期值",Duration.ofMillis(200));
            assertEquals("短期值",tenant.read(key).orElseThrow());
            await().pollInSameThread().atMost(Duration.ofSeconds(3)).pollInterval(Duration.ofMillis(20)).until(() -> tenant.read(key).isEmpty()); return true;
        });
    }
    @Test void invalidTtlNullAndOversizedValuesRejectBeforeWrite() {
        run(p(A), () -> {
            var key = keys.tenantResource("probe","bounds");
            for (Duration ttl : List.of(Duration.ZERO,Duration.ofSeconds(-1),Duration.ofHours(25),Duration.ofNanos(1),Duration.ofNanos(1_000_001))) {
                assertThrows(IllegalArgumentException.class, () -> tenant.write(key,"值",ttl));
            }
            assertThrows(IllegalArgumentException.class, () -> tenant.write(key,"值",null));
            assertThrows(IllegalArgumentException.class, () -> tenant.write(key,null,Duration.ofSeconds(1)));
            assertThrows(IllegalArgumentException.class, () -> tenant.write(key,"汉".repeat(21846),Duration.ofSeconds(1)));
            assertThrows(IllegalArgumentException.class, () -> tenant.write(key,"\uD800",Duration.ofSeconds(1)));
            assertTrue(tenant.read(key).isEmpty());
            tenant.write(key,"a".repeat(65536),Duration.ofSeconds(2)); assertEquals(65536,tenant.read(key).orElseThrow().length()); return true;
        });
    }
    @Test void addressCapturedInACannotBeUsedInBOrNarrowerScope() {
        var a = p(A); var key = run(a, () -> keys.tenantResource("probe","bound"));
        run(p(B), () -> { assertThrows(TenantAccessDeniedException.class, () -> tenant.read(key));
            assertThrows(TenantAccessDeniedException.class, () -> tenant.write(key,"值",Duration.ofSeconds(1)));
            assertThrows(TenantAccessDeniedException.class, () -> tenant.delete(key)); return true; });
        run(a, () -> {
            try (var scope = TenantExecutionScope.narrow(principal(A,Set.of(DataScopeType.STORES),Set.of(A1)).grants().get(PERMISSION))) {
                assertThrows(TenantAccessDeniedException.class, () -> tenant.read(key));
            } return true;
        });
    }
    @Test void addressFromDifferentBuilderCannotSelectAnotherPrefixOrEnvironment() {
        run(p(A), () -> {
            var foreign = new RedisKeyBuilder("other", "prod", stores).tenantResource("probe","same");
            assertThrows(TenantAccessDeniedException.class, () -> tenant.read(foreign));
            assertThrows(TenantAccessDeniedException.class, () -> tenant.write(foreign,"值",Duration.ofSeconds(1))); return true;
        });
    }
    @Test void platformIsIndependentPermissionCheckedAndNeverFallsBackFromTenant() {
        var principal = new CurrentPrincipal(PrincipalType.PLATFORM,OWNER,null,UUID.randomUUID(),0,Set.of(PlatformRedisAccess.PERMISSION),Set.of(),Map.of());
        platformIdentity.set(principal); var key = platform.resource("probe","same");
        platform.write(key,"平台值",Duration.ofSeconds(2)); assertEquals("平台值",platform.read(key).orElseThrow());
        run(p(A), () -> {
            var tenantKey = keys.tenantResource("probe","same");
            assertNotEquals(key.encoded,tenantKey.encoded); assertTrue(tenant.read(tenantKey).isEmpty());
            assertThrows(TenantAccessDeniedException.class, () -> tenant.read(key));
            assertThrows(PermissionDeniedException.class, () -> platform.read(key)); return true;
        });
        var tenantKey = run(p(A), () -> keys.tenantResource("probe","same"));
        assertThrows(PermissionDeniedException.class, () -> platform.read(tenantKey));
        platformIdentity.set(p(A)); assertThrows(BusinessException.class, () -> platform.resource("probe","same"));
        platformIdentity.set(new CurrentPrincipal(PrincipalType.PLATFORM,OWNER,null,UUID.randomUUID(),0,Set.of(),Set.of(),Map.of()));
        assertThrows(PermissionDeniedException.class, () -> platform.resource("probe","same"));
        platformIdentity.set(null); assertThrows(BusinessException.class, () -> platform.read(key));
    }
    @Test void rawResourceHitRequiresCurrentStorePolicyAndFilteredListsHaveNoApi() {
        var id = "resource-a2";
        run(p(A), () -> { tenant.write(keys.tenantResource("probe",id),"门店A2原始资源",Duration.ofSeconds(2)); return true; });
        var limited = principal(A,Set.of(DataScopeType.STORES),Set.of(A1));
        run(limited, () -> {
            // 原始资源可共享存储，消费适配器在命中后仍复核本次资源范围。
            assertTrue(tenant.read(keys.tenantResource("probe",id)).isPresent());
            assertThrows(TenantAccessDeniedException.class, () -> stores.requireStore(A2)); return true;
        });
        assertEquals(Set.of("tenantResource","storeResource"),Arrays.stream(RedisKeyBuilder.class.getDeclaredMethods())
                .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers())).map(java.lang.reflect.Method::getName).collect(java.util.stream.Collectors.toSet()));
    }
    @Test void unknownSerializationVersionMalformedUtf8AndOversizedStoredValueFailExplicitly() {
        run(p(A), () -> {
            var key = keys.tenantResource("probe","corrupt");
            try (var raw = factory.getConnection()) {
                for (byte[] bad : List.of(new byte[]{'v','2',':','x'},new byte[]{'v','1',':',(byte)0xff},new byte[65540])) {
                    raw.stringCommands().set(key.encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8),bad);
                    assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE,assertThrows(BusinessException.class, () -> tenant.read(key)).error().code());
                }
            }
            assertTrue(tenant.delete(key)); return true;
        });
    }
    @Test void actualRedisOutageIsInfrastructureFailureForReadWriteAndDelete() {
        try (var isolated = new GenericContainer<>(DockerImageName.parse(RedisTestSupport.IMAGE)).withExposedPorts(6379)) {
            isolated.start();
            var broken = new LettuceConnectionFactory(new RedisStandaloneConfiguration(isolated.getHost(),isolated.getMappedPort(6379)),
                    LettuceClientConfiguration.builder().commandTimeout(Duration.ofMillis(100)).shutdownTimeout(Duration.ofMillis(100)).build());
            broken.afterPropertiesSet(); broken.start();
            try {
                var access = new TenantRedisAccess(keys,new RedisValueStore(broken),stores);
                run(p(A), () -> { var key = keys.tenantResource("probe","outage"); access.write(key,"值",Duration.ofSeconds(2));
                    assertEquals("值",access.read(key).orElseThrow()); isolated.stop();
                    for (Runnable op : List.<Runnable>of(() -> access.read(key),() -> access.write(key,"值",Duration.ofSeconds(1)),() -> access.delete(key))) {
                        assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE,assertThrows(BusinessException.class,op::run).error().code());
                    } return true;
                });
            } finally { broken.destroy(); }
        }
    }
}
