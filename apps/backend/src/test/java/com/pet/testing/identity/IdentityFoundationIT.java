package com.pet.testing.identity;

import com.pet.platform.Application;
import com.pet.platform.identity.application.*;
import com.pet.platform.identity.application.authentication.*;
import com.pet.platform.identity.application.bootstrap.*;
import com.pet.platform.identity.infrastructure.bootstrap.*;
import com.pet.platform.platform.infrastructure.PostgresStoreOwnershipReader;
import com.pet.platform.shared.api.*;
import com.pet.platform.shared.exception.*;
import com.pet.platform.shared.security.*;
import com.pet.platform.shared.tenancy.*;
import com.pet.testing.*;
import jakarta.persistence.EntityManagerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;

/** 独立临时PostgreSQL，正式migration/真实角色；测试数据不属于业务验收账号。 */
@SpringBootTest(classes=Application.class) @ActiveProfiles("test")
class IdentityFoundationIT {
    static final PostgreSQLContainer POSTGRES=new PostgreSQLContainer(DockerImageName.parse(PostgresIntegrationSupport.IMAGE).asCompatibleSubstituteFor("postgres"))
        .withDatabaseName("p05_identity_fixture");
    static { POSTGRES.start();IdentityDatabaseSupport.provision(POSTGRES);Runtime.getRuntime().addShutdownHook(new Thread(POSTGRES::stop,"p05-fixture-cleanup")); }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        RedisTestSupport.properties(r);
        r.add("spring.datasource.url",() -> POSTGRES.getJdbcUrl().split("\\?",2)[0]);
        r.add("spring.datasource.username",() -> IdentityDatabaseSupport.RUNTIME);r.add("spring.datasource.password",POSTGRES::getPassword);
        r.add("spring.flyway.url",() -> POSTGRES.getJdbcUrl().split("\\?",2)[0]);
        r.add("spring.flyway.user",() -> IdentityDatabaseSupport.MIGRATION);r.add("spring.flyway.password",POSTGRES::getPassword);
    }
    @Autowired StaffAuthentication authentication;
    @Autowired PasswordService passwords;
    @Autowired StoreOwnershipReader storeFacts;
    @Autowired StoreScopeGuard storeGuard;
    @Autowired CurrentPrincipalProvider currentPrincipal;
    @Autowired EntityManagerFactory factory;
    @Autowired ApplicationContext spring;
    @Autowired Flyway flyway;
    @Autowired JdbcTemplate runtime;
    JdbcTemplate owner;
    static final String TEST_PASSWORD="临时技术密码-OnlyContainer😀";
    Map<String,String> commandEnvironment() {
        return Map.of("PET_BOOTSTRAP_DATABASE_URL",POSTGRES.getJdbcUrl().split("\\?",2)[0],
            "PET_BOOTSTRAP_DATABASE_USERNAME",IdentityDatabaseSupport.BOOTSTRAP,"PET_BOOTSTRAP_DATABASE_PASSWORD",POSTGRES.getPassword());
    }
    @BeforeEach void reset() {
        owner=new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword()));
        owner.execute("DROP TRIGGER IF EXISTS p05_fail_permission ON public.identity_role_permission");
        owner.execute("TRUNCATE public.identity_employee_store,public.identity_role_permission,public.identity_employee_role,public.identity_role,public.identity_employee,public.platform_store,public.platform_tenant");
        assertTrue(TenantContextHolder.current().isEmpty());
    }
    @AfterEach void record(TestInfo info) throws Exception {
        assertTrue(TenantContextHolder.current().isEmpty());
        var observation=new LinkedHashMap<String,Object>();
        observation.put("test",info.getTestMethod().orElseThrow().getName());observation.put("serverVersion",owner.queryForObject("show server_version",String.class));
        observation.put("runtime",runtime.queryForObject("select current_user",String.class));
        observation.put("counts",counts());observation.put("outsideTransactionTenant",runtime.queryForObject("select coalesce(nullif(current_setting('pet.tenant_id',true),''),'EMPTY')",String.class));
        Path directory=Path.of("target/p05-01-observations");Files.createDirectories(directory);
        Files.writeString(directory.resolve(info.getTestMethod().orElseThrow().getName()+".json"),tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(observation));
    }
    @AfterAll static void stop() { POSTGRES.stop(); }
    Map<String,Long> counts() {
        var counts=new TreeMap<String,Long>();
        for(String table:List.of("platform_tenant","platform_store","identity_employee","identity_role","identity_employee_role","identity_role_permission","identity_employee_store")) counts.put(table,owner.queryForObject("select count(*) from public."+table,Long.class));
        return counts;
    }
    BootstrapResult initialize(String code,String login,boolean store) {
        try(var input=new BootstrapRequest(code,"临时租户",login,TEST_PASSWORD.toCharArray(),store?"first-store":null,store?"临时门店":null)) {
            return new IdentityBootstrap(new BootstrapJdbc(commandEnvironment()),passwords).initialize(input);
        }
    }
    StaffIdentity staff(BootstrapResult result) { return authentication.loadForSession(result.tenantId(),result.employeeId()).orElseThrow(); }
    <T> T execute(StaffIdentity staff,String permission,Supplier<T> action) {
        var p=new CurrentPrincipal(PrincipalType.STAFF,staff.employeeId(),staff.tenantId(),UUID.randomUUID(),staff.authorizationVersion(),staff.grants().keySet(),staff.authorizedStoreIds(),staff.grants());
        return new TrustedTenantExecutor(() -> Optional.of(p)).execute(permission,action);
    }
    String passwordHash(UUID employee) { return owner.queryForObject("select password_hash from public.identity_employee where id=?",String.class,employee); }
    void failedSql(String sql,Object...args) { assertThrows(org.springframework.dao.DataAccessException.class,() -> owner.update(sql,args)); }
    UUID addStore(UUID tenant,String code) {
        UUID id=UUID.randomUUID();owner.update("insert into public.platform_store(id,tenant_id,code,name,status) values (?,?,?,'临时门店','ACTIVE')",id,tenant,code);return id;
    }
    UUID addRole(BootstrapResult staff,String code,String status,Map<String,String> grants) {
        UUID id=UUID.randomUUID();owner.update("insert into public.identity_role(id,tenant_id,code,name,status) values (?,?,?,'临时角色',?)",id,staff.tenantId(),code,status);
        owner.update("insert into public.identity_employee_role(id,tenant_id,employee_id,role_id) values (?,?,?,?)",UUID.randomUUID(),staff.tenantId(),staff.employeeId(),id);
        grants.forEach((permission,scope) -> owner.update("insert into public.identity_role_permission(id,tenant_id,role_id,permission_code,scope_type) values (?,?,?,?,?)",UUID.randomUUID(),staff.tenantId(),id,permission,scope));return id;
    }
    @Test void formalMigrationRepeatedAndJpaValidationWithSevenModels() {
        assertEquals(7,factory.getMetamodel().getEntities().size());assertEquals(1,flyway.info().applied().length);
        assertEquals(0,flyway.migrate().migrationsExecuted);assertTrue(flyway.validateWithResult().validationSuccessful);
        assertEquals(7,owner.queryForObject("select count(*) from pg_class c join pg_namespace n on n.oid=c.relnamespace where n.nspname='public' and c.relname in ('platform_tenant','platform_store','identity_employee','identity_role','identity_employee_role','identity_role_permission','identity_employee_store') and c.relrowsecurity and c.relforcerowsecurity and c.relowner='pet_migrator'::regrole",Integer.class));
    }
    @Test void tenantNormalizationUniqueAndDifferentTenantsShareLogin() {
        var a=initialize(" Tenant-A "," ADMIN ",false);var b=initialize("tenant-b","admin",false);
        assertEquals("tenant-a",owner.queryForObject("select code from platform_tenant where id=?",String.class,a.tenantId()));
        assertThrows(IllegalStateException.class,() -> initialize("TENANT-A","other",false));
        assertEquals(2,counts().get("identity_employee"));
        assertEquals(a.employeeId(),authentication.verifyCredentials("TENANT-A","Admin",TEST_PASSWORD.toCharArray()).orElseThrow().employeeId());
        assertEquals(b.employeeId(),authentication.verifyCredentials("tenant-b","admin",TEST_PASSWORD.toCharArray()).orElseThrow().employeeId());
        failedSql("insert into platform_tenant(id,code,name,status) values (?,'Tenant-X','技术','ACTIVE')",UUID.randomUUID());
    }
    @Test void sameTenantLoginUniquenessAndCanonicalDatabaseConstraint() {
        var a=initialize("tenant-a","admin",false);
        failedSql("insert into identity_employee(id,tenant_id,login_name,display_name,status,password_hash) select ?,tenant_id,login_name,display_name,status,password_hash from identity_employee where id=?",UUID.randomUUID(),a.employeeId());
        failedSql("update identity_employee set login_name='Admin' where id=?",a.employeeId());
        assertEquals(1,counts().get("identity_employee"));
    }
    @Test void roleAndStoreCompositeReferencesRejectForeignTenantAndRestrictDelete() {
        var a=initialize("tenant-a","admin",true);var b=initialize("tenant-b","admin",true);
        var role=owner.queryForObject("select id from identity_role where tenant_id=?",UUID.class,b.tenantId());
        failedSql("insert into identity_employee_role(id,tenant_id,employee_id,role_id) values (?,?,?,?)",UUID.randomUUID(),a.tenantId(),a.employeeId(),role);
        failedSql("insert into identity_employee_store(id,tenant_id,employee_id,store_id) values (?,?,?,?)",UUID.randomUUID(),a.tenantId(),a.employeeId(),b.storeId());
        failedSql("insert into identity_role_permission(id,tenant_id,role_id,permission_code,scope_type) values (?,?,?,'identity:user:list','TENANT')",UUID.randomUUID(),a.tenantId(),role);
        failedSql("delete from platform_tenant where id=?",a.tenantId());
        assertEquals(2,counts().get("identity_employee_role"));assertEquals(2,counts().get("identity_employee_store"));
    }
    @Test void runtimeRlsNoContextNoForeignTenantAndCredentialColumnDenied() throws Exception {
        var a=initialize("tenant-a","admin",true);initialize("tenant-b","admin",true);
        assertEquals(0,runtime.queryForObject("select count(*) from platform_store",Integer.class));
        try(var c=DriverManager.getConnection(POSTGRES.getJdbcUrl(),IdentityDatabaseSupport.RUNTIME,POSTGRES.getPassword())) {
            c.setAutoCommit(false);
            try(var s=c.prepareStatement("select set_config('pet.tenant_id',?,true)")) { s.setString(1,a.tenantId().toString());s.execute(); }
            try(var s=c.createStatement();var r=s.executeQuery("select count(*) from platform_store")) { r.next();assertEquals(1,r.getInt(1)); }
            c.rollback();
            for(String sql:List.of("select password_hash from identity_employee","SET ROLE pet_migrator","SET ROLE pet_bootstrap","SET ROLE pet_auth_owner","SET ROLE pet_bootstrap_owner","truncate platform_store","alter table platform_store disable row level security")) {
                assertThrows(SQLException.class,() -> { try(var s=c.createStatement()) { s.execute(sql); } });c.rollback();
            }
        }
        assertEquals(2,counts().get("platform_store"));
    }
    @Test void formalRlsWithCheckRejectsForeignInsertAndTenantMoveWithIndependentRead() throws Exception {
        var a=initialize("tenant-a","admin",true);var b=initialize("tenant-b","admin",true);
        // 只在临时容器增加DML技术探针权限，验证正式策略；正式runtime仍仅SELECT。
        owner.execute("GRANT INSERT,UPDATE ON platform_store TO pet_runtime");
        try(var c=DriverManager.getConnection(POSTGRES.getJdbcUrl(),IdentityDatabaseSupport.RUNTIME,POSTGRES.getPassword())) {
            c.setAutoCommit(false);
            for(String operation:List.of("INSERT","UPDATE")) {
                try(var q=c.prepareStatement("select set_config('pet.tenant_id',?,true)")) { q.setString(1,a.tenantId().toString());q.execute(); }
                if(operation.equals("INSERT")) {
                    try(var q=c.prepareStatement("insert into platform_store(id,tenant_id,code,name,status) values (?,?,'forged','技术','ACTIVE')")) {
                        q.setObject(1,UUID.randomUUID());q.setObject(2,b.tenantId());assertEquals("42501",assertThrows(SQLException.class,q::executeUpdate).getSQLState());
                    }
                } else {
                    try(var q=c.prepareStatement("update platform_store set tenant_id=? where id=?")) {
                        q.setObject(1,b.tenantId());q.setObject(2,a.storeId());assertEquals("42501",assertThrows(SQLException.class,q::executeUpdate).getSQLState());
                    }
                }
                c.rollback();
            }
        } finally { owner.execute("REVOKE INSERT,UPDATE ON platform_store FROM pet_runtime"); }
        assertEquals(2,counts().get("platform_store"));assertEquals(a.tenantId(),owner.queryForObject("select tenant_id from platform_store where id=?",UUID.class,a.storeId()));
    }
    @Test void authenticationFunctionsOwnedByMinimalNoLoginRolesAndNotPublic() {
        var functions=owner.queryForList("select p.proname,p.prosecdef,p.proconfig,r.rolname,r.rolcanlogin,r.rolsuper,r.rolbypassrls,exists (select 1 from aclexplode(p.proacl) acl where acl.grantee=0 and acl.privilege_type='EXECUTE') as public_execute from pg_proc p join pg_namespace n on n.oid=p.pronamespace join pg_roles r on r.oid=p.proowner where n.nspname='pet_identity'");
        assertEquals(3,functions.size());
        for(var f:functions) { assertEquals(true,f.get("prosecdef"));assertEquals(false,f.get("rolcanlogin"));assertEquals(false,f.get("rolsuper"));assertEquals(false,f.get("rolbypassrls"));assertEquals(false,f.get("public_execute"));assertTrue(f.get("proconfig").toString().contains("pg_catalog, pg_temp")); }
        assertEquals(false,runtime.queryForObject("select has_function_privilege(current_user,'pet_identity.bootstrap_tenant(text,text,text,text,text,text)','EXECUTE')",Boolean.class));
    }
    @Test void authenticationBeforeContextReturnsMinimalCredentialWithoutSerialization() throws Exception {
        initialize("tenant-a","admin",false);
        var candidate=authentication.findCandidate("tenant-a","admin").orElseThrow();
        assertTrue(TenantContextHolder.current().isEmpty());assertTrue(candidate.accepts(passwords,TEST_PASSWORD.toCharArray()));
        assertFalse(candidate.toString().contains("admin"));assertFalse(candidate.toString().contains("pbkdf2"));
        var mapper=tools.jackson.databind.json.JsonMapper.builder().disable(tools.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS).build();
        assertEquals("{}",mapper.writeValueAsString(candidate));
        assertTrue(authentication.findCandidate("unknown","admin").isEmpty());assertTrue(authentication.findCandidate("tenant-a","unknown").isEmpty());
        assertTrue(authentication.findCandidate("tenant-a' OR true--","admin").isEmpty());
        assertEquals(7,candidateColumns());
        assertTrue(authentication.verifyCredentials("tenant-a","admin","错误技术密码-input".toCharArray()).isEmpty());
    }
    int candidateColumns() throws Exception {
        try(var c=DriverManager.getConnection(POSTGRES.getJdbcUrl(),IdentityDatabaseSupport.RUNTIME,POSTGRES.getPassword());var q=c.prepareStatement("select * from pet_identity.authentication_candidate(?,?)")) {
            q.setString(1,"tenant-a");q.setString(2,"admin");try(var r=q.executeQuery()) { return r.getMetaData().getColumnCount(); }
        }
    }
    @Test void authenticationCannotBeHijackedByTemporaryShadowTables() throws Exception {
        initialize("tenant-a","admin",false);
        try(var c=DriverManager.getConnection(POSTGRES.getJdbcUrl(),IdentityDatabaseSupport.RUNTIME,POSTGRES.getPassword());var s=c.createStatement()) {
            s.execute("create temporary table platform_tenant(id uuid,code text,status text)");
            s.execute("create temporary table identity_employee(id uuid,tenant_id uuid,login_name text,password_hash text)");
            try(var q=c.prepareStatement("select employee_id from pet_identity.authentication_candidate(?,?)")) {
                q.setString(1,"tenant-a");q.setString(2,"admin");try(var r=q.executeQuery()) { assertTrue(r.next());assertNotNull(r.getObject(1));assertFalse(r.next()); }
            }
        }
    }
    @Test void plaintextNeverStoredAndPasswordInputLifecycleCleared() {
        var a=initialize("tenant-a","admin",false);var encoded=passwordHash(a.employeeId());
        assertFalse(encoded.contains(TEST_PASSWORD));assertTrue(passwords.matches(TEST_PASSWORD.toCharArray(),encoded));
        assertEquals(0,owner.queryForObject("select count(*) from information_schema.columns where table_schema='public' and column_name in ('password','initial_password','plain_password')",Integer.class));
        assertFalse(spring.getBeansOfType(BootstrapWriter.class).size()>0);assertFalse(spring.getBeansOfType(IdentityBootstrap.class).size()>0);
    }
    @Test void storeProductionBeanAndGuardReadOfficialTenantAndStatus() {
        var a=initialize("tenant-a","admin",true);var b=initialize("tenant-b","admin",true);var identity=staff(a);
        assertEquals(1,spring.getBeansOfType(StoreOwnershipReader.class).size());assertInstanceOf(PostgresStoreOwnershipReader.class,storeFacts);
        execute(identity,"platform:store:list",() -> {
            assertEquals(Optional.of(a.tenantId()),storeFacts.findTenantId(a.storeId()));storeGuard.requireStore(a.storeId());
            assertThrows(TenantAccessDeniedException.class,() -> storeGuard.requireStore(b.storeId()));
            assertThrows(TenantAccessDeniedException.class,() -> storeGuard.requireStore(UUID.randomUUID()));return null;
        });
        owner.update("update platform_store set status='DISABLED' where id=?",a.storeId());
        execute(identity,"platform:store:list",() -> { assertTrue(storeFacts.findTenantId(a.storeId()).isEmpty());assertThrows(TenantAccessDeniedException.class,() -> storeGuard.requireStore(a.storeId()));return null; });
        assertTrue(staff(a).authorizedStoreIds().isEmpty());assertThrows(TenantAccessDeniedException.class,() -> storeFacts.findTenantId(a.storeId()));
    }
    @Test void sameTenantUnauthorizedStoreDeniedAfterFactFound() {
        var a=initialize("tenant-a","admin",true);UUID other=addStore(a.tenantId(),"unassigned");
        execute(staff(a),"platform:store:list",() -> { assertEquals(Optional.of(a.tenantId()),storeFacts.findTenantId(other));assertThrows(TenantAccessDeniedException.class,() -> storeGuard.requireStore(other));return null; });
    }
    @Test void unavailableStoreSourceIsDependencyFailureWithoutGuardRecursion() {
        var a=initialize("tenant-a","admin",true);owner.execute("REVOKE SELECT ON public.platform_store FROM pet_runtime");
        try {
            execute(staff(a),"platform:store:list",() -> { assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE,assertThrows(BusinessException.class,() -> storeGuard.requireStore(a.storeId())).error().code());return null; });
        } finally { owner.execute("GRANT SELECT ON public.platform_store TO pet_runtime"); }
    }
    @Test void bootstrapRelationsCompleteAndOptionalStoreReallyOptional() {
        var a=initialize("tenant-a","admin",true);
        assertEquals(Map.of("platform_tenant",1L,"platform_store",1L,"identity_employee",1L,"identity_role",1L,"identity_employee_role",1L,"identity_role_permission",9L,"identity_employee_store",1L),counts());
        assertEquals(PermissionCatalog.ADMIN_PERMISSIONS,staff(a).grants().keySet());assertEquals(Set.of(a.storeId()),staff(a).authorizedStoreIds());
        assertTrue(staff(a).grants().values().stream().allMatch(scope -> scope.types().equals(Set.of(DataScopeType.TENANT))));
        var b=initialize("tenant-b","admin",false);assertNull(b.storeId());assertTrue(staff(b).authorizedStoreIds().isEmpty());assertEquals(1,counts().get("platform_store"));
    }
    @Test void bootstrapIntermediateFailureRollsBackAllTablesIndependentConnection() {
        owner.execute("create or replace function public.p05_fail_permission() returns trigger language plpgsql as $$ begin raise exception '技术故障注入'; end $$");
        owner.execute("create trigger p05_fail_permission before insert on identity_role_permission for each row execute function public.p05_fail_permission()");
        assertThrows(IllegalStateException.class,() -> initialize("rollback-a","admin",true));
        assertTrue(counts().values().stream().allMatch(value -> value==0));
        assertEquals(0,owner.queryForObject("select count(*) from platform_tenant where code='rollback-a'",Integer.class));
    }
    @Test void bootstrapRepeatDoesNotChangeHashPermissionsOrRepairHalfState() {
        var a=initialize("tenant-a","admin",true);String old=passwordHash(a.employeeId());
        owner.update("delete from identity_role_permission where tenant_id=? and permission_code='identity:user:update'",a.tenantId());
        assertThrows(IllegalStateException.class,() -> initialize("TENANT-A","another-admin",true));
        assertTrue(old.equals(passwordHash(a.employeeId())));assertEquals(8,counts().get("identity_role_permission"));assertEquals(1,counts().get("identity_employee"));
    }
    @Test void concurrentBootstrapHasExactlyOneCompleteAdministrator() throws Exception {
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            var tasks=new ArrayList<Future<Boolean>>();
            for(int i=0;i<2;i++) tasks.add(pool.submit(() -> { start.await();try { initialize("same-tenant","admin",true);return true; } catch(IllegalStateException conflict) { return false; } }));
            start.countDown();int successes=0;for(var future:tasks) if(future.get(15,TimeUnit.SECONDS)) successes++;
            assertEquals(1,successes);assertEquals(1,counts().get("platform_tenant"));assertEquals(1,counts().get("identity_employee"));assertEquals(1,counts().get("identity_employee_role"));assertEquals(9,counts().get("identity_role_permission"));
        } finally { pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
    }
    @Test void disabledTenantEmployeeRoleAndStoreAreReflectedByAuthorityLoad() {
        var a=initialize("tenant-a","admin",true);
        owner.update("update identity_role set status='DISABLED' where tenant_id=?",a.tenantId());assertTrue(staff(a).grants().isEmpty());
        owner.update("update identity_employee set status='DISABLED',security_version=security_version+1 where id=?",a.employeeId());
        assertTrue(authentication.loadForSession(a.tenantId(),a.employeeId()).isEmpty());assertTrue(authentication.verifyCredentials("tenant-a","admin",TEST_PASSWORD.toCharArray()).isEmpty());
        owner.update("update identity_employee set status='ACTIVE' where id=?",a.employeeId());
        owner.update("update platform_tenant set status='DISABLED',security_version=security_version+1 where id=?",a.tenantId());
        assertTrue(authentication.findCandidate("tenant-a","admin").isEmpty());assertTrue(authentication.loadForSession(a.tenantId(),a.employeeId()).isEmpty());
    }
    @Test void perPermissionRoleUnionDoesNotEnlargeOtherPermissions() {
        var a=initialize("tenant-a","admin",true);owner.update("delete from identity_employee_role where tenant_id=?",a.tenantId());
        addRole(a,"reader","ACTIVE",Map.of("identity:user:list","TENANT","identity:user:update","STORES"));
        addRole(a,"self","ACTIVE",Map.of("identity:user:update","SELF"));
        addRole(a,"disabled","DISABLED",Map.of("identity:user:update","TENANT"));
        var identity=staff(a);assertEquals(Set.of(DataScopeType.TENANT),identity.grants().get("identity:user:list").types());
        assertEquals(Set.of(DataScopeType.STORES,DataScopeType.SELF),identity.grants().get("identity:user:update").types());
        assertEquals(Set.of(a.storeId()),identity.grants().get("identity:user:update").storeIds());
        assertThrows(UnsupportedOperationException.class,() -> identity.grants().clear());
    }
    @Test void emptyStoresAndUnknownPermissionFailClosed() {
        var a=initialize("tenant-a","admin",false);owner.update("delete from identity_employee_role where tenant_id=?",a.tenantId());
        addRole(a,"stores","ACTIVE",Map.of("identity:user:list","STORES"));assertTrue(staff(a).grants().get("identity:user:list").storeIds().isEmpty());
        addRole(a,"unknown","ACTIVE",Map.of("future:business:unapproved","TENANT"));
        assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE,assertThrows(BusinessException.class,() -> staff(a)).error().code());
    }
    @Test void noRealSessionDoesNotCreatePrincipalAndBootstrapNotSpringBean() {
        assertTrue(currentPrincipal.currentPrincipal().isEmpty());initialize("tenant-a","admin",false);
        assertTrue(currentPrincipal.currentPrincipal().isEmpty());assertTrue(spring.getBeansOfType(BootstrapWriter.class).isEmpty());
        assertTrue(spring.getBeansOfType(IdentityBootstrap.class).isEmpty());
    }
    @Test void explicitPackagedCommandsMigrateBootstrapAndConflictWithSafeOutput() throws Exception {
        String migration=runCommand("migrate",null,0);assertTrue(migration.contains("本次执行数量=0"));
        String[] args={"bootstrap","--tenant-code","command-tenant","--tenant-name","临时命令租户","--admin-login","admin","--password-stdin"};
        String output=runCommand(args,TEST_PASSWORD+"\n",0);assertTrue(output.contains("初始化成功"));
        String repeated=runCommand(args,"另一临时技术密码-input\n",2);assertTrue(repeated.contains("未修改密码或权限"));
        assertFalse(output.contains(TEST_PASSWORD));assertFalse(repeated.contains("pbkdf2"));
        assertEquals(1,counts().get("identity_employee"));assertEquals(9,counts().get("identity_role_permission"));
        Files.writeString(Path.of("target/p05-01-command-output.txt"),migration+output+repeated);
    }
    String runCommand(String command,String input,int expected) throws Exception { return runCommand(new String[]{command},input,expected); }
    String runCommand(String[] args,String input,int expected) throws Exception {
        var argv=new ArrayList<String>();argv.add("../../scripts/backend-identity.sh");argv.addAll(Arrays.asList(args));
        var builder=new ProcessBuilder(argv).redirectErrorStream(true);builder.environment().putAll(commandEnvironment());
        builder.environment().put("JAVA_HOME",System.getProperty("java.home"));
        builder.environment().put("PET_MIGRATION_DATABASE_URL",POSTGRES.getJdbcUrl().split("\\?",2)[0]);
        builder.environment().put("PET_MIGRATION_DATABASE_USERNAME",IdentityDatabaseSupport.MIGRATION);builder.environment().put("PET_MIGRATION_DATABASE_PASSWORD",POSTGRES.getPassword());
        var process=builder.start();try(var stream=process.getOutputStream()) { if(input!=null) stream.write(input.getBytes(StandardCharsets.UTF_8)); }
        assertTrue(process.waitFor(20,TimeUnit.SECONDS));String output=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        assertEquals(expected,process.exitValue(),output);return output;
    }
    @Test void commandRejectsPasswordArgumentAndMissingConsoleBeforeDatabase() {
        var out=new ByteArrayOutputStream();var error=new ByteArrayOutputStream();
        assertEquals(2,BootstrapCommand.run(new String[]{"--password","DO-NOT-OUTPUT"},Map.of(),InputStream.nullInputStream(),new PrintStream(out),new PrintStream(error)));
        assertFalse(error.toString(StandardCharsets.UTF_8).contains("DO-NOT-OUTPUT"));assertTrue(counts().values().stream().allMatch(v -> v==0));
    }
    @Test void requestClonesAndClearsPasswordWithoutSerializingIt() throws Exception {
        char[] input=TEST_PASSWORD.toCharArray();
        var request=new BootstrapRequest("tenant-a","临时租户","admin",input,null,null);
        var field=BootstrapRequest.class.getDeclaredField("password");field.setAccessible(true);char[] owned=(char[])field.get(request);
        input[0]='X';assertNotEquals(input[0],owned[0]);
        request.close();for(char value:owned) assertEquals('\0',value);
        assertFalse(request.toString().contains(TEST_PASSWORD));
    }
    @Test void runtimeStartupCheckRejectsCredentialAccessAndBootstrapMembership() {
        var validator=spring.getBean(com.pet.platform.identity.infrastructure.IdentityRuntimePermissions.class);
        owner.execute("GRANT SELECT(password_hash) ON identity_employee TO pet_runtime");
        try { assertThrows(IllegalStateException.class,validator::afterPropertiesSet); }
        finally { owner.execute("REVOKE SELECT(password_hash) ON identity_employee FROM pet_runtime"); }
        owner.execute("GRANT pet_bootstrap TO p05_probe_runtime WITH INHERIT FALSE, SET TRUE");
        try { assertThrows(IllegalStateException.class,validator::afterPropertiesSet); }
        finally { owner.execute("REVOKE pet_bootstrap FROM p05_probe_runtime"); }
        assertDoesNotThrow(validator::afterPropertiesSet);
    }
    @Test void bootstrapLoginCannotReadTablesOrExecuteAuthentication() throws Exception {
        try(var c=DriverManager.getConnection(POSTGRES.getJdbcUrl(),IdentityDatabaseSupport.BOOTSTRAP,POSTGRES.getPassword());var s=c.createStatement()) {
            s.execute("SET ROLE pet_bootstrap");
            for(String sql:List.of("select * from platform_tenant","select * from identity_employee","select * from pet_identity.authentication_candidate('tenant-a','admin')","SET ROLE pet_migrator","SET ROLE pet_bootstrap_owner")) assertThrows(SQLException.class,() -> s.execute(sql));
        }
    }
    @Test void bootstrapCommandRejectsPrivilegedOrOrdinaryRuntimeLogin() {
        for(String role:List.of(IdentityDatabaseSupport.RUNTIME,POSTGRES.getUsername())) {
            var env=new HashMap<>(commandEnvironment());env.put("PET_BOOTSTRAP_DATABASE_USERNAME",role);
            assertThrows(IllegalStateException.class,() -> new BootstrapJdbc(env).initialize("wrong-role","临时租户","admin",passwords.hash(TEST_PASSWORD.toCharArray()),null,null));
        }
        assertTrue(counts().values().stream().allMatch(v -> v==0));
    }
}
