package com.pet.testing.tenantpersistence;

import com.pet.platform.Application;
import com.pet.platform.shared.exception.TenantAccessDeniedException;
import com.pet.platform.shared.security.*;
import com.pet.platform.shared.tenancy.*;
import com.pet.testing.*;
import jakarta.persistence.EntityManager;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import org.junit.jupiter.api.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class, properties = {
    "spring.flyway.locations=classpath:security-migrations", "spring.datasource.hikari.maximum-pool-size=1", "spring.datasource.hikari.minimum-idle=1"})
@ActiveProfiles("test") @Import({SafetyFixtures.class,AsyncTenantPersistenceIT.Probes.class})
class AsyncTenantPersistenceIT {
    static final UUID A=UUID.randomUUID(),B=UUID.randomUUID(),OWNER=UUID.randomUUID(),OTHER=UUID.randomUUID(),A1=UUID.randomUUID(),A2=UUID.randomUUID(),B1=UUID.randomUUID();
    static final String PERMISSION="safety:resource:operate";
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse(PostgresIntegrationSupport.IMAGE).asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("p04_async_fixture");
    static {
        POSTGRES.start(); Runtime.getRuntime().addShutdownHook(new Thread(POSTGRES::stop,"p04-async-postgres-cleanup"));
        try (var c=DriverManager.getConnection(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword()); var s=c.createStatement()) {
            s.execute("CREATE ROLE security_probe_runtime LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOINHERIT");
            try(var q=c.prepareStatement("select format('ALTER ROLE security_probe_runtime PASSWORD %L', ?::text)")) {
                q.setString(1,POSTGRES.getPassword()); try(var r=q.executeQuery()) { r.next(); s.execute(r.getString(1)); }
            }
        } catch(SQLException failure) { throw new IllegalStateException("异步测试受限角色初始化失败",failure); }
    }
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        RedisTestSupport.properties(r);
        r.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl().split("\\?",2)[0]);
        r.add("spring.datasource.username", () -> "security_probe_runtime"); r.add("spring.datasource.password",POSTGRES::getPassword);
        r.add("spring.flyway.url", () -> POSTGRES.getJdbcUrl().split("\\?",2)[0]);
        r.add("spring.flyway.user",POSTGRES::getUsername); r.add("spring.flyway.password",POSTGRES::getPassword); r.add("spring.flyway.init-sql", () -> "");
    }
    @TestConfiguration(proxyBeanMethods=false) static class Probes {
        @Bean AsyncDatabaseProbe asyncDatabaseProbe(SafetyRepositories.Parent parents,EntityManager em) { return new AsyncDatabaseProbe(parents,em); }
    }
    @Autowired SafetyApplicationService service;
    @Autowired SafetyFixtures.PrincipalFixture identity;
    @Autowired TrustedTenantExecutor trusted;
    @Autowired TenantTaskExecutor configuredExecutor;
    @Autowired AsyncDatabaseProbe probe;
    @Autowired JdbcTemplate runtime;
    @Autowired PlatformTransactionManager transactions;
    JdbcTemplate owner;
    UUID ap,ap2,bp,as1,as2,bs1,own,other,customer;
    final List<Map<String,Object>> taskObservations = new ArrayList<>();
    CurrentPrincipal principal(UUID id,PrincipalType domain,UUID subject,Set<DataScopeType> types,Set<UUID> stores) {
        return new CurrentPrincipal(domain,subject,id,UUID.randomUUID(),0,Set.of(PERMISSION),id.equals(A)?Set.of(A1,A2):Set.of(B1),
                Map.of(PERMISSION,new DataScope(id,domain,subject,types,stores)));
    }
    CurrentPrincipal p(UUID id) { return principal(id,PrincipalType.STAFF,OWNER,Set.of(DataScopeType.TENANT),Set.of()); }
    <T> T scope(CurrentPrincipal p,Supplier<T> work) {
        identity.set(p); try { return trusted.execute(PERMISSION,work); }
        finally { identity.set(null); assertTrue(TenantContextHolder.current().isEmpty()); }
    }
    static <T> T result(Future<T> f) throws Exception { return f.get(10,TimeUnit.SECONDS); }
    static void cleanWorker() {
        assertTrue(TenantContextHolder.current().isEmpty());
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive()); assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
        assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
        for(String k:List.of("tenantId","operatorId","storeId","traceId")) assertNull(MDC.get(k));
    }
    static TenantTaskExecutor executor(int threads) { return new TenantTaskExecutor(threads,8,Duration.ofSeconds(30),Duration.ofSeconds(2)); }
    @BeforeEach void seed() {
        cleanWorker(); taskObservations.clear();
        owner=new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword()));
        owner.execute("TRUNCATE safety_child,safety_parent,safety_store_resource,safety_owned_resource,safety_store_fact");
        owner.update("insert into safety_store_fact(id,tenant_id) values (?,?),(?,?),(?,?)",A1,A,A2,A,B1,B);
        ap=scope(p(A),()->service.create("first")); ap2=scope(p(A),()->service.create("second")); bp=scope(p(B),()->service.create("first"));
        as1=scope(p(A),()->service.createStore(A1,"store1")); as2=scope(p(A),()->service.createStore(A2,"store2")); bs1=scope(p(B),()->service.createStore(B1,"store1"));
        own=scope(principal(A,PrincipalType.STAFF,OWNER,Set.of(DataScopeType.SELF),Set.of()),service::createOwned);
        other=scope(principal(A,PrincipalType.STAFF,OTHER,Set.of(DataScopeType.SELF),Set.of()),service::createOwned);
        customer=scope(principal(A,PrincipalType.CUSTOMER,OWNER,Set.of(DataScopeType.SELF),Set.of()),service::createOwned);
    }
    @AfterEach void databaseEvidence(TestInfo info) throws Exception {
        cleanWorker();
        String guc=runtime.queryForObject("select coalesce(nullif(current_setting('pet.tenant_id',true),''),'EMPTY')",String.class);
        assertEquals("EMPTY",guc); assertEquals(0,runtime.queryForObject("select count(*) from safety_parent",Long.class));
        var snapshot=new LinkedHashMap<String,Object>();snapshot.put("test",info.getTestMethod().orElseThrow().getName());snapshot.put("outsideTransactionTenantGuc",guc);
        snapshot.put("runtimeRole",runtime.queryForObject("select current_user",String.class));
        snapshot.put("role",runtime.queryForMap("select rolsuper,rolbypassrls,rolinherit,rolcreatedb,rolcreaterole from pg_roles where rolname=current_user"));
        snapshot.put("observations",taskObservations);
        snapshot.put("parents",owner.queryForList("select code,display_name,version from safety_parent order by tenant_id,code"));
        var dir=java.nio.file.Path.of("target/p04-03-database-observations");java.nio.file.Files.createDirectories(dir);
        java.nio.file.Files.writeString(dir.resolve(info.getTestMethod().orElseThrow().getName()+".json"),tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(snapshot));
    }
    @AfterAll static void stop() { POSTGRES.stop(); }
    String name(UUID id) { return owner.queryForObject("select display_name from safety_parent where id=?",String.class,id); }
    @Test void runtimeIsNonOwnerNonSuperuserNonBypassAndFiveTablesHaveForcedRls() {
        var role=runtime.queryForMap("select rolsuper,rolbypassrls,rolinherit,rolcreatedb,rolcreaterole from pg_roles where rolname=current_user");
        for(Object flag:role.values()) assertEquals(false,flag);
        assertEquals(0,runtime.queryForObject("select count(*) from pg_class where relname like 'safety_%' and relowner=(select oid from pg_roles where rolname=current_user)",Long.class));
        assertEquals(0,runtime.queryForObject("select count(*) from pg_auth_members where member=(select oid from pg_roles where rolname=current_user)",Long.class));
        assertEquals(5,runtime.queryForObject("select count(*) from pg_class where relname in ('safety_parent','safety_child','safety_store_resource','safety_owned_resource','safety_store_fact') and relrowsecurity and relforcerowsecurity",Long.class));
        assertNotEquals("security_probe_runtime",owner.queryForObject("select current_user",String.class));
        assertFalse(runtime.queryForObject("select has_schema_privilege(current_user,'public','CREATE')",Boolean.class));
    }
    @Test void configuredBeanCreatesIndependentWorkerTransactionAfterCommittedCaller() throws Exception {
        var caller=scope(p(A),probe::observe);
        var f=scope(p(A),()->configuredExecutor.submit(()->{
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive()); assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
            return probe.observe();
        }));
        var worker=result(f);taskObservations.add(worker);
        assertNotEquals(caller.get("transactionId"),worker.get("transactionId"));assertEquals(true,worker.get("transactionActive"));assertEquals(A.toString(),worker.get("tenantGuc"));
        assertEquals(2L,worker.get("count"));result(configuredExecutor.submitUnscoped(()->{cleanWorker();return true;}));
    }
    @Test void sameThreadAndSingleDatabaseConnectionRunAThenBThenAnonymous() throws Exception {
        try(var tasks=executor(1)) {
            long thread=result(tasks.submitUnscoped(()->{cleanWorker();return Thread.currentThread().threadId();}));
            Set<Object> connections=new HashSet<>();
            for(UUID id:List.of(A,B,A,B)) {
                var observation=result(scope(p(id),()->tasks.submit(()->{assertEquals(thread,Thread.currentThread().threadId());return probe.observe();})));
                taskObservations.add(observation);connections.add(observation.get("connectionPid"));
                assertEquals(id.toString(),observation.get("tenantGuc"));assertEquals(id.equals(A)?2L:1L,observation.get("count"));
                assertEquals(thread,result(tasks.submitUnscoped(()->{cleanWorker();return Thread.currentThread().threadId();})));
            }
            assertEquals(1,connections.size());
        }
    }
    @Test void asyncAAndBQueriesAndWritesNeverReachForeignRows() throws Exception {
        try(var tasks=executor(1)) {
            result(scope(p(A),()->tasks.submit(()->{
                assertEquals("2",service.listParents(100).total());assertFalse(service.existsParent(bp));
                assertThrows(TenantAccessDeniedException.class,()->service.rename(bp,"越权"));service.rename(ap,"异步A合法");return true;
            })));
            result(scope(p(B),()->tasks.submit(()->{assertFalse(service.existsParent(ap));service.rename(bp,"异步B合法");return true;})));
            assertEquals("异步A合法",name(ap));assertEquals("异步B合法",name(bp));assertEquals("初值",name(ap2));
            result(tasks.submitUnscoped(()->{cleanWorker();return true;}));
        }
    }
    @Test void exceptionAfterFlushRollsBackAndFollowingBHasNoAState() throws Exception {
        try(var tasks=executor(1)) {
            var failure=scope(p(A),()->tasks.submit(()->{service.failAfterFlushedWrite(ap);return true;}));
            assertInstanceOf(IllegalStateException.class,assertThrows(ExecutionException.class,()->result(failure)).getCause());
            assertEquals("初值",name(ap));
            var observation=result(scope(p(B),()->tasks.submit(probe::observe)));taskObservations.add(observation);
            assertEquals(B.toString(),observation.get("tenantGuc"));assertEquals(1L,observation.get("count"));
            result(tasks.submitUnscoped(()->{cleanWorker();return true;}));
        }
    }
    @Test void asyncStoresEmptyScopesAndSelfSubjectDomainsUseActualPostgresPolicies() throws Exception {
        try(var tasks=executor(1)) {
            var empty=principal(A,PrincipalType.STAFF,OWNER,Set.of(DataScopeType.STORES),Set.of());
            assertTrue(result(scope(empty,()->tasks.submit(()->service.listStores().items().isEmpty()))));
            var store=principal(A,PrincipalType.STAFF,OWNER,Set.of(DataScopeType.STORES),Set.of(A1));
            assertEquals(List.of(as1),result(scope(store,()->tasks.submit(()->service.listStores().items()))));
            for(PrincipalType domain:List.of(PrincipalType.STAFF,PrincipalType.CUSTOMER)) {
                var self=principal(A,domain,OWNER,Set.of(DataScopeType.SELF),Set.of());
                assertEquals(List.of(domain==PrincipalType.STAFF?own:customer),result(scope(self,()->tasks.submit(()->service.listOwned().items()))));
            }
            result(tasks.submitUnscoped(()->{cleanWorker();return true;}));
        }
    }
    @Test void concurrentSnapshotScopesStayIndependentEvenWithOneDatabaseConnection() throws Exception {
        try(var tasks=executor(2)) {
            var barrier=new CyclicBarrier(2);var f=scope(p(A),()->tasks.submit(()->{barrier.await(5,TimeUnit.SECONDS);return probe.observe();}));
            var g=scope(p(B),()->tasks.submit(()->{barrier.await(5,TimeUnit.SECONDS);return probe.observe();}));
            var a=result(f);var b=result(g);taskObservations.add(a);taskObservations.add(b);
            assertEquals(A.toString(),a.get("tenantGuc"));assertEquals(B.toString(),b.get("tenantGuc"));assertNotEquals(a.get("transactionId"),b.get("transactionId"));
        }
    }
    @Test void submissionInsideActualJpaTransactionIsRejectedAndCommittedDataIsVisibleAfterReturn() throws Exception {
        var invoked=new AtomicBoolean();
        scope(p(A),()->new TransactionTemplate(transactions).execute(status->{
            service.countParents();assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertThrows(IllegalStateException.class,()->configuredExecutor.submit(()->{invoked.set(true);return true;}));return true;
        }));
        assertFalse(invoked.get());
        UUID added=scope(p(A),()->service.create("已提交后再提交任务"));
        assertTrue(result(scope(p(A),()->configuredExecutor.submit(()->service.existsParent(added)))));
        result(configuredExecutor.submitUnscoped(()->{cleanWorker();return true;}));
    }
    @Test void expiredScopeAtRealTransactionCommitRollsBackAndConnectionCanServeB() throws Exception {
        var time=new AtomicLong();
        // 仅测试构造时注入确定性单调时钟；不公开生产快照构造或时钟入口。
        var constructor=TenantTaskExecutor.class.getDeclaredConstructor(int.class,int.class,Duration.class,Duration.class,LongSupplier.class);
        constructor.setAccessible(true);
        try(var tasks=constructor.newInstance(1,4,Duration.ofSeconds(1),Duration.ofSeconds(2),(LongSupplier)time::get)) {
            var f=scope(p(A),()->tasks.submit(()->{probe.renameBeforeCommit(ap,()->time.set(TimeUnit.SECONDS.toNanos(2)));return true;}));
            assertInstanceOf(RejectedExecutionException.class,assertThrows(ExecutionException.class,()->result(f)).getCause());
            assertEquals("初值",name(ap));
            var b=result(scope(p(B),()->tasks.submit(probe::observe)));taskObservations.add(b);assertEquals(B.toString(),b.get("tenantGuc"));
            result(tasks.submitUnscoped(()->{cleanWorker();return true;}));
        }
    }
}
