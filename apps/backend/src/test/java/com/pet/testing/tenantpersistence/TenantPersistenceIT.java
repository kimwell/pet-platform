package com.pet.testing.tenantpersistence;

import com.pet.platform.Application;
import com.pet.platform.shared.exception.*;
import com.pet.platform.shared.security.*;
import com.pet.platform.shared.tenancy.*;
import com.pet.testing.PostgresIntegrationSupport;
import java.sql.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;

/** 真PostgreSQL隔离容器；没有测试自动回滚，每个验证读取都用独立owner连接。 */
@SpringBootTest(classes = Application.class, properties = {
    "spring.flyway.locations=classpath:security-migrations", "spring.datasource.hikari.maximum-pool-size=2",
    "spring.datasource.hikari.minimum-idle=1", "spring.jpa.properties.hibernate.cache.use_second_level_cache=false",
    "spring.jpa.properties.hibernate.cache.use_query_cache=false"})
@ActiveProfiles("test") @Import(SafetyFixtures.class)
class TenantPersistenceIT {
    static final String PERMISSION = "safety:resource:operate";
    static final UUID A = UUID.randomUUID(), B = UUID.randomUUID(), A1 = UUID.randomUUID(), A2 = UUID.randomUUID(), B1 = UUID.randomUUID();
    static final UUID OWNER = UUID.randomUUID(), OTHER = UUID.randomUUID();
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse(PostgresIntegrationSupport.IMAGE).asCompatibleSubstituteFor("postgres"))
        .withDatabaseName("p04_security_fixture");
    static {
        POSTGRES.start();
        Runtime.getRuntime().addShutdownHook(new Thread(POSTGRES::stop, "p04-security-postgres-cleanup"));
        // 仅临时容器账号；不保存、打印或复用到任何生产环境。
        try (var c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()); var s = c.createStatement()) {
            s.execute("CREATE ROLE security_probe_runtime LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOINHERIT");
            try (var q = c.prepareStatement("select format('ALTER ROLE security_probe_runtime PASSWORD %L', ?::text)")) {
                q.setString(1, POSTGRES.getPassword());
                try (var result = q.executeQuery()) { result.next(); s.execute(result.getString(1)); }
            }
        } catch (SQLException e) { throw new IllegalStateException("无法初始化安全测试角色", e); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        com.pet.testing.RedisTestSupport.properties(r);
        r.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl().split("\\?", 2)[0]);
        r.add("spring.datasource.username", () -> "security_probe_runtime"); r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.flyway.url", () -> POSTGRES.getJdbcUrl().split("\\?", 2)[0]);
        r.add("spring.flyway.user", POSTGRES::getUsername); r.add("spring.flyway.password", POSTGRES::getPassword);
    }
    @Autowired SafetyApplicationService service;
    @Autowired SafetyFixtures.PrincipalFixture provider;
    @Autowired TrustedTenantExecutor executor;
    @Autowired SafetyBypassProbe bypass;
    @Autowired SafetyRepositories.Parent parentRepository;
    @Autowired JdbcTemplate runtime;
    JdbcTemplate owner;
    UUID ap, ap2, bp, as1, as2, bs1, owned, otherOwned, customerOwned;

    @BeforeEach void fixtures() {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertTrue(TenantContextHolder.current().isEmpty());
        owner = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        owner.execute("DROP TRIGGER IF EXISTS safety_short_update ON safety_parent");
        owner.execute("TRUNCATE safety_child, safety_parent, safety_store_resource, safety_owned_resource, safety_store_fact");
        owner.update("insert into safety_store_fact(id,tenant_id) values (?,?),(?,?),(?,?)", A1,A,A2,A,B1,B);
        ap = run(tenant(A), () -> service.create("shared-code")); ap2 = run(tenant(A), () -> service.create("second"));
        bp = run(tenant(B), () -> service.create("shared-code"));
        as1 = run(tenant(A), () -> service.createStore(A1, "store1")); as2 = run(tenant(A), () -> service.createStore(A2, "store2"));
        bs1 = run(tenant(B), () -> service.createStore(B1, "store1"));
        owned = run(self(A, PrincipalType.STAFF, OWNER), service::createOwned);
        otherOwned = run(self(A, PrincipalType.STAFF, OTHER), service::createOwned);
        customerOwned = run(self(A, PrincipalType.CUSTOMER, OWNER), service::createOwned);
        provider.set(null);
    }
    @AfterEach void recordIndependentDatabaseState(TestInfo info) throws Exception {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
        assertTrue(TenantContextHolder.current().isEmpty());
        var snapshot = new LinkedHashMap<String,Object>();
        snapshot.put("test", info.getTestMethod().orElseThrow().getName());
        snapshot.put("postgresql", owner.queryForObject("show server_version", String.class));
        snapshot.put("runtimeRole", runtime.queryForObject("select current_user", String.class));
        snapshot.put("outsideTransactionTenantGuc", runtime.queryForObject("select coalesce(nullif(current_setting('pet.tenant_id',true),''),'EMPTY')", String.class));
        snapshot.put("parents", owner.queryForList("select case tenant_id when '" + A + "'::uuid then 'A' else 'B' end as tenant,code,display_name,version from safety_parent order by tenant_id,code"));
        snapshot.put("childCount",dbCount("safety_child")); snapshot.put("storeCount",dbCount("safety_store_resource"));
        var directory = java.nio.file.Path.of("target/p04-02-database-observations"); java.nio.file.Files.createDirectories(directory);
        java.nio.file.Files.writeString(directory.resolve(info.getTestMethod().orElseThrow().getName()+".json"),tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(snapshot));
    }
    @AfterAll static void stopContainer() { POSTGRES.stop(); }
    CurrentPrincipal principal(UUID tenant, PrincipalType type, UUID id, Set<DataScopeType> types, Set<UUID> stores) {
        var scope = new DataScope(tenant, type, id, types, stores);
        return new CurrentPrincipal(type, id, tenant, UUID.randomUUID(), 1, Set.of(PERMISSION),
            tenant.equals(A) ? Set.of(A1,A2) : Set.of(B1), Map.of(PERMISSION, scope));
    }
    CurrentPrincipal tenant(UUID tenant) { return principal(tenant, PrincipalType.STAFF, OWNER, Set.of(DataScopeType.TENANT), Set.of()); }
    CurrentPrincipal stores(Set<UUID> stores) { return principal(A, PrincipalType.STAFF, OWNER, Set.of(DataScopeType.STORES), stores); }
    CurrentPrincipal self(UUID tenant, PrincipalType type, UUID owner) { return principal(tenant,type,owner,Set.of(DataScopeType.SELF),Set.of()); }
    <T> T run(CurrentPrincipal principal, Supplier<T> work) { provider.set(principal); try { return executor.execute(PERMISSION, work); } finally { provider.set(null); assertTrue(TenantContextHolder.current().isEmpty()); } }
    void act(CurrentPrincipal principal, Runnable work) { run(principal, () -> { work.run(); return null; }); }
    String dbName(String table, UUID id) { return owner.queryForObject("select display_name from " + table + " where id=?", String.class, id); }
    long dbCount(String table) { return owner.queryForObject("select count(*) from " + table, Long.class); }
    String sqlState(Throwable failure) {
        for (Throwable cursor = failure; cursor != null; cursor = cursor.getCause()) if (cursor instanceof SQLException sql) return sql.getSQLState();
        return null;
    }
    void denied(CurrentPrincipal p, Runnable work) { act(p, () -> assertThrows(TenantAccessDeniedException.class, work::run)); }

    @Test void pagesExcludeOtherTenantAndUseSecuredActualCount() {
        var result = run(tenant(A), () -> service.listParents(1)); assertEquals("2",result.total()); assertEquals(1,result.items().size()); assertFalse(result.items().contains(bp));
        assertEquals("1",run(tenant(B), () -> service.listParents(100)).total());
    }
    @Test void idDetailUnifiesMissingAndOtherTenant() {
        var p = tenant(A);
        run(p, () -> { var missing = assertThrows(TenantAccessDeniedException.class, () -> service.name(UUID.randomUUID()));
            var hidden = assertThrows(TenantAccessDeniedException.class, () -> service.name(bp));
            assertEquals(missing.getMessage(),hidden.getMessage()); assertEquals(missing.error().code(),hidden.error().code()); return null; });
    }
    @Test void existsCountAndIdSetsDoNotLeakOtherTenant() {
        run(tenant(A), () -> { assertFalse(service.existsParent(bp)); assertEquals(2,service.countParents()); assertEquals(List.of(ap),service.parentIds(List.of(ap,bp))); return null; });
    }
    @Test void businessOrCannotEscapeOuterTenantAndCount() {
        var result = run(tenant(A), () -> service.orParents(1)); assertEquals("2",result.total()); assertFalse(result.items().contains(bp));
    }
    @Test void emptyStoresHaveNoRowsTotalsOrExistence() {
        run(stores(Set.of()), () -> { assertTrue(service.listStores().items().isEmpty()); assertEquals("0",service.listStores().total()); assertEquals(0,service.countStores()); assertFalse(service.existsStore(as1)); return null; });
    }
    @Test void storeRangeExcludesSameTenantOtherStoreAndOtherTenant() {
        run(stores(Set.of(A1)), () -> { assertEquals(List.of(as1),service.listStores().items()); assertEquals("1",service.listStores().total()); assertFalse(service.existsStore(as2)); assertFalse(service.existsStore(bs1)); return null; });
    }
    @Test void selfUsesExplicitOwnerAndPrincipalTypeAcrossSameUuid() {
        run(self(A,PrincipalType.STAFF,OWNER), () -> { assertEquals(List.of(owned),service.listOwned().items()); assertEquals(1,service.countOwned()); assertFalse(service.existsOwned(otherOwned)); assertFalse(service.existsOwned(customerOwned)); return null; });
        assertEquals(List.of(customerOwned),run(self(A,PrincipalType.CUSTOMER,OWNER), service::listOwned).items());
    }
    @Test void declaredStoreAndSelfUnionStaysInsideTenant() {
        var p = principal(A,PrincipalType.STAFF,OWNER,Set.of(DataScopeType.STORES,DataScopeType.SELF),Set.of(A1));
        assertEquals(Set.of(as1,as2), new HashSet<>(run(p,service::listStores).items()));
    }
    @Test void tenantStoreQueriesAndWritesStillRespectIdentityStoreCeiling() {
        var p = tenant(A); var limited = new CurrentPrincipal(p.principalType(),p.principalId(),p.tenantId(),p.sessionId(),p.authorizationVersion(),p.permissionCodes(),Set.of(A1),p.grants());
        assertEquals(List.of(as1),run(limited,service::listStores).items()); denied(limited, () -> service.renameStore(as2,"越权")); assertEquals("门店夹具",dbName("safety_store_resource",as2));
    }
    @Test void storeSingleWritesRejectOutOfScopeAndLegitimateUpdateCommits() {
        denied(stores(Set.of(A1)), () -> service.renameStore(as2,"越权")); denied(stores(Set.of(A1)), () -> service.removeStore(bs1));
        assertEquals("门店夹具",dbName("safety_store_resource",as2)); assertEquals(3,dbCount("safety_store_resource"));
        act(stores(Set.of(A1)), () -> service.renameStore(as1,"合法门店更新")); assertEquals("合法门店更新",dbName("safety_store_resource",as1));
    }
    @Test void selfWritesUseOwnerTypeAndIdAndDoNotAffectOtherOwners() {
        var p = self(A,PrincipalType.STAFF,OWNER); denied(p, () -> service.renameOwned(otherOwned,"越权")); denied(p, () -> service.removeOwned(customerOwned));
        assertEquals("本人夹具",dbName("safety_owned_resource",otherOwned)); assertEquals(3,dbCount("safety_owned_resource"));
        act(p, () -> service.renameOwned(owned,"合法本人更新")); assertEquals("合法本人更新",dbName("safety_owned_resource",owned));
    }
    @Test void tenantLevelResourceRejectsUndeclaredStoresAndSelf() {
        denied(stores(Set.of(A1)),service::countParents); denied(self(A,PrincipalType.STAFF,OWNER),service::countParents);
        denied(stores(Set.of(A1)),service::listOwned);
    }
    @Test void creationAssignsOnlyCurrentTenantAndTenantBusinessCodeIsNotGlobal() {
        UUID id = run(tenant(A), () -> service.create("fresh")); assertEquals(A,owner.queryForObject("select tenant_id from safety_parent where id=?",UUID.class,id));
        assertEquals(2,owner.queryForObject("select count(*) from safety_parent where code='shared-code'",Integer.class));
    }
    @Test void reflectedForgeryBeforePersistIsRejectedAndNothingInserted() {
        denied(tenant(A), () -> service.forgedCreate(B)); assertEquals(3,dbCount("safety_parent"));
    }
    @Test void updateOtherTenantDoesNotChangeDatabase() {
        denied(tenant(A), () -> service.rename(bp,"越权")); assertEquals("初值",dbName("safety_parent",bp));
    }
    @Test void deleteOtherTenantDoesNotChangeDatabase() {
        denied(tenant(A), () -> service.remove(bp)); assertEquals(3,dbCount("safety_parent"));
    }
    @Test void legitimateManagedUpdateAndScopedDeleteCommit() {
        act(tenant(A), () -> service.rename(ap,"合法更新")); assertEquals("合法更新",dbName("safety_parent",ap));
        act(tenant(A), () -> service.remove(ap)); assertEquals(2,dbCount("safety_parent")); assertEquals("初值",dbName("safety_parent",bp));
    }
    @Test void detachedEntityCannotBeInsertedOrMergedThroughControlledPort() {
        var detached = run(tenant(B), () -> service.detached(bp)); denied(tenant(A), () -> service.attemptedEntity(detached));
        assertEquals("初值",dbName("safety_parent",bp)); assertEquals(3,dbCount("safety_parent"));
    }
    @Test void crossTenantAssociationRejectedByApplicationWithoutWrite() {
        denied(tenant(A), () -> service.createChild(bp)); assertEquals(0,dbCount("safety_child"));
    }
    @Test void compositeForeignKeyRejectsAssociationEvenWhenApplicationIsBypassed() {
        var error = assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> owner.update("insert into safety_child(id,tenant_id,parent_id,created_at,updated_at,version,display_name) values (?,?,?,now(),now(),0,'绕过夹具')",UUID.randomUUID(),A,bp));
        assertEquals("23503", ((SQLException)error.getRootCause()).getSQLState()); assertEquals(0,dbCount("safety_child"));
    }
    @Test void legalAssociationUsesControlledParentProjectionAndRestrictsDelete() {
        UUID child = run(tenant(A), () -> service.createChild(ap)); assertEquals("初值",run(tenant(A), () -> service.parentOfChild(child)));
        act(tenant(A), () -> assertThrows(RuntimeException.class, () -> service.remove(ap))); assertEquals(3,dbCount("safety_parent")); assertEquals(1,dbCount("safety_child"));
    }
    @Test void associationDoesNotIntroduceImplicitJpaNavigationOrCascade() {
        for (var field : SafetyChild.class.getDeclaredFields()) {
            assertFalse(field.isAnnotationPresent(jakarta.persistence.ManyToOne.class));
            assertFalse(field.isAnnotationPresent(jakarta.persistence.OneToMany.class));
            assertNotEquals(SafetyParent.class,field.getType());
        }
    }
    @Test void storeCreateRejectsSameTenantUnauthorizedAndForeignStore() {
        denied(stores(Set.of(A1)), () -> service.createStore(A2,"禁止")); denied(stores(Set.of(A1)), () -> service.createStore(B1,"禁止")); assertEquals(3,dbCount("safety_store_resource"));
    }
    @Test void compositeStoreForeignKeyRejectsCrossTenantFact() {
        var error = assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> owner.update("insert into safety_store_resource(id,tenant_id,store_id,created_at,updated_at,version,code,display_name,owner_type,owner_id) values (?,?,?,now(),now(),0,'伪造门店','伪造','STAFF',?)",UUID.randomUUID(),A,B1,OWNER));
        assertEquals("23503", ((SQLException)error.getRootCause()).getSQLState()); assertEquals(3,dbCount("safety_store_resource"));
    }
    @Test void ordinaryUpdatesCannotMoveTenantOrStoreEvenUsingReflection() {
        denied(tenant(A), () -> service.mutateTenant(ap,B)); denied(tenant(A), () -> service.mutateStore(as1,A2));
        assertEquals(A,owner.queryForObject("select tenant_id from safety_parent where id=?",UUID.class,ap)); assertEquals(A1,owner.queryForObject("select store_id from safety_store_resource where id=?",UUID.class,as1)); assertEquals("门店夹具",dbName("safety_store_resource",as1));
    }
    @Test void validAtomicBatchUpdatesActualUniqueRowsAndVersion() {
        assertEquals(2,run(tenant(A), () -> service.renameAll(List.of(ap,ap2),"批次成功"))); assertEquals("批次成功",dbName("safety_parent",ap)); assertEquals("批次成功",dbName("safety_parent",ap2)); assertEquals(1L,owner.queryForObject("select version from safety_parent where id=?",Long.class,ap));
    }
    @Test void mixedTenantBatchUpdatesNothing() {
        denied(tenant(A), () -> service.renameAll(List.of(ap,bp),"越权")); assertEquals("初值",dbName("safety_parent",ap)); assertEquals("初值",dbName("safety_parent",bp));
    }
    @Test void mixedUnauthorizedStoreBatchUpdatesNothing() {
        denied(stores(Set.of(A1)), () -> service.renameStores(List.of(as1,as2),"越权")); assertEquals("门店夹具",dbName("safety_store_resource",as1)); assertEquals("门店夹具",dbName("safety_store_resource",as2));
    }
    @Test void duplicateBatchIdsAreDeduplicatedBeforeCountAndWrite() {
        assertEquals(1,run(tenant(A), () -> service.renameAll(List.of(ap,ap,ap),"去重"))); assertEquals("去重",dbName("safety_parent",ap)); assertEquals(1L,owner.queryForObject("select version from safety_parent where id=?",Long.class,ap));
    }
    @Test void missingTargetRejectsWholeBatchAndNoIdIsDisclosed() {
        denied(tenant(A), () -> service.renameAll(List.of(ap,UUID.randomUUID()),"越权")); assertEquals("初值",dbName("safety_parent",ap));
    }
    @Test void alreadyFlushedWriteRollsBackAtApplicationFailure() {
        act(tenant(A), () -> assertThrows(IllegalStateException.class, () -> service.failAfterFlushedWrite(ap))); assertEquals("初值",dbName("safety_parent",ap));
    }
    @Test void affectedRowMismatchRollsBackRealPartialDml() {
        owner.execute("CREATE OR REPLACE FUNCTION safety_skip_one() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.display_name='行数异常' AND OLD.code='shared-code' THEN RETURN NULL; END IF; RETURN NEW; END $$");
        owner.execute("CREATE TRIGGER safety_short_update BEFORE UPDATE ON safety_parent FOR EACH ROW EXECUTE FUNCTION safety_skip_one()");
        act(tenant(A), () -> assertThrows(IllegalStateException.class, () -> service.renameAll(List.of(ap,ap2),"行数异常")));
        assertEquals("初值",dbName("safety_parent",ap)); assertEquals("初值",dbName("safety_parent",ap2)); assertEquals(0L,owner.queryForObject("select version from safety_parent where id=?",Long.class,ap2));
    }
    @Test void atomicDeleteRejectsMixedTenantAndCommitsLegalBatch() {
        denied(tenant(A), () -> service.removeAll(List.of(ap,bp))); assertEquals(3,dbCount("safety_parent"));
        assertEquals(2,run(tenant(A), () -> service.removeAll(List.of(ap,ap2)))); assertEquals(1,dbCount("safety_parent")); assertEquals("初值",dbName("safety_parent",bp));
    }
    @Test void atomicStoreDeleteRejectsUnauthorizedTarget() {
        denied(stores(Set.of(A1)), () -> service.removeStores(List.of(as1,as2))); assertEquals(3,dbCount("safety_store_resource"));
    }
    @Test void batchBoundsAndSafetyAttributeWhitelistRejectBeforeWrite() {
        act(tenant(A), () -> { assertThrows(IllegalArgumentException.class, () -> service.renameAll(Collections.nCopies(101,ap),"越权")); assertThrows(IllegalArgumentException.class, () -> service.renameAll(List.of(),"越权")); assertThrows(IllegalArgumentException.class, () -> service.forbiddenField(List.of(ap))); }); assertEquals("初值",dbName("safety_parent",ap));
    }
    @Test void noContextAndAuthorityReadCannotUseControlledPersistence() {
        assertThrows(TenantAccessDeniedException.class,service::countParents); assertThrows(TenantAccessDeniedException.class, () -> service.create("匿名")); assertEquals(3,dbCount("safety_parent"));
    }
    @Test void authorityRootWithoutBusinessPurposeRefusesDatabaseAccess() throws Exception {
        provider.set(tenant(A));
        try {
            var filter = new TenantContextFilter(provider, null);
            filter.doFilter(new org.springframework.mock.web.MockHttpServletRequest(), new org.springframework.mock.web.MockHttpServletResponse(),
                (request, response) -> assertThrows(TenantAccessDeniedException.class, service::countParents));
        } finally { provider.set(null); }
        assertTrue(TenantContextHolder.current().isEmpty());
    }
    @Test void controlledRepositoryRequiresActualTransaction() {
        act(tenant(A), () -> assertThrows(IllegalStateException.class, () -> parentRepository.total(null)));
    }
    @Test void persistenceCapabilityCannotBorrowAnotherPermissionRange() {
        var p = tenant(A); String otherPermission = "safety:resource:read";
        var other = new CurrentPrincipal(p.principalType(),p.principalId(),p.tenantId(),p.sessionId(),p.authorizationVersion(),
            Set.of(otherPermission),p.authorizedStoreIds(),Map.of(otherPermission,p.grants().get(PERMISSION)));
        provider.set(other);
        try { executor.execute(otherPermission, () -> { assertThrows(PermissionDeniedException.class, service::countParents); return null; }); }
        finally { provider.set(null); }
    }
    @Test void repeatedTenantsDoNotReusePredicatesEntitiesOrConnectionGuc() {
        for (int i=0;i<4;i++) { assertEquals("2",run(tenant(A), () -> service.listParents(100)).total()); assertEquals("1",run(tenant(B), () -> service.listParents(100)).total()); assertEquals(0,bypass.withoutScope()); }
    }
    @Test void nestedNarrowRepositoryCannotRecoverOuterGrant() {
        var p = tenant(A); run(p, () -> { try (var narrow = TenantExecutionScope.narrow(new DataScope(A,PrincipalType.STAFF,OWNER,Set.of(DataScopeType.STORES),Set.of(A1)))) {
            try (var samePermission = TenantExecutionScope.forPermission(PERMISSION)) { assertEquals(List.of(as1),service.listStores().items()); assertFalse(service.existsStore(as2)); } } return null; });
    }
    @Test void transactionCannotWriteEntityLoadedInWiderExecutionScope() {
        var narrow = new DataScope(A,PrincipalType.STAFF,OWNER,Set.of(DataScopeType.STORES),Set.of(A1));
        denied(tenant(A), () -> service.readThenNarrowAndWrite(as2,narrow)); assertEquals("门店夹具",dbName("safety_store_resource",as2));
    }
    @Test void commitRejectsScopeThatChangedAfterFirstDatabaseAccess() {
        var narrow = new DataScope(A,PrincipalType.STAFF,OWNER,Set.of(DataScopeType.STORES),Set.of(A1));
        denied(tenant(A), () -> service.narrowInside(narrow));
    }
    @Test void requiresNewRestoresOuterTransactionBindingAndLocalRls() {
        assertEquals(4,run(tenant(A), () -> bypass.requiredWithNew(bypass))); assertEquals(0,bypass.withoutScope());
    }
    @Test void rlsProtectsUnscopedNativeAndJpqlDmlAgainstForeignTenant() {
        assertEquals(0,run(tenant(A), () -> bypass.nativeRename(bp))); assertEquals(0,run(tenant(A), () -> bypass.jpqlRename(bp))); assertEquals("初值",dbName("safety_parent",bp));
        assertEquals(1,run(tenant(A), () -> bypass.nativeRename(ap))); assertEquals("原生写入",dbName("safety_parent",ap));
    }
    @Test void rlsWithCheckRejectsNativeTenantMoveAndForgedInsert() {
        act(tenant(A), () -> assertEquals("42501", sqlState(assertThrows(RuntimeException.class, () -> bypass.forceTenantMove(ap2,B)))));
        act(tenant(A), () -> assertEquals("42501", sqlState(assertThrows(RuntimeException.class, () -> bypass.forgedNativeInsert(B)))));
        assertEquals(A,owner.queryForObject("select tenant_id from safety_parent where id=?",UUID.class,ap2)); assertEquals(3,dbCount("safety_parent"));
    }
    @Test void rlsRejectsWithoutTenantAndRuntimeCannotDdlTruncateOrEscalate() {
        assertEquals(0,bypass.withoutScope());
        assertEquals("security_probe_runtime",runtime.queryForObject("select current_user",String.class));
        var attributes = runtime.queryForMap("select rolsuper,rolbypassrls,rolcreaterole,rolcreatedb from pg_roles where rolname=current_user");
        assertTrue(attributes.values().stream().allMatch(Boolean.FALSE::equals));
        for (String sql : List.of("TRUNCATE safety_parent", "ALTER TABLE safety_parent DISABLE ROW LEVEL SECURITY", "CREATE TABLE safety_escape(id int)", "SET ROLE " + POSTGRES.getUsername())) {
            assertEquals("42501",sqlState(assertThrows(org.springframework.dao.DataAccessException.class, () -> runtime.execute(sql))));
        }
        assertThrows(org.springframework.dao.DataAccessException.class, () -> runtime.update("insert into safety_parent(id,tenant_id,created_at,updated_at,version,code,display_name) values (?,?,now(),now(),0,'拒绝','拒绝')",UUID.randomUUID(),A));
        assertEquals(3,dbCount("safety_parent"));
    }
    @Test void allFixtureTenantTablesHaveForcedRlsPoliciesAndCompositeReferences() {
        var tables = owner.queryForList("select relname,relrowsecurity,relforcerowsecurity from pg_class where relname in ('safety_parent','safety_child','safety_store_resource','safety_owned_resource','safety_store_fact')");
        assertEquals(5,tables.size()); for(var t:tables) { assertEquals(true,t.get("relrowsecurity")); assertEquals(true,t.get("relforcerowsecurity")); }
        assertEquals(5,owner.queryForObject("select count(*) from pg_constraint where contype='u' and pg_get_constraintdef(oid)='UNIQUE (tenant_id, id)' and conrelid in ('safety_parent'::regclass,'safety_child'::regclass,'safety_store_resource'::regclass,'safety_owned_resource'::regclass,'safety_store_fact'::regclass)",Integer.class));
        assertEquals(5,owner.queryForObject("select count(*) from pg_policies where tablename like 'safety_%'",Integer.class));
        assertEquals(2,owner.queryForObject("select count(*) from pg_constraint where contype='f' and conrelid in ('safety_child'::regclass,'safety_store_resource'::regclass) and array_length(conkey,1)=2",Integer.class));
    }
}
