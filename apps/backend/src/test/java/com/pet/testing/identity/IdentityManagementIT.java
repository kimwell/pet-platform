package com.pet.testing.identity;

import com.pet.platform.Application;
import com.pet.platform.identity.application.*;
import com.pet.platform.identity.application.bootstrap.*;
import com.pet.platform.identity.infrastructure.bootstrap.*;
import com.pet.testing.*;
import java.net.*;
import java.net.http.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** B01真实PG/Redis/正式认证与HTTP写入，SQL仅准备隔离反例或注入故障。 */
@SpringBootTest(classes=Application.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test") @TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdentityManagementIT extends PostgresIntegrationSupport {
    @LocalServerPort int port;
    @Autowired PasswordService passwords;
    @Autowired JsonMapper json;
    @Autowired com.pet.platform.identity.infrastructure.session.AuthenticationRedis redis;
    final HttpClient client=HttpClient.newHttpClient();
    final String password=UUID.randomUUID()+"隔离技术", replacement=UUID.randomUUID()+"改密技术";
    JdbcTemplate owner;BootstrapResult a,b;String code,admin;UUID second;
    Map<String,String> environment(){return Map.of("PET_BOOTSTRAP_DATABASE_URL",POSTGRES.getJdbcUrl().split("\\?",2)[0],"PET_BOOTSTRAP_DATABASE_USERNAME",IdentityDatabaseSupport.BOOTSTRAP,"PET_BOOTSTRAP_DATABASE_PASSWORD",POSTGRES.getPassword());}
    BootstrapResult bootstrap(String code){try(var r=new BootstrapRequest(code,"B01隔离验收","admin",password.toCharArray(),"main","第一门店")){return new IdentityBootstrap(new BootstrapJdbc(environment()),passwords).initialize(r);}}
    @BeforeAll void owner(){owner=new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword()));}
    @BeforeEach void setup()throws Exception{var keys=redis.stringRedisTemplate.keys("pet:test:auth:*");if(keys!=null&&!keys.isEmpty())redis.stringRedisTemplate.delete(keys);code="b"+UUID.randomUUID().toString().replace("-","").substring(0,15);a=bootstrap(code);b=bootstrap("f"+UUID.randomUUID().toString().replace("-","").substring(0,15));new BootstrapJdbc(environment()).upgradeIdentityManagement(a.tenantId());admin=login(code,"admin",password);second=UUID.randomUUID();owner.update("insert into platform_store(id,tenant_id,code,name,status) values(?,?,?,'第二门店','ACTIVE')",second,a.tenantId(),"second");}
    HttpResponse<String> call(String token,String method,String path,Object body)throws Exception{var q=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/admin"+path)).timeout(Duration.ofSeconds(20));if(token!=null)q.header("X-Staff-Token","Bearer "+token);if(body!=null)q.header("Content-Type","application/json");return client.send(q.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
    JsonNode data(HttpResponse<String> r){assertEquals(200,r.statusCode(),r.body());assertFalse(r.body().contains(password));assertFalse(r.body().contains(replacement));return json.readTree(r.body()).path("data");}
    void error(HttpResponse<String> r,int status,String code){assertEquals(status,r.statusCode(),r.body());assertEquals(code,json.readTree(r.body()).path("error").path("code").asText());}
    String login(String tenant,String login,String secret)throws Exception{return data(call(null,"POST","/auth/token/login",Map.of("tenantCode",tenant,"loginName",login,"password",secret))).path("token").path("value").asText();}
    UUID employee(String login)throws Exception{return UUID.fromString(data(call(admin,"POST","/identity/users",Map.of("loginName",login,"displayName","员工 "+login,"initialPassword",password))).path("id").asText());}
    UUID role()throws Exception{return UUID.fromString(data(call(admin,"POST","/identity/roles",Map.of("code","r"+UUID.randomUUID().toString().replace("-","").substring(0,10),"name","授权验收角色"))).path("id").asText());}
    String version(UUID e)throws Exception{return data(call(admin,"GET","/identity/users/"+e+"/management",null)).path("version").asText();}
    String roleVersion(UUID r)throws Exception{return data(call(admin,"GET","/identity/roles/"+r,null)).path("role").path("version").asText();}
    void assign(UUID e,String kind,List<UUID> ids)throws Exception{data(call(admin,"PUT","/identity/users/"+e+"/"+kind,Map.of("version",version(e),"ids",ids)));}
    Map<String,String> grant(String permission,String scope){return Map.of("permissionCode",permission,"scopeType",scope);}
    void grants(UUID r,List<?> grants)throws Exception{data(call(admin,"PUT","/identity/roles/"+r+"/grants",Map.of("version",roleVersion(r),"grants",grants)));}
    String activate(String login)throws Exception{String initial=login(code,login,password);data(call(initial,"PUT","/auth/password",Map.of("currentPassword",password,"newPassword",replacement)));error(call(initial,"GET","/auth/me",null),401,"SESSION_EXPIRED");return login(code,login,replacement);}
    @Test void completeBusinessLifecycleAndPermissionSpecificUnion()throws Exception{
        UUID e=employee("member"),peer=employee("peer"),outside=employee("outside"),r=role();
        data(call(admin,"PUT","/identity/users/"+e,Map.of("version",version(e),"displayName","正式编辑姓名")));
        grants(r,List.of(grant("identity:user:list","STORES"),grant("identity:user:list","SELF"),grant("identity:user:detail","SELF")));
        assign(e,"stores",List.of(a.storeId()));assign(peer,"stores",List.of(a.storeId()));assign(e,"roles",List.of(r));
        String staff=activate("member");var rows=data(call(staff,"GET","/identity/users",null));assertEquals("3",rows.path("total").asText());
        assertEquals(200,call(staff,"GET","/identity/users/"+e,null).statusCode());error(call(staff,"GET","/identity/users/"+peer,null),404,"RESOURCE_NOT_FOUND");
        error(call(staff,"GET","/identity/users/"+outside,null),404,"RESOURCE_NOT_FOUND");
        grants(r,List.of());error(call(staff,"GET","/auth/me",null),401,"SESSION_EXPIRED");
        staff=login(code,"member",replacement);error(call(staff,"GET","/identity/users",null),403,"PERMISSION_DENIED");
        data(call(admin,"PUT","/identity/users/"+e+"/status",Map.of("version",version(e),"status","DISABLED")));
        error(call(staff,"GET","/auth/me",null),401,"SESSION_EXPIRED");
        error(call(null,"POST","/auth/token/login",Map.of("tenantCode",code,"loginName","member","password",replacement)),401,"LOGIN_FAILED");
        assertEquals("DISABLED",owner.queryForObject("select status from identity_employee where id=?",String.class,e));
        assertEquals("正式编辑姓名",owner.queryForObject("select display_name from identity_employee where id=?",String.class,e));
        assertEquals(1,owner.queryForObject("select count(*) from identity_employee_role where employee_id=?",Integer.class,e));assertEquals(0,owner.queryForObject("select count(*) from identity_role_permission where role_id=?",Integer.class,r));
        data(call(admin,"PUT","/identity/users/"+e+"/status",Map.of("version",version(e),"status","ACTIVE")));assertNotNull(login(code,"member",replacement));
    }
    @Test void absentIndependentPermissionsAndReadWriteSeparation()throws Exception{employee("no-rights");String t=activate("no-rights");error(call(t,"POST","/identity/users",Map.of("loginName","blocked","displayName","禁止","initialPassword",password)),403,"PERMISSION_DENIED");error(call(t,"GET","/identity/roles",null),403,"PERMISSION_DENIED");}
    @Test void foreignTargetsAndAssociationsNeverMutate()throws Exception{UUID e=employee("target");error(call(admin,"PUT","/identity/users/"+b.employeeId(),Map.of("version","0","displayName","跨租户")),404,"RESOURCE_NOT_FOUND");
        UUID foreignRole=owner.queryForObject("select id from identity_role where tenant_id=?",UUID.class,b.tenantId());
        error(call(admin,"PUT","/identity/users/"+e+"/roles",Map.of("version",version(e),"ids",List.of(foreignRole))),404,"RESOURCE_NOT_FOUND");
        error(call(admin,"PUT","/identity/users/"+e+"/stores",Map.of("version",version(e),"ids",List.of(b.storeId()))),404,"RESOURCE_NOT_FOUND");assertEquals("0",version(e));}
    @Test void grantCeilingRejectsDifferentPermissionAndStoreUpperBound()throws Exception{UUID e=employee("target"),r=role();
        error(call(admin,"PUT","/identity/users/"+e+"/stores",Map.of("version",version(e),"ids",List.of(second))),404,"RESOURCE_NOT_FOUND");
        owner.update("delete from identity_role_permission where tenant_id=? and permission_code='identity:user:list'",a.tenantId());
        error(call(admin,"PUT","/identity/roles/"+r+"/grants",Map.of("version","0","grants",List.of(grant("identity:user:list","TENANT")))),403,"PERMISSION_DENIED");assertEquals("0",roleVersion(r));}
    @Test void tenantGrantCannotBeGrantedBySelfScope()throws Exception{UUID r=role();owner.update("update identity_role_permission set scope_type='SELF' where tenant_id=? and permission_code='identity:user:list'",a.tenantId());error(call(admin,"PUT","/identity/roles/"+r+"/grants",Map.of("version","0","grants",List.of(grant("identity:user:list","TENANT")))),403,"PERMISSION_DENIED");}
    @Test void versionConcurrentEditsHaveExactlyOneWinner()throws Exception{UUID e=employee("target");try(var pool=Executors.newFixedThreadPool(2)){var go=new CountDownLatch(1);var x=pool.submit(()->{go.await();return call(admin,"PUT","/identity/users/"+e,Map.of("version","0","displayName","甲")).statusCode();});var y=pool.submit(()->{go.await();return call(admin,"PUT","/identity/users/"+e,Map.of("version","0","displayName","乙")).statusCode();});go.countDown();assertEquals(Set.of(200,409),Set.of(x.get(30,TimeUnit.SECONDS),y.get(30,TimeUnit.SECONDS)));}assertEquals("1",version(e));}
    @Test void associationAndSecurityIntentRollbackWhenFinalEventFails()throws Exception{UUID e=employee("target"),r=role();long before=owner.queryForObject("select security_version from identity_employee where id=?",Long.class,e);
        owner.execute("create function public.b01_fail_event() returns trigger language plpgsql as $$ begin if NEW.operation='ASSIGN_ROLES' then raise exception 'controlled failure'; end if; return NEW; end $$");owner.execute("create trigger b01_fail before insert on identity_management_event for each row execute function public.b01_fail_event()");
        try{error(call(admin,"PUT","/identity/users/"+e+"/roles",Map.of("version","0","ids",List.of(r))),503,"DEPENDENCY_UNAVAILABLE");}finally{owner.execute("drop trigger b01_fail on identity_management_event");owner.execute("drop function public.b01_fail_event()");}
        assertEquals("0",version(e));assertEquals(before,owner.queryForObject("select security_version from identity_employee where id=?",Long.class,e));assertEquals(0,owner.queryForObject("select count(*) from identity_employee_role where employee_id=?",Integer.class,e));assertEquals(0,owner.queryForObject("select count(*) from identity_session_cleanup where employee_id=?",Integer.class,e));assign(e,"roles",List.of(r));}
    @Test void protectedAdminAndRoleCannotBeDisabledOrReassigned()throws Exception{error(call(admin,"PUT","/identity/users/"+a.employeeId()+"/status",Map.of("version",version(a.employeeId()),"status","DISABLED")),403,"PERMISSION_DENIED");UUID r=owner.queryForObject("select id from identity_role where tenant_id=?",UUID.class,a.tenantId());error(call(admin,"PUT","/identity/roles/"+r,Map.of("version",roleVersion(r),"name","危险","status","DISABLED")),403,"PERMISSION_DENIED");}
    @Test void duplicateAndInvalidFieldsArePreciseAndNoTenantInputAccepted()throws Exception{employee("target");var r=call(admin,"POST","/identity/users",Map.of("loginName","TARGET","displayName","名称","initialPassword",password));error(r,422,"VALIDATION_FAILED");assertEquals("loginName",json.readTree(r.body()).path("error").path("fieldErrors").get(0).path("field").asText());error(call(admin,"POST","/identity/users",Map.of("loginName","other","displayName","名称","initialPassword",password,"tenantId",b.tenantId())),400,"BAD_REQUEST");}
    @Test void realOptionsArePagedAndRespectStoreCeiling()throws Exception{var list=data(call(admin,"GET","/identity/roles?page=1&pageSize=1",null));assertEquals("1",list.path("total").asText());var stores=data(call(admin,"GET","/platform/stores/options",null));assertEquals("1",stores.path("total").asText());assertEquals(a.storeId().toString(),stores.path("items").get(0).path("id").asText());error(call(admin,"GET","/platform/stores/options?tenantId="+b.tenantId(),null),400,"BAD_REQUEST");}
    @Test void resetAndExplicitSessionRevokeReuseFormalSecurityService()throws Exception{UUID e=employee("target");String token=activate("target");data(call(admin,"POST","/identity/users/"+e+"/revoke-sessions",Map.of("version",version(e),"currentPassword",password)));error(call(token,"GET","/auth/me",null),401,"SESSION_EXPIRED");data(call(admin,"PUT","/identity/users/"+e+"/password",Map.of("version",version(e),"currentPassword",password,"newPassword",password)));assertTrue(data(call(admin,"GET","/identity/users/"+e+"/management",null)).path("passwordChangeRequired").asBoolean());}
    @Test void redisFailureRefusesMutationAndRecoveryKeepsVersions()throws Exception{UUID e=employee("target");RedisTestSupport.REDIS.getDockerClient().pauseContainerCmd(RedisTestSupport.REDIS.getContainerId()).exec();
        try{error(call(admin,"PUT","/identity/users/"+e,Map.of("version","0","displayName","故障中")),503,"DEPENDENCY_UNAVAILABLE");}finally{RedisTestSupport.REDIS.getDockerClient().unpauseContainerCmd(RedisTestSupport.REDIS.getContainerId()).exec();}
        assertEquals("0",version(e));data(call(admin,"PUT","/identity/users/"+e,Map.of("version","0","displayName","恢复后")));assertEquals("1",version(e));}
    @Test void explicitUpgradeIsIdempotentAndDoesNotTouchForeignTenant(){assertEquals(0,new BootstrapJdbc(environment()).upgradeIdentityManagement(a.tenantId()));assertEquals(9,owner.queryForObject("select count(*) from identity_role_permission where tenant_id=?",Integer.class,b.tenantId()));assertEquals(17,owner.queryForObject("select count(*) from identity_role_permission where tenant_id=?",Integer.class,a.tenantId()));}
    @Test void runtimeRlsAndCredentialRestrictionsRemainEnforced()throws Exception{UUID e=employee("target");try(var c=DriverManager.getConnection(POSTGRES.getJdbcUrl(),IdentityDatabaseSupport.RUNTIME,POSTGRES.getPassword())){c.setAutoCommit(false);try(var s=c.createStatement()){s.execute("select set_config('pet.tenant_id','"+a.tenantId()+"',true)");assertEquals(0,s.executeUpdate("update identity_employee set display_name='禁止' where id='"+b.employeeId()+"'"));assertThrows(SQLException.class,()->s.execute("select password_hash from identity_employee where id='"+e+"'"));c.rollback();assertThrows(SQLException.class,()->s.execute("select pet_identity.upgrade_identity_management('"+a.tenantId()+"')"));c.rollback();}}}
}
