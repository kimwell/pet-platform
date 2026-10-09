package com.pet.testing.identity;

import com.pet.platform.Application;
import com.pet.platform.identity.application.*;
import com.pet.platform.identity.application.bootstrap.*;
import com.pet.platform.identity.application.employee.*;
import com.pet.platform.identity.infrastructure.bootstrap.*;
import com.pet.platform.identity.infrastructure.session.*;
import com.pet.platform.shared.security.*;
import com.pet.platform.shared.tenancy.TenantContextHolder;
import com.pet.testing.*;
import jakarta.persistence.EntityManagerFactory;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.*;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** 真实HTTP/正式认证/迁移/受限PG/Redis。操作者由正式bootstrap建立；目标和授权反例是隔离SQL技术夹具。 */
@SpringBootTest(classes=Application.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"springdoc.api-docs.enabled=true","spring.jpa.properties.hibernate.generate_statistics=true",
                "logging.level.org.hibernate.stat=OFF","logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF",
                "spring.datasource.hikari.maximum-pool-size=1","spring.datasource.hikari.minimum-idle=1"})
@ActiveProfiles("test") @Import(EmployeeDirectoryIT.Gateway.class) @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmployeeDirectoryIT extends PostgresIntegrationSupport {
    static final String PATH="/api/admin/identity/users", APP="wx0000000000000001";
    @LocalServerPort int port;
    @Autowired PasswordService passwords;
    @Autowired AuthenticationRedis redis;
    @Autowired CurrentPrincipalProvider provider;
    @Autowired EmployeeDirectory directory;
    @Autowired EntityManagerFactory factory;
    @Autowired JsonMapper json;
    JdbcTemplate owner;
    final HttpClient client=HttpClient.newHttpClient();
    String password=UUID.randomUUID()+" 临时技术😀",code,foreignCode;
    BootstrapResult a,b;
    UUID role,s2,same,other,multi,none,disabled,literal;
    final List<Map<String,Object>> observations=new ArrayList<>();
    @DynamicPropertySource static void wechat(DynamicPropertyRegistry r) {
        r.add("pet.wechat.enabled",()->"true"); r.add("PET_WECHAT_P07_IT_SECRET",()->"OnlyTestSecretInput1234567890");
        r.add("pet.wechat.applications.main.version",()->"1"); r.add("pet.wechat.applications.main.app-id",()->APP);
        r.add("pet.wechat.applications.main.secret-property",()->"PET_WECHAT_P07_IT_SECRET");
        r.add("pet.wechat.applications.main.timeout-millis",()->"1000");r.add("pet.wechat.applications.main.exchanges-per-minute",()->"20");
        r.add("pet.wechat.entries.main.enabled",()->"true"); r.add("pet.wechat.entries.main.application",()->"main");
        r.add("pet.wechat.entries.main.tenant-codes[0]",()->"p07-customer");
    }
    @TestConfiguration(proxyBeanMethods=false) static class Gateway {
        @Bean @Primary com.pet.platform.customeridentity.application.WechatMiniProgramGateway boundary() {
            return (config,code) -> new com.pet.platform.customeridentity.application.WechatMiniProgramGateway.Identity(config.appId(),code,null);
        }
    }
    @BeforeAll void setup() {
        owner=new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword()));
        assertInstanceOf(SessionPrincipalProvider.class,provider);
        initialize("p07-customer");
    }
    Map<String,String> environment() { return Map.of("PET_BOOTSTRAP_DATABASE_URL",POSTGRES.getJdbcUrl().split("\\?",2)[0],
            "PET_BOOTSTRAP_DATABASE_USERNAME",IdentityDatabaseSupport.BOOTSTRAP,"PET_BOOTSTRAP_DATABASE_PASSWORD",POSTGRES.getPassword()); }
    BootstrapResult initialize(String code) {
        try(var request=new BootstrapRequest(code,"员工读取技术租户","admin",password.toCharArray(),"store","技术门店")) {
            return new IdentityBootstrap(new BootstrapJdbc(environment()),passwords).initialize(request);
        }
    }
    @BeforeEach void facts() {
        observations.clear();
        var keys=redis.stringRedisTemplate.keys("pet:test:auth:*"); if(keys!=null && !keys.isEmpty()) redis.stringRedisTemplate.delete(keys);
        code="e"+UUID.randomUUID().toString().replace("-","").substring(0,20);
        foreignCode="f"+UUID.randomUUID().toString().replace("-","").substring(0,20);
        a=initialize(code);b=initialize(foreignCode);
        role=owner.queryForObject("select id from identity_role where tenant_id=?",UUID.class,a.tenantId());
        s2=UUID.randomUUID(); owner.update("insert into platform_store(id,tenant_id,code,name,status) values(?,?,?,'第二技术门店','ACTIVE')",s2,a.tenantId(),"other");
        String hash=owner.queryForObject("select password_hash from identity_employee where id=?",String.class,a.employeeId());
        same=target("same","相同姓名","ACTIVE",hash,a.storeId());
        other=target("other","相同姓名","ACTIVE",hash,s2);
        multi=target("multi","相同姓名","ACTIVE",hash,a.storeId(),s2);
        none=target("none","相同姓名","ACTIVE",hash);
        disabled=target("disabled","停用员工","DISABLED",hash,a.storeId());
        literal=target("literal","A%_\\AbC","ACTIVE",hash,a.storeId());
        owner.update("update identity_employee set created_at='2026-10-09T00:00:00Z',updated_at='2026-10-09T00:00:00Z' where tenant_id=?",a.tenantId());
    }
    UUID target(String login,String name,String status,String hash,UUID...stores) {
        UUID id=UUID.randomUUID(); owner.update("insert into identity_employee(id,tenant_id,login_name,display_name,status,password_hash) values(?,?,?,?,?,?)",id,a.tenantId(),login,name,status,hash);
        for(UUID store:stores) owner.update("insert into identity_employee_store(id,tenant_id,employee_id,store_id) values(?,?,?,?)",UUID.randomUUID(),a.tenantId(),id,store);
        return id;
    }
    void grants(String permission,String...scopes) {
        owner.update("delete from identity_role_permission where tenant_id=? and role_id=? and permission_code=?",a.tenantId(),role,permission);
        for(String scope:scopes) owner.update("insert into identity_role_permission(id,tenant_id,role_id,permission_code,scope_type) values(?,?,?,?,?)",UUID.randomUUID(),a.tenantId(),role,permission,scope);
        owner.update("update identity_employee set authorization_version=authorization_version+1 where id=?",a.employeeId());
    }
    HttpResponse<String> request(String method,String path,String body,String...headers)throws Exception {
        var q=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(20));
        for(int i=0;i<headers.length;i+=2) q.header(headers[i],headers[i+1]);
        if(body!=null) q.header("Content-Type","application/json");
        return client.send(q.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    JsonNode data(HttpResponse<String> response) { return json.readTree(response.body()).path("data"); }
    String token(String tenant)throws Exception {
        var r=request("POST","/api/admin/auth/token/login",json.writeValueAsString(Map.of("tenantCode",tenant,"loginName","admin","password",password)));
        assertEquals(200,r.statusCode());return data(r).path("token").path("value").asText();
    }
    String token()throws Exception { return token(code); }
    HttpResponse<String> get(String token,String suffix)throws Exception {
        var r=request("GET",PATH+suffix,null,"X-Staff-Token","Bearer "+token);
        observations.add(Map.of("path",PATH+suffix,"status",r.statusCode(),"body",json.readTree(r.body())));
        assertEquals("no-store",r.headers().firstValue("Cache-Control").orElseThrow());
        assertFalse(r.body().contains(token));assertFalse(r.body().contains(password));return r;
    }
    void error(HttpResponse<String> r,int status,String error) {
        assertEquals(status,r.statusCode(),r.body());assertEquals(error,json.readTree(r.body()).path("error").path("code").asText());
    }
    Set<UUID> ids(HttpResponse<String> r,int total) {
        assertEquals(200,r.statusCode(),r.body());assertTrue(data(r).path("total").isString());assertEquals(Integer.toString(total),data(r).path("total").asText());
        var result=new HashSet<UUID>(); for(var row:data(r).path("items")) assertTrue(result.add(UUID.fromString(row.path("id").asText())));
        return result;
    }
    Set<UUID> all() { return Set.of(a.employeeId(),same,other,multi,none,disabled,literal); }
    Set<UUID> stores() { return Set.of(a.employeeId(),same,multi,disabled,literal); }
    @AfterEach void evidence(TestInfo info)throws Exception {
        assertTrue(TenantContextHolder.current().isEmpty());assertTrue(provider.currentPrincipal().isEmpty());
        Path dir=Path.of("target/p07-01-observations");Files.createDirectories(dir);
        Files.writeString(dir.resolve(info.getTestMethod().orElseThrow().getName()+".json"),json.writeValueAsString(observations));
    }
    @Test void anonymousListAndDetailRequireIdentity()throws Exception { error(request("GET",PATH,null),401,"AUTH_REQUIRED");error(request("GET",PATH+"/"+same,null),401,"AUTH_REQUIRED"); }
    @Test void missingIndependentReadPermissionsAre403()throws Exception {
        grants(EmployeeDirectory.LIST);String t=token();error(get(t,""),403,"PERMISSION_DENIED");error(get(t,"/"+a.employeeId()),403,"PERMISSION_DENIED");
        assertEquals(200,request("GET","/api/admin/auth/me",null,"X-Staff-Token","Bearer "+t).statusCode());
        assertEquals(200,request("POST","/api/admin/auth/logout",null,"X-Staff-Token","Bearer "+t).statusCode());
    }
    @Test void tenantListAndDetailContainOnlyThisTenant()throws Exception {
        assertEquals(1,new BootstrapJdbc(environment()).upgradeEmployeeRead(a.tenantId()));String t=token();assertEquals(all(),ids(get(t,""),7));
        for(UUID id:all()) assertEquals(id.toString(),data(get(t,"/"+id)).path("id").asText());
        error(get(t,"/"+b.employeeId()),404,"RESOURCE_NOT_FOUND");
    }
    @Test void selfListAndDetailMapToStaffSubjectId()throws Exception {
        grants(EmployeeDirectory.LIST,"SELF");grants(EmployeeDirectory.DETAIL,"SELF");String t=token();assertEquals(Set.of(a.employeeId()),ids(get(t,""),1));
        assertEquals(a.employeeId().toString(),data(get(t,"/"+a.employeeId())).path("id").asText());error(get(t,"/"+same),404,"RESOURCE_NOT_FOUND");
    }
    @Test void storesUseAnySharedActiveAuthorizedStoreWithoutRelationLeak()throws Exception {
        grants(EmployeeDirectory.LIST,"STORES");grants(EmployeeDirectory.DETAIL,"STORES");String t=token();assertEquals(stores(),ids(get(t,""),5));
        assertEquals(multi.toString(),data(get(t,"/"+multi)).path("id").asText());error(get(t,"/"+other),404,"RESOURCE_NOT_FOUND");error(get(t,"/"+none),404,"RESOURCE_NOT_FOUND");
        assertEquals(Set.of("id","loginName","displayName","status","createdAt","updatedAt"),data(get(t,"/"+multi)).propertyNames());
    }
    @Test void noAuthorizedStoresMeansEmptyStoresScope()throws Exception {
        grants(EmployeeDirectory.LIST,"STORES");grants(EmployeeDirectory.DETAIL,"STORES");owner.update("delete from identity_employee_store where employee_id=?",a.employeeId());
        String t=token();assertEquals(Set.of(),ids(get(t,""),0));error(get(t,"/"+a.employeeId()),404,"RESOURCE_NOT_FOUND");
    }
    @Test void storesAndSelfUnionIncludesNoStoreSelfOnly()throws Exception {
        grants(EmployeeDirectory.LIST,"STORES","SELF");grants(EmployeeDirectory.DETAIL,"STORES","SELF");owner.update("delete from identity_employee_store where employee_id=?",a.employeeId());
        String t=token();assertEquals(Set.of(a.employeeId()),ids(get(t,""),1));assertEquals(200,get(t,"/"+a.employeeId()).statusCode());error(get(t,"/"+none),404,"RESOURCE_NOT_FOUND");
    }
    @Test void tenantMixedWithOtherScopesNormalizesWithoutCrossTenant()throws Exception {
        grants(EmployeeDirectory.LIST,"TENANT","STORES","SELF");assertEquals(all(),ids(get(token(),""),7));
    }
    @Test void listScopeCannotEnlargeDetailOrSensitiveManagement()throws Exception {
        grants(EmployeeDirectory.LIST,"TENANT");grants(EmployeeDirectory.DETAIL,"SELF");grants("identity:user:reset-password","SELF");String t=token();
        assertEquals(all(),ids(get(t,""),7));error(get(t,"/"+multi),404,"RESOURCE_NOT_FOUND");
        var r=request("PUT",PATH+"/"+multi+"/password",json.writeValueAsString(Map.of("currentPassword",password,"newPassword",UUID.randomUUID()+"技术","version","0")),"X-Staff-Token","Bearer "+t);
        error(r,404,"RESOURCE_NOT_FOUND");
    }
    @Test void hiddenUnknownAndForeignDetailsHaveIdenticalErrors()throws Exception {
        grants(EmployeeDirectory.DETAIL,"SELF");String t=token();var hidden=get(t,"/"+same);var unknown=get(t,"/"+UUID.randomUUID());var foreign=get(t,"/"+b.employeeId());
        for(var r:List.of(hidden,unknown,foreign)) error(r,404,"RESOURCE_NOT_FOUND");
        assertEquals(json.readTree(hidden.body()).path("error"),json.readTree(unknown.body()).path("error"));assertEquals(json.readTree(hidden.body()).path("error"),json.readTree(foreign.body()).path("error"));
    }
    @Test void storeFilterNeverEnlargesCurrentScope()throws Exception {
        grants(EmployeeDirectory.LIST,"STORES");String t=token();assertEquals(stores(),ids(get(t,"?storeId="+a.storeId()),5));
        for(UUID id:List.of(s2,b.storeId(),UUID.randomUUID())) error(get(t,"?storeId="+id),404,"RESOURCE_NOT_FOUND");
        grants(EmployeeDirectory.LIST,"SELF");error(get(t,"?storeId="+a.storeId()),404,"RESOURCE_NOT_FOUND");
    }
    @Test void tenantStoreFilterStillChecksIdentityStoreCeiling()throws Exception {
        String t=token();error(get(t,"?storeId="+s2),404,"RESOURCE_NOT_FOUND");
        owner.update("insert into identity_employee_store(id,tenant_id,employee_id,store_id) values(?,?,?,?)",UUID.randomUUID(),a.tenantId(),a.employeeId(),s2);
        assertEquals(Set.of(a.employeeId(),other,multi),ids(get(t,"?storeId="+s2),3));
    }
    @Test void targetStatusDoesNotGrantOrRemoveReadVisibility()throws Exception {
        String t=token();assertEquals(Set.of(disabled),ids(get(t,"?status=DISABLED"),1));assertEquals(6,ids(get(t,"?status=ACTIVE"),6).size());
        grants(EmployeeDirectory.DETAIL,"TENANT");assertEquals("DISABLED",data(get(t,"/"+disabled)).path("status").asText());
    }
    @Test void inactiveStoreRevokesStoresVisibilityOnNextRequest()throws Exception {
        grants(EmployeeDirectory.LIST,"STORES");String t=token();assertEquals(stores(),ids(get(t,""),5));owner.update("update platform_store set status='DISABLED' where id=?",a.storeId());
        assertEquals(Set.of(),ids(get(t,""),0));error(get(t,"?storeId="+a.storeId()),404,"RESOURCE_NOT_FOUND");
    }
    @Test void keywordMatchesAccountAndNameIgnoringCaseLiterally()throws Exception {
        String t=token();assertEquals(Set.of(same),ids(get(t,"?keyword=SAME"),1));
        for(String query:List.of("%","_","\\","a%_\\abc")) assertEquals(Set.of(literal),ids(get(t,"?keyword="+URLEncoder.encode(query,java.nio.charset.StandardCharsets.UTF_8)),1));
        assertEquals(Set.of(),ids(get(t,"?keyword="+URLEncoder.encode("' OR 1=1 --",java.nio.charset.StandardCharsets.UTF_8)),0));
    }
    @Test void keywordLengthAndBlankBoundaries()throws Exception {
        String t=token();assertEquals(Set.of(),ids(get(t,"?keyword="+"x".repeat(100)),0));
        assertEquals(Set.of(),ids(get(t,"?keyword="+URLEncoder.encode("😀".repeat(100),java.nio.charset.StandardCharsets.UTF_8)),0));
        error(get(t,"?keyword="+URLEncoder.encode("😀".repeat(101),java.nio.charset.StandardCharsets.UTF_8)),422,"VALIDATION_FAILED");
        for(String suffix:List.of("?keyword=","?keyword=%20","?keyword="+"x".repeat(101))) error(get(t,suffix),422,"VALIDATION_FAILED");
    }
    @Test void illegalStatusIdUnknownAndRepeatedFiltersAre400()throws Exception {
        String t=token();for(String suffix:List.of("?status=active","?status=","?storeId=bad","?keyword=a&keyword=b","?status=ACTIVE&status=DISABLED","?storeId="+a.storeId()+"&storeId="+a.storeId(),"?tenantId="+b.tenantId(),"?sort=createdAt","/bad","/"+same+"?keyword=x")) error(get(t,suffix),400,"BAD_REQUEST");
    }
    @Test void invalidPaginationAndSortFollowFrozenErrors()throws Exception {
        String t=token();for(String query:List.of("page=0","page=-1","page=1.5","page=","page=1&page=2","pageSize=0","pageSize=101","pageSize=20&pageSize=20")) error(get(t,"?"+query),400,"PAGINATION_INVALID");
        for(String query:List.of("sortBy=passwordHash","sortBy=loginName;drop","sortOrder=asc","sortBy=id&sortOrder=ASC","sortBy=id&sortBy=id","sortBy=id&sortOrder=asc&sortOrder=desc")) error(get(t,"?"+query),400,"SORT_INVALID");
        error(get(t,"?page=2147483647&pageSize=100"),422,"RESULT_TOO_LARGE");
    }
    @Test void sameValueSortAndManyToManyPagesHaveStableUniqueIds()throws Exception {
        owner.update("insert into identity_employee_store(id,tenant_id,employee_id,store_id) values(?,?,?,?)",UUID.randomUUID(),a.tenantId(),a.employeeId(),s2);
        grants(EmployeeDirectory.LIST,"STORES");String t=token();var seen=new ArrayList<String>();
        for(int page=1;page<=3;page++) { var r=get(t,"?page="+page+"&pageSize=2&sortBy=createdAt&sortOrder=asc");ids(r,6);for(var row:data(r).path("items")) seen.add(row.path("id").asText()); }
        assertEquals(6,new HashSet<>(seen).size());var expected=seen.stream().sorted().toList();assertEquals(expected,seen);
        assertEquals(data(get(t,"?pageSize=2&sortBy=createdAt&sortOrder=asc")),data(get(t,"?pageSize=2&sortBy=createdAt&sortOrder=asc")));
        assertEquals(Set.of(same,other,multi),ids(get(t,"?keyword="+URLEncoder.encode("相同姓名",java.nio.charset.StandardCharsets.UTF_8)+"&sortBy=displayName&sortOrder=desc"),3));
    }
    @Test void emptyFilterAndBeyondLastPagePreserveTotalAndRequestedPage()throws Exception {
        String t=token();assertEquals(Set.of(),ids(get(t,"?keyword=absent"),0));var r=get(t,"?page=20&pageSize=2");assertEquals(Set.of(),ids(r,7));assertEquals(20,data(r).path("page").asInt());assertEquals(2,data(r).path("pageSize").asInt());
    }
    @Test void permissionAndRoleRemovalImmediatelyAffectNewRequests()throws Exception {
        grants(EmployeeDirectory.DETAIL,"TENANT");String t=token();assertEquals(all(),ids(get(t,""),7));assertEquals(200,get(t,"/"+multi).statusCode());
        grants(EmployeeDirectory.LIST);error(get(t,""),403,"PERMISSION_DENIED");assertEquals(200,get(t,"/"+multi).statusCode());
        owner.update("update identity_role set status='DISABLED' where id=?",role);error(get(t,"/"+multi),403,"PERMISSION_DENIED");
    }
    @Test void nextRequestRechecksStoreAssignmentEvenWithoutVersionBump()throws Exception {
        grants(EmployeeDirectory.LIST,"STORES");String t=token();assertEquals(stores(),ids(get(t,""),5));owner.update("delete from identity_employee_store where employee_id=?",a.employeeId());assertEquals(Set.of(),ids(get(t,""),0));
    }
    @Test void accountAndSingleConnectionReuseNeverMixTenantRows()throws Exception {
        String x=token(),y=token(foreignCode);
        try(var worker=Executors.newSingleThreadExecutor()) {
            worker.submit(()->{ for(int i=0;i<12;i++) {
                assertEquals(all(),ids(get(x,""),7));assertEquals(Set.of(b.employeeId()),ids(get(y,""),1));
                error(request("GET",PATH,null),401,"AUTH_REQUIRED");
            } return null; }).get(25,TimeUnit.SECONDS);
        }
        try(var c=DriverManager.getConnection(POSTGRES.getJdbcUrl(),IdentityDatabaseSupport.RUNTIME,POSTGRES.getPassword());var s=c.createStatement();var r=s.executeQuery("select id from identity_employee")) { assertFalse(r.next()); }
    }
    @Test void listAndDetailProjectionHaveNoNPlusOne()throws Exception {
        grants(EmployeeDirectory.DETAIL,"TENANT");String t=token();var stats=factory.unwrap(org.hibernate.SessionFactory.class).getStatistics();stats.clear();
        assertEquals(all(),ids(get(t,""),7));assertEquals(2,stats.getPrepareStatementCount());assertEquals(0,stats.getEntityLoadCount());stats.clear();
        assertEquals(200,get(t,"/"+multi).statusCode());assertEquals(1,stats.getPrepareStatementCount());assertEquals(0,stats.getEntityLoadCount());
    }
    @Test void validCustomerAndPlatformCredentialsCannotReadStaffDirectory()throws Exception {
        var customer=request("POST","/api/customer/auth/wechat/login",json.writeValueAsString(Map.of("tenantCode","p07-customer","entryId","main","code",UUID.randomUUID().toString())));
        assertEquals(200,customer.statusCode());String customerToken=data(customer).path("token").path("value").asText();
        for(String suffix:List.of("","/"+same)) {
            error(request("GET",PATH+suffix,null,"X-Customer-Token","Bearer "+customerToken),401,"AUTH_DOMAIN_MISMATCH");
            error(get(customerToken,suffix),401,"SESSION_EXPIRED");
        }
        owner.execute("truncate pet_control.platform_security_event,pet_control.platform_session_cleanup,pet_control.platform_bootstrap,pet_control.platform_permission,pet_control.platform_account");
        new PlatformBootstrap(new PlatformBootstrapJdbc(Map.of("PET_PLATFORM_BOOTSTRAP_DATABASE_URL",POSTGRES.getJdbcUrl().split("\\?",2)[0],
                "PET_PLATFORM_BOOTSTRAP_DATABASE_USERNAME",IdentityDatabaseSupport.PLATFORM_BOOTSTRAP,"PET_PLATFORM_BOOTSTRAP_DATABASE_PASSWORD",POSTGRES.getPassword())),passwords).initialize("root","平台技术管理员",password.toCharArray());
        var pre=request("GET","/api/platform/auth/csrf",null);String preCookie=cookie(pre,"pet_dev_platform_pre");
        var platform=request("POST","/api/platform/auth/login",json.writeValueAsString(Map.of("loginName","root","password",password)),"Cookie",preCookie,"X-CSRF-Token",data(pre).path("csrfToken").asText(),"Origin","http://localhost");
        assertEquals(200,platform.statusCode());String platformCookie=cookie(platform,"pet_dev_platform_sid");
        assertEquals(200,request("GET","/api/platform/auth/me",null,"Cookie",platformCookie).statusCode());
        // PLATFORM Cookie不是STAFF凭据，既有读取策略忽略它；STAFF请求保持无身份。
        for(String suffix:List.of("","/"+same)) error(request("GET",PATH+suffix,null,"Cookie",platformCookie),401,"AUTH_REQUIRED");
    }
    String cookie(HttpResponse<String> response,String name) {
        return response.headers().allValues("Set-Cookie").stream().filter(s->s.startsWith(name+"=")).findFirst().orElseThrow().split(";",2)[0];
    }
    @Test void webCookieUsesTheSameFormalReadContract()throws Exception {
        var pre=request("GET","/api/admin/auth/csrf",null);
        var login=request("POST","/api/admin/auth/login",json.writeValueAsString(Map.of("tenantCode",code,"loginName","admin","password",password)),"Cookie",cookie(pre,"pet_dev_staff_pre"),"X-CSRF-Token",data(pre).path("csrfToken").asText(),"Origin","http://localhost");
        assertEquals(200,login.statusCode());assertEquals(all(),ids(request("GET",PATH,null,"Cookie",cookie(login,"pet_dev_staff_sid")),7));
    }
    @Test void noReadPermissionDoesNotBlockSelfPasswordChange()throws Exception {
        grants(EmployeeDirectory.LIST);String t=token();error(get(t,""),403,"PERMISSION_DENIED");
        assertEquals(200,request("PUT","/api/admin/auth/password",json.writeValueAsString(Map.of("currentPassword",password,"newPassword",UUID.randomUUID()+"技术")),"X-Staff-Token","Bearer "+t).statusCode());
        error(get(t,""),401,"SESSION_EXPIRED");
    }
    @Test void allPublicSortFieldsWorkInBothDirections()throws Exception {
        String t=token();for(String field:List.of("id","loginName","displayName","status","createdAt","updatedAt"))
            for(String direction:List.of("asc","desc")) assertEquals(all(),ids(get(t,"?sortBy="+field+"&sortOrder="+direction),7));
    }
    @Test void databaseReadFailureReturns503ThenRecovers()throws Exception {
        String t=token();owner.execute("revoke select(display_name) on identity_employee from pet_runtime");
        try { error(get(t,""),503,"DEPENDENCY_UNAVAILABLE"); } finally { owner.execute("grant select(display_name) on identity_employee to pet_runtime"); }
        assertEquals(all(),ids(get(t,""),7));
    }
    @Test void rlsRestrictedRoleDefaultsToNoRowsAndCannotSelectCredentials()throws Exception {
        try(var c=DriverManager.getConnection(POSTGRES.getJdbcUrl(),IdentityDatabaseSupport.RUNTIME,POSTGRES.getPassword());var s=c.createStatement()) {
            try(var r=s.executeQuery("select id from identity_employee")) { assertFalse(r.next()); }
            c.setAutoCommit(false);try(var q=c.prepareStatement("select set_config('pet.tenant_id',?,true)")) { q.setString(1,a.tenantId().toString());q.execute(); }
            try(var r=s.executeQuery("select count(id) from identity_employee")) { r.next();assertEquals(7,r.getInt(1)); }
            try(var q=c.prepareStatement("select id from identity_employee where id=?")) { q.setObject(1,b.employeeId());assertFalse(q.executeQuery().next()); }
            c.rollback();assertThrows(SQLException.class,()->s.executeQuery("select password_hash from identity_employee"));c.rollback();
            assertThrows(SQLException.class,()->s.executeQuery("select pet_identity.upgrade_employee_read('"+a.tenantId()+"')"));c.rollback();
            try(var r=s.executeQuery("select id from identity_employee_store")) { assertFalse(r.next()); }
        }
        assertThrows(com.pet.platform.shared.exception.TenantAccessDeniedException.class,()->directory.list(EmployeeQuery.from(Map.of())));
    }
    @Test void explicitUpgradeIsLimitedIdempotentAndChangesCurrentAuthority()throws Exception {
        String t=token();error(get(t,"/"+same),403,"PERMISSION_DENIED");assertEquals(9,owner.queryForObject("select count(*) from identity_role_permission where role_id=?",Integer.class,role));
        assertEquals(1,new BootstrapJdbc(environment()).upgradeEmployeeRead(a.tenantId()));assertEquals(200,get(t,"/"+same).statusCode());
        long version=owner.queryForObject("select authorization_version from identity_employee where id=?",Long.class,a.employeeId());
        assertEquals(0,new BootstrapJdbc(environment()).upgradeEmployeeRead(a.tenantId()));assertEquals(version,owner.queryForObject("select authorization_version from identity_employee where id=?",Long.class,a.employeeId()));
        assertEquals(0,owner.queryForObject("select count(*) from identity_role_permission where tenant_id=? and permission_code='identity:user:detail'",Integer.class,b.tenantId()));
        var out=new ByteArrayOutputStream();var err=new ByteArrayOutputStream();assertEquals(0,EmployeeReadUpgradeCommand.run(new String[]{"--tenant-id",a.tenantId().toString()},environment(),new PrintStream(out),new PrintStream(err)));
        assertEquals(2,EmployeeReadUpgradeCommand.run(new String[]{"--tenant-id",UUID.randomUUID().toString()},environment(),new PrintStream(out),new PrintStream(err)));
    }
    @Test void publicOpenApiSchemaMatchesActualDtoAndQueryFields()throws Exception {
        var doc=dataOfDocument();var operation=doc.path("paths").path(PATH).path("get");assertEquals("listEmployees",operation.path("operationId").asText());
        var params=new HashSet<String>();for(var p:operation.path("parameters")) params.add(p.path("name").asText());
        assertEquals(Set.of("keyword","status","storeId","page","pageSize","sortBy","sortOrder"),params);
        var model=doc.path("components").path("schemas").path("EmployeeView");assertEquals(Set.of("id","loginName","displayName","status","createdAt","updatedAt"),model.path("properties").propertyNames());
        grants(EmployeeDirectory.DETAIL,"TENANT");String t=token();var dto=data(get(t,"/"+multi));assertEquals(model.path("properties").propertyNames(),dto.propertyNames());
        assertEquals("string",model.path("properties").path("id").path("type").asText());assertEquals("ACTIVE",model.path("properties").path("status").path("enum").get(0).asText());
        assertEquals("string",doc.path("components").path("schemas").path("PageResponseEmployeeView").path("properties").path("total").path("type").asText());
        for(var row:data(get(t,"" )).path("items")) { assertEquals(model.path("properties").propertyNames(),row.propertyNames());assertTrue(row.path("createdAt").asText().matches(com.pet.platform.shared.serialization.ScalarCodecs.INSTANT_PATTERN)); }
    }
    JsonNode dataOfDocument()throws Exception { var r=request("GET","/v3/api-docs",null);assertEquals(200,r.statusCode());return json.readTree(r.body()); }
}
