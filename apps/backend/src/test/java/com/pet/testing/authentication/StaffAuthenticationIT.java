package com.pet.testing.authentication;

import com.pet.platform.Application;
import com.pet.platform.identity.application.*;
import com.pet.platform.identity.application.bootstrap.*;
import com.pet.platform.identity.infrastructure.bootstrap.BootstrapJdbc;
import com.pet.platform.identity.infrastructure.session.AuthenticationRedis;
import com.pet.platform.identity.application.authentication.StaffSessionPort;
import com.pet.platform.shared.security.*;
import com.pet.platform.shared.tenancy.*;
import com.pet.platform.shared.api.ApiResponse;
import com.pet.platform.shared.api.ErrorCode;
import com.pet.testing.*;
import jakarta.servlet.http.*;
import java.net.URI;
import java.net.http.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** 正式迁移/受限运行角色/真实初始化服务/HTTP/Redis；没有测试CurrentPrincipalProvider。 */
@SpringBootTest(classes=Application.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test") @Import(StaffAuthenticationIT.Probes.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StaffAuthenticationIT extends PostgresIntegrationSupport {
    @LocalServerPort int port;
    @Autowired PasswordService passwords;
    @Autowired CurrentPrincipalProvider provider;
    @Autowired AuthenticationRedis dao;
    @Autowired StaffSessionPort sessions;
    @Autowired JsonMapper json;
    JdbcTemplate owner;
    HttpClient client=HttpClient.newHttpClient();
    String password=UUID.randomUUID()+"临时容器😀";
    String code;BootstrapResult initialized;
    record Web(String cookie,String csrf,String pre,String sessionId) { }
    @BeforeAll void setup(){owner=new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword()));assertInstanceOf(SessionPrincipalProvider.class,provider);}
    @BeforeEach void account(){dao.stringRedisTemplate.delete("pet:test:auth:staff:ip:"+com.pet.platform.identity.infrastructure.session.SaStaffSessions.digest("127.0.0.1"));code="a"+UUID.randomUUID().toString().replace("-","").substring(0,20);initialized=initialize(code);}
    BootstrapResult initialize(String c){try(var r=new BootstrapRequest(c,"HTTP技术租户","admin",password.toCharArray(),"store","HTTP技术门店")){return new IdentityBootstrap(new BootstrapJdbc(Map.of("PET_BOOTSTRAP_DATABASE_URL",POSTGRES.getJdbcUrl().split("\\?",2)[0],"PET_BOOTSTRAP_DATABASE_USERNAME",IdentityDatabaseSupport.BOOTSTRAP,"PET_BOOTSTRAP_DATABASE_PASSWORD",POSTGRES.getPassword())),passwords).initialize(r);}}
    String loginBody(String tenant,String name,String pwd){return json.writeValueAsString(Map.of("tenantCode",tenant,"loginName",name,"password",pwd));}
    HttpResponse<String> request(String method,String path,String body,String... headers)throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));
        for(int i=0;i<headers.length;i+=2)b.header(headers[i],headers[i+1]);
        if(body!=null)b.header("Content-Type","application/json");
        b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
        return client.send(b.build(),HttpResponse.BodyHandlers.ofString());
    }
    JsonNode data(HttpResponse<String> r){return json.readTree(r.body()).path("data");}
    void error(HttpResponse<String> r,int status,String code){assertEquals(status,r.statusCode());assertEquals(code,json.readTree(r.body()).path("error").path("code").asText());assertEquals("no-store",r.headers().firstValue("Cache-Control").orElseThrow());assertEquals(r.headers().firstValue("X-Trace-Id").orElseThrow(),json.readTree(r.body()).path("traceId").asText());assertFalse(r.body().contains(password));}
    String cookie(HttpResponse<String> r,String name){return r.headers().allValues("Set-Cookie").stream().filter(s -> s.startsWith(name+"=")).findFirst().orElseThrow().split(";",2)[0];}
    Web pre()throws Exception{var r=request("GET","/api/admin/auth/csrf",null);assertEquals(200,r.statusCode());return new Web(null,data(r).path("csrfToken").asText(),cookie(r,"pet_dev_staff_pre"),null);}
    Web web()throws Exception{var p=pre();var r=request("POST","/api/admin/auth/login",loginBody(code,"ADMIN",password),"Cookie",p.pre(),"X-CSRF-Token",p.csrf(),"Origin","http://localhost");assertEquals(200,r.statusCode());assertFalse(data(r).has("token"));String c=cookie(r,"pet_dev_staff_sid");var csrf=request("GET","/api/admin/auth/csrf",null,"Cookie",c);return new Web(c,data(csrf).path("csrfToken").asText(),p.pre(),data(r).path("sessionId").asText());}
    String token()throws Exception{var r=request("POST","/api/admin/auth/token/login",loginBody(code,"admin",password));assertEquals(200,r.statusCode());assertTrue(r.headers().allValues("Set-Cookie").isEmpty());assertEquals("X-Staff-Token",data(r).path("token").path("headerName").asText());return data(r).path("token").path("value").asText();}
    HttpResponse<String> me(String token)throws Exception{return request("GET","/api/admin/auth/me",null,"X-Staff-Token","Bearer "+token);}
    @Test void realLoginAndCurrentIdentityAreFormalDatabaseFacts()throws Exception{String t=token();var r=me(t);assertEquals(200,r.statusCode());assertEquals(initialized.tenantId().toString(),data(r).path("tenantId").asText());assertEquals(initialized.employeeId().toString(),data(r).path("principalId").asText());assertEquals(9,data(r).path("permissionCodes").size());assertEquals(1,data(r).path("authorizedStoreIds").size());assertFalse(r.body().contains(t));assertFalse(r.body().contains("password_hash"));}
    @Test void wrongPasswordDoesNotCreateSession()throws Exception{error(request("POST","/api/admin/auth/token/login",loginBody(code,"admin",password+"wrong")),401,"LOGIN_FAILED");assertNull(dao.getSession("pet:test:staff:staff:session:"+initialized.employeeId()));}
    @Test void absentTenantAndAccountHaveSameSafeFailure()throws Exception{var a=request("POST","/api/admin/auth/token/login",loginBody("unknown","admin",password));var b=request("POST","/api/admin/auth/token/login",loginBody(code,"missing",password));error(a,401,"LOGIN_FAILED");error(b,401,"LOGIN_FAILED");assertEquals(json.readTree(a.body()).path("error"),json.readTree(b.body()).path("error"));}
    @Test void disabledTenantLoginRejected()throws Exception{owner.update("update platform_tenant set status='DISABLED' where id=?",initialized.tenantId());error(request("POST","/api/admin/auth/token/login",loginBody(code,"admin",password)),401,"LOGIN_FAILED");}
    @Test void disabledEmployeeLoginRejected()throws Exception{owner.update("update identity_employee set status='DISABLED' where id=?",initialized.employeeId());error(request("POST","/api/admin/auth/token/login",loginBody(code,"admin",password)),401,"LOGIN_FAILED");}
    @Test void cookieAttributesAndWebBodyContainNoToken()throws Exception{var p=pre();var r=request("POST","/api/admin/auth/login",loginBody(code,"admin",password),"Cookie",p.pre(),"X-CSRF-Token",p.csrf(),"Origin","http://localhost");assertEquals(200,r.statusCode());String h=r.headers().allValues("Set-Cookie").stream().filter(s -> s.startsWith("pet_dev_staff_sid=")).findFirst().orElseThrow();assertTrue(h.contains("HttpOnly"));assertTrue(h.contains("Path=/api/admin/"));assertTrue(h.contains("SameSite=Lax"));assertFalse(h.contains("Secure"));assertFalse(h.contains("Max-Age"));assertFalse(data(r).has("token"));}
    @Test void missingAndWrongCsrfRejected()throws Exception{var p=pre();error(request("POST","/api/admin/auth/login",loginBody(code,"admin",password),"Cookie",p.pre(),"Origin","http://localhost"),403,"CSRF_INVALID");error(request("POST","/api/admin/auth/login",loginBody(code,"admin",password),"Cookie",p.pre(),"Origin","http://localhost","X-CSRF-Token","x".repeat(43)),403,"CSRF_INVALID");}
    @Test void expiredAnonymousCsrfRejected()throws Exception{var p=pre();sessions.deletePre(p.pre().split("=",2)[1]);error(request("POST","/api/admin/auth/login",loginBody(code,"admin",password),"Cookie",p.pre(),"Origin","http://localhost","X-CSRF-Token",p.csrf()),403,"CSRF_INVALID");}
    @Test void sourceRequiredAndExactRefererFallback()throws Exception{var p=pre();error(request("POST","/api/admin/auth/login",loginBody(code,"admin",password),"Cookie",p.pre(),"X-CSRF-Token",p.csrf()),403,"CSRF_INVALID");error(request("POST","/api/admin/auth/login",loginBody(code,"admin",password),"Cookie",p.pre(),"Origin","http://localhost.attacker.invalid","X-CSRF-Token",p.csrf()),403,"CSRF_INVALID");assertEquals(200,request("POST","/api/admin/auth/login",loginBody(code,"admin",password),"Cookie",p.pre(),"Referer","http://localhost/login","X-CSRF-Token",p.csrf()).statusCode());}
    @Test void webLogoutInvalidatesCookieAndCsrf()throws Exception{var w=web();error(request("POST","/api/admin/auth/logout",null,"Cookie",w.cookie(),"Origin","http://localhost"),403,"CSRF_INVALID");var r=request("POST","/api/admin/auth/logout",null,"Cookie",w.cookie(),"Origin","http://localhost","X-CSRF-Token",w.csrf());assertEquals(200,r.statusCode());String h=r.headers().allValues("Set-Cookie").stream().filter(s -> s.startsWith("pet_dev_staff_sid=")).findFirst().orElseThrow();assertTrue(h.contains("Max-Age=0"));assertTrue(h.contains("Path=/api/admin/"));assertTrue(h.contains("HttpOnly"));error(request("GET","/api/admin/auth/me",null,"Cookie",w.cookie()),401,"SESSION_EXPIRED");error(request("POST","/api/admin/auth/logout",null,"Cookie",w.cookie(),"Origin","http://localhost","X-CSRF-Token",w.csrf()),401,"SESSION_EXPIRED");}
    @Test void csrfRotationAndSessionFixationProtection()throws Exception{var w=web();var r=request("POST","/api/admin/auth/login",loginBody(code,"admin",password),"Cookie",w.cookie(),"Origin","http://localhost","X-CSRF-Token",w.csrf());assertEquals(200,r.statusCode());String next=cookie(r,"pet_dev_staff_sid");assertNotEquals(w.cookie(),next);error(request("GET","/api/admin/auth/me",null,"Cookie",w.cookie()),401,"SESSION_EXPIRED");error(request("POST","/api/admin/auth/logout",null,"Cookie",next,"Origin","http://localhost","X-CSRF-Token",w.csrf()),403,"CSRF_INVALID");assertNull(sessions.readPre(w.pre().split("=",2)[1]));}
    @Test void webAndMiniLogoutAreDeviceIsolated()throws Exception{var w=web();String t=token();assertEquals(200,request("POST","/api/admin/auth/logout",null,"X-Staff-Token","Bearer "+t).statusCode());error(me(t),401,"SESSION_EXPIRED");assertEquals(200,request("GET","/api/admin/auth/me",null,"Cookie",w.cookie()).statusCode());}
    @Test void credentialAmbiguityDomainAndHeaderGrammar()throws Exception{var w=web();String t=token();error(request("GET","/api/admin/auth/me",null,"Cookie",w.cookie(),"X-Staff-Token","Bearer "+t),400,"AUTH_CREDENTIAL_AMBIGUOUS");error(request("GET","/api/admin/auth/me",null,"X-Customer-Token","Bearer "+t),401,"AUTH_DOMAIN_MISMATCH");error(request("GET","/api/admin/auth/me",null,"Authorization","Bearer "+t),401,"AUTH_DOMAIN_MISMATCH");error(request("GET","/api/admin/auth/me",null,"X-Staff-Token","bearer "+t),400,"BAD_REQUEST");error(request("GET","/api/admin/auth/me",null,"X-Staff-Token","Bearer "+t,"X-Staff-Token","Bearer "+t),400,"AUTH_CREDENTIAL_AMBIGUOUS");}
    @Test void miniRejectsWebCookiesAndWebRejectsMiniToken()throws Exception{var w=web();String t=token();error(request("POST","/api/admin/auth/token/login",loginBody(code,"admin",password),"Cookie",w.cookie()),400,"AUTH_CREDENTIAL_AMBIGUOUS");error(request("GET","/api/admin/auth/me",null,"X-Staff-Token","Bearer "+w.cookie().split("=",2)[1]),401,"AUTH_DOMAIN_MISMATCH");error(request("GET","/api/admin/auth/me",null,"Cookie","pet_dev_staff_sid="+t),401,"AUTH_DOMAIN_MISMATCH");}
    @Test void otherSaSpacesCannotResolveStaffToken()throws Exception{String t=token();assertNull(cn.dev33.satoken.SaManager.getStpLogic("customer").getLoginIdByToken(t));assertNull(cn.dev33.satoken.SaManager.getStpLogic("platform").getLoginIdByToken(t));}
    @Test void clientTenantFieldsCannotOverrideTrustedIdentity()throws Exception{String t=token();UUID fake=UUID.randomUUID();var r=request("GET","/api/admin/auth/me?tenantId="+fake,null,"X-Staff-Token","Bearer "+t,"X-Tenant-Id",fake.toString());assertEquals(initialized.tenantId().toString(),data(r).path("tenantId").asText());String body=loginBody(code,"admin",password).replace("{","{\"tenantId\":\""+fake+"\",");error(request("POST","/api/admin/auth/token/login",body),400,"BAD_REQUEST");}
    @Test void tenantAndEmployeeDisableInvalidateExistingSessions()throws Exception{String t=token();owner.update("update platform_tenant set status='DISABLED',security_version=security_version+1 where id=?",initialized.tenantId());error(me(t),401,"SESSION_REVOKED");owner.update("update platform_tenant set status='ACTIVE' where id=?",initialized.tenantId());error(me(t),401,"SESSION_REVOKED");String t2=token();owner.update("update identity_employee set status='DISABLED',security_version=security_version+1 where id=?",initialized.employeeId());error(me(t2),401,"SESSION_REVOKED");}
    @Test void credentialVersionChangeInvalidatesSession()throws Exception{String t=token();owner.update("update identity_employee set security_version=security_version+1 where id=?",initialized.employeeId());error(me(t),401,"SESSION_REVOKED");}
    @Test void rolePermissionAndAssignmentsReloadEvenWithoutVersionBump()throws Exception{String t=token();owner.update("delete from identity_role_permission where tenant_id=? and permission_code='identity:user:disable'",initialized.tenantId());assertEquals(8,data(me(t)).path("permissionCodes").size());owner.update("update identity_role set status='DISABLED' where tenant_id=?",initialized.tenantId());assertEquals(0,data(me(t)).path("permissionCodes").size());owner.update("update identity_role set status='ACTIVE' where tenant_id=?",initialized.tenantId());owner.update("delete from identity_employee_role where tenant_id=?",initialized.tenantId());assertEquals(0,data(me(t)).path("permissionCodes").size());}
    @Test void storeAssignmentsAndPermissionScopesReload()throws Exception{String t=token();owner.update("update identity_role_permission set scope_type='STORES' where tenant_id=? and permission_code='platform:store:list'",initialized.tenantId());var r=me(t);assertTrue(data(r).path("dataScope").toString().contains("STORES"));owner.update("delete from identity_employee_store where tenant_id=?",initialized.tenantId());assertEquals(0,data(me(t)).path("authorizedStoreIds").size());assertEquals(404,request("GET","/api/admin/__authentication-test/store/"+initialized.storeId(),null,"X-Staff-Token","Bearer "+t).statusCode());}
    @Test void formalStoreUsesProviderTenantTransactionAndRls()throws Exception{String t=token();assertEquals(200,request("GET","/api/admin/__authentication-test/store/"+initialized.storeId(),null,"X-Staff-Token","Bearer "+t).statusCode());var other=initialize("b"+UUID.randomUUID().toString().substring(0,20));assertEquals(404,request("GET","/api/admin/__authentication-test/store/"+other.storeId(),null,"X-Staff-Token","Bearer "+t).statusCode());}
    @Test void realUserAsyncSnapshotsAreRevalidatedBeforeExecution()throws Exception{String t=token();assertEquals(200,request("GET","/api/admin/__authentication-test/async",null,"X-Staff-Token","Bearer "+t).statusCode());}
    @Test void requestFailureAndAnonymousReuseDoNotLeakIdentity()throws Exception{String t=token();error(request("GET","/api/admin/__authentication-test/failure",null,"X-Staff-Token","Bearer "+t),500,"INTERNAL_ERROR");for(int i=0;i<12;i++)error(request("GET","/api/admin/auth/me",null),401,"AUTH_REQUIRED");assertTrue(provider.currentPrincipal().isEmpty());assertTrue(TenantContextHolder.current().isEmpty());}
    @Test void concurrentAttemptCountersAreAtomicAndNormalizationShared()throws Exception{var pool=Executors.newFixedThreadPool(12);String account=UUID.randomUUID().toString();var calls=new ArrayList<Future<Boolean>>();var gate=new CountDownLatch(1);for(int i=0;i<24;i++)calls.add(pool.submit(() -> {gate.await();try{sessions.limit(UUID.randomUUID().toString(),account);return true;}catch(com.pet.platform.shared.exception.BusinessException e){assertEquals(ErrorCode.RATE_LIMITED,e.error().code());return false;}}));gate.countDown();int accepted=0;for(var f:calls)if(f.get(10,TimeUnit.SECONDS))accepted++;pool.shutdown();assertEquals(10,accepted);for(int i=0;i<10;i++)request("POST","/api/admin/auth/token/login",loginBody(code,"ADMIN",password+"wrong"));error(request("POST","/api/admin/auth/token/login",loginBody(code," admin ",password)),429,"RATE_LIMITED");}
    @Test void maximumFiveDevicesDoesNotSilentlyEvict()throws Exception{String first=token();for(int i=0;i<4;i++)token();error(request("POST","/api/admin/auth/token/login",loginBody(code,"admin",password)),409,"SESSION_LIMIT_REACHED");assertEquals(200,me(first).statusCode());request("POST","/api/admin/auth/logout",null,"X-Staff-Token","Bearer "+first);token();}
    @Test void invalidWebCookieCanRecoverButDependencyFailureKeepsCookie()throws Exception {
        var w=web();
        owner.execute("REVOKE EXECUTE ON FUNCTION pet_identity.staff_authorization(uuid,uuid) FROM pet_runtime");
        try {
            var failure=request("GET","/api/admin/auth/me",null,"Cookie",w.cookie());
            error(failure,503,"DEPENDENCY_UNAVAILABLE");assertTrue(failure.headers().allValues("Set-Cookie").isEmpty());
        } finally {owner.execute("GRANT EXECUTE ON FUNCTION pet_identity.staff_authorization(uuid,uuid) TO pet_runtime");}
        assertEquals(200,request("GET","/api/admin/auth/me",null,"Cookie",w.cookie()).statusCode());
        assertEquals(200,request("POST","/api/admin/auth/logout",null,"Cookie",w.cookie(),"Origin","http://localhost","X-CSRF-Token",w.csrf()).statusCode());
        var expired=request("GET","/api/admin/auth/csrf",null,"Cookie",w.cookie());
        error(expired,401,"SESSION_EXPIRED");
        assertTrue(expired.headers().allValues("Set-Cookie").stream().anyMatch(v -> v.startsWith("pet_dev_staff_sid=") && v.contains("Max-Age=0") && v.contains("Path=/api/admin/") && v.contains("HttpOnly")));
        assertEquals(200,request("GET","/api/admin/auth/csrf",null).statusCode());
        var next=web();owner.update("update identity_employee set security_version=security_version+1 where id=?",initialized.employeeId());
        var revoked=request("GET","/api/admin/auth/me",null,"Cookie",next.cookie());
        error(revoked,401,"SESSION_REVOKED");assertEquals("pet_dev_staff_sid=",cookie(revoked,"pet_dev_staff_sid"));
        assertNotNull(web());
    }
    @Test void databaseFailureIs503AndDoesNotDeleteValidSession()throws Exception{String t=token();owner.execute("REVOKE EXECUTE ON FUNCTION pet_identity.staff_authorization(uuid,uuid) FROM pet_runtime");try{error(me(t),503,"DEPENDENCY_UNAVAILABLE");error(request("POST","/api/admin/auth/token/login",loginBody(code,"admin",password)),503,"DEPENDENCY_UNAVAILABLE");}finally{owner.execute("GRANT EXECUTE ON FUNCTION pet_identity.staff_authorization(uuid,uuid) TO pet_runtime");}assertEquals(200,me(t).statusCode());}
    @Test void redisFailureIs503AndDoesNotBecomeAuthenticationFailure()throws Exception{String t=token();var w=web();RedisTestSupport.REDIS.getDockerClient().pauseContainerCmd(RedisTestSupport.REDIS.getContainerId()).exec();try{error(me(t),503,"DEPENDENCY_UNAVAILABLE");var browser=request("GET","/api/admin/auth/me",null,"Cookie",w.cookie());error(browser,503,"DEPENDENCY_UNAVAILABLE");assertTrue(browser.headers().allValues("Set-Cookie").isEmpty());error(request("POST","/api/admin/auth/token/login",loginBody(code,"admin",password)),503,"DEPENDENCY_UNAVAILABLE");}finally{RedisTestSupport.REDIS.getDockerClient().unpauseContainerCmd(RedisTestSupport.REDIS.getContainerId()).exec();}assertEquals(200,me(t).statusCode());assertEquals(200,request("GET","/api/admin/auth/me",null,"Cookie",w.cookie()).statusCode());}
    @Test void forwardedHeadersCannotBypassIpLimit()throws Exception{
        String key="pet:test:auth:staff:ip:"+com.pet.platform.identity.infrastructure.session.SaStaffSessions.digest("127.0.0.1");
        dao.stringRedisTemplate.opsForValue().set(key,"60",Duration.ofMinutes(5));
        error(request("POST","/api/admin/auth/token/login",loginBody(code,"admin",password),"X-Forwarded-For","203.0.113.3"),429,"RATE_LIMITED");
    }
    @Test void invalidSerializationAndNamespaceRejected(){assertThrows(com.pet.platform.shared.exception.BusinessException.class,() -> dao.setObject("pet:test:staff:staff:session:technical",new java.io.File("x"),10));assertThrows(com.pet.platform.shared.exception.BusinessException.class,() -> dao.get("pet:test:v1:tenant:other"));assertThrows(com.pet.platform.shared.exception.BusinessException.class,() -> dao.set("pet:test:staff:staff:token:technical","x",-1));}
    @Test void publicEndpointIgnoresCredentialsAndUrlTokenRejected()throws Exception{String t=token();assertEquals(200,request("GET","/actuator/health/liveness",null,"X-Staff-Token","Bearer "+t).statusCode());error(request("GET","/api/admin/auth/me?token="+t,null),400,"BAD_REQUEST");}
    @TestConfiguration(proxyBeanMethods=false) static class Probes {@Bean Probe probe(TenantTaskExecutor tasks,StoreScopeGuard stores){return new Probe(tasks,stores);}}
    @RestController static class Probe {
        final TenantTaskExecutor tasks;final StoreScopeGuard stores;Probe(TenantTaskExecutor tasks,StoreScopeGuard stores){this.tasks=tasks;this.stores=stores;}
        @GetMapping("/api/admin/__authentication-test/store/{id}") ApiResponse.Success<String> store(@PathVariable UUID id){try(var s=TenantExecutionScope.forPermission("platform:store:list")){stores.requireStore(id);return ApiResponse.success(TenantContextHolder.current().orElseThrow().tenantId().toString());}}
        @GetMapping("/api/admin/__authentication-test/async") ApiResponse.Success<Void> async()throws Exception{try(var s=TenantExecutionScope.forPermission("identity:user:list")){tasks.submit(() -> null).get(5,TimeUnit.SECONDS);return ApiResponse.success(null);}}
        @GetMapping("/api/admin/__authentication-test/failure") ApiResponse.Success<Void> failure(){throw new IllegalStateException("技术异常，不含凭据");}
    }
}
