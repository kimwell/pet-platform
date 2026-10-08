package com.pet.testing.authentication;

import com.pet.testing.*;
import com.pet.platform.identity.application.PasswordService;
import com.pet.platform.identity.application.bootstrap.*;
import com.pet.platform.identity.infrastructure.bootstrap.BootstrapJdbc;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** 两个独立JVM生产JAR共享正式PostgreSQL与Redis，不共享内存身份。 */
class StaffSessionRuntimeIT extends PostgresIntegrationSupport {
    final JsonMapper json=JsonMapper.builder().build();
    final HttpClient http=HttpClient.newHttpClient();
    record Running(Process process,int port,Path log) implements AutoCloseable {
        public void close()throws Exception{process.destroy();assertTrue(process.waitFor(15,TimeUnit.SECONDS));}
    }
    Running start(String profile,boolean shortExpiry,String suffix,String... extra)throws Exception {
        int port;try(var socket=new ServerSocket(0,0,InetAddress.getLoopbackAddress())){port=socket.getLocalPort();}
        var argv=new ArrayList<>(List.of(Path.of(System.getProperty("java.home"),"bin/java").toString(),"-jar","target/pet-platform-backend-0.0.0-SNAPSHOT.jar","--spring.profiles.active="+profile));
        argv.addAll(Arrays.asList(extra));
        if(shortExpiry)argv.addAll(List.of("--pet.auth.mini-absolute-seconds=4","--pet.auth.mini-idle-seconds=2","--pet.auth.web-absolute-seconds=4","--pet.auth.web-idle-seconds=2"));
        var builder=new ProcessBuilder(argv);var env=builder.environment();RedisTestSupport.environment(env);
        env.put("PET_ENVIRONMENT",profile);env.put("PET_DATABASE_URL",POSTGRES.getJdbcUrl().split("\\?",2)[0]);env.put("PET_DATABASE_USERNAME",IdentityDatabaseSupport.RUNTIME);env.put("PET_DATABASE_PASSWORD",POSTGRES.getPassword());
        env.put("PET_DATABASE_MIGRATION_ENABLED","false");env.put("PET_PUBLIC_ORIGIN","https://authentication.example.invalid");env.put("SERVER_PORT",Integer.toString(port));env.put("SERVER_ADDRESS","127.0.0.1");env.put("PET_AUTH_COOKIE_SECURE","true");
        Path log=Path.of("target/p05-02-jar-"+suffix+".log");return new Running(builder.redirectErrorStream(true).redirectOutput(log.toFile()).start(),port,log);
    }
    void ready(Running app)throws Exception{long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(40);while(System.nanoTime()<deadline && app.process().isAlive()){try{if(call(app,"GET","/actuator/health",null).statusCode()==200)return;}catch(java.io.IOException ignored){}Thread.sleep(100);}fail("生产JAR未就绪，见"+app.log());}
    HttpResponse<String> call(Running a,String method,String path,String body,String...headers)throws Exception{var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+a.port()+path)).timeout(Duration.ofSeconds(5));if(body!=null)b.header("Content-Type","application/json");for(int i=0;i<headers.length;i+=2)b.header(headers[i],headers[i+1]);return http.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());}
    JsonNode data(HttpResponse<String> r){return json.readTree(r.body()).path("data");}
    String setup(String password){Flyway.configure().dataSource(POSTGRES.getJdbcUrl(),IdentityDatabaseSupport.MIGRATION,POSTGRES.getPassword()).locations("classpath:db/migration").load().migrate();String code="r"+UUID.randomUUID().toString().replace("-","").substring(0,20);try(var input=new BootstrapRequest(code,"JVM技术租户","admin",password.toCharArray(),null,null)){new IdentityBootstrap(new BootstrapJdbc(Map.of("PET_BOOTSTRAP_DATABASE_URL",POSTGRES.getJdbcUrl().split("\\?",2)[0],"PET_BOOTSTRAP_DATABASE_USERNAME",IdentityDatabaseSupport.BOOTSTRAP,"PET_BOOTSTRAP_DATABASE_PASSWORD",POSTGRES.getPassword())),new PasswordService()).initialize(input);}return code;}
    String body(String code,String password){return json.writeValueAsString(Map.of("tenantCode",code,"loginName","admin","password",password));}
    String login(Running a,String code,String password)throws Exception{var r=call(a,"POST","/api/admin/auth/token/login",body(code,password));assertEquals(200,r.statusCode());return data(r).path("token").path("value").asText();}
    String cookie(HttpResponse<String> r,String name){return r.headers().allValues("Set-Cookie").stream().filter(s->s.startsWith(name+"=")).findFirst().orElseThrow().split(";",2)[0];}
    @Test void twoProductionInstancesShareAndRevokeTheSameRedisSessionAndSecureCookie()throws Exception {
        String password=UUID.randomUUID()+"临时JVM";String code=setup(password);
        try(var first=start("prod",false,"first");var second=start("prod",false,"second")){
            ready(first);ready(second);String t=login(first,code,password);
            assertEquals(200,call(second,"GET","/api/admin/auth/me",null,"X-Staff-Token","Bearer "+t).statusCode());
            assertEquals(200,call(second,"POST","/api/admin/auth/logout",null,"X-Staff-Token","Bearer "+t).statusCode());
            assertEquals(401,call(first,"GET","/api/admin/auth/me",null,"X-Staff-Token","Bearer "+t).statusCode());
            var pre=call(first,"GET","/api/admin/auth/csrf",null);String preCookie=cookie(pre,"__Secure-pet_staff_pre");
            var web=call(first,"POST","/api/admin/auth/login",body(code,password),"Cookie",preCookie,"X-CSRF-Token",data(pre).path("csrfToken").asText(),"Origin","https://authentication.example.invalid");assertEquals(200,web.statusCode());
            String raw=web.headers().allValues("Set-Cookie").stream().filter(s -> s.startsWith("__Secure-pet_staff_sid=")).findFirst().orElseThrow();assertTrue(raw.contains("Secure"));assertTrue(raw.contains("HttpOnly"));assertTrue(raw.contains("SameSite=Lax"));assertTrue(raw.contains("Path=/api/admin/"));assertFalse(raw.contains("Domain="));assertFalse(raw.contains("Max-Age"));assertFalse(data(web).has("token"));
            String sid=cookie(web,"__Secure-pet_staff_sid");var csrf=call(second,"GET","/api/admin/auth/csrf",null,"Cookie",sid);assertEquals(200,csrf.statusCode());
            var logout=call(second,"POST","/api/admin/auth/logout",null,"Cookie",sid,"X-CSRF-Token",data(csrf).path("csrfToken").asText(),"Origin","https://authentication.example.invalid");assertEquals(200,logout.statusCode());assertTrue(logout.headers().allValues("Set-Cookie").stream().anyMatch(s -> s.startsWith("__Secure-pet_staff_sid=") && s.contains("Secure") && s.contains("Max-Age=0")));
            assertEquals(401,call(first,"GET","/api/admin/auth/me",null,"Cookie",sid).statusCode());
            Files.writeString(Path.of("target/p05-02-two-instances.txt"),"两个独立JVM：登录A→me B 200→退出B→me A 401；生产Cookie属性与跨实例CSRF/退出通过。HTTP属性检查不等于生产TLS浏览器验收。\n");
        }
    }
    @Test void twoProductionInstancesObservePasswordChangeAndOldWebMiniSessionsCannotReturn()throws Exception {
        String password=UUID.randomUUID()+" 容器原秘密 ",next=UUID.randomUUID()+" 容器新秘密 ";String code=setup(password);
        try(var first=start("prod",false,"credential-first");var second=start("prod",false,"credential-second")){
            ready(first);ready(second);String a=login(first,code,password),b=login(second,code,password);
            var pre=call(first,"GET","/api/admin/auth/csrf",null);
            var web=call(first,"POST","/api/admin/auth/login",body(code,password),"Cookie",cookie(pre,"__Secure-pet_staff_pre"),"X-CSRF-Token",data(pre).path("csrfToken").asText(),"Origin","https://authentication.example.invalid");assertEquals(200,web.statusCode());String sid=cookie(web,"__Secure-pet_staff_sid");
            var changed=call(second,"PUT","/api/admin/auth/password",json.writeValueAsString(Map.of("currentPassword",password,"newPassword",next)),"X-Staff-Token","Bearer "+a);
            assertEquals(200,changed.statusCode());assertEquals("COMPLETE",changed.headers().firstValue("X-Session-Cleanup").orElseThrow());
            assertEquals(401,call(first,"GET","/api/admin/auth/me",null,"X-Staff-Token","Bearer "+a).statusCode());assertEquals(401,call(second,"GET","/api/admin/auth/me",null,"X-Staff-Token","Bearer "+b).statusCode());assertEquals(401,call(second,"GET","/api/admin/auth/me",null,"Cookie",sid).statusCode());
            assertEquals(401,call(first,"POST","/api/admin/auth/token/login",body(code,password)).statusCode());String fresh=login(second,code,next);assertEquals(200,call(first,"GET","/api/admin/auth/me",null,"X-Staff-Token","Bearer "+fresh).statusCode());
            Files.writeString(Path.of("target/p05-03-two-instances.txt"),"两个独立生产JAR JVM共享正式PG/Redis：B改密→A/B旧小程序和WEB会话401；旧密码失败、新密码登录B→me A 200。\n");
        }
    }
    @Test void productionJarRejectsInsecureCookieAndUntrustedForwardingConfiguration()throws Exception {
        setup(UUID.randomUUID()+"临时配置");
        for(String setting:List.of("--pet.auth.cookie-secure=false","--server.forward-headers-strategy=framework")) {
            try(var app=start("prod",false,"invalid-"+(setting.contains("cookie")?"cookie":"forwarded"),setting)) {
                assertTrue(app.process().waitFor(20,TimeUnit.SECONDS));assertNotEquals(0,app.process().exitValue());
                assertTrue(Files.readString(app.log()).contains(setting.contains("cookie")?"pet.auth.cookie-secure":"server.forward-headers-strategy"));
            }
        }
    }
    @Test void idleExpiryDoesNotRenewOnMeAndAbsoluteExpiryCannotSlide()throws Exception {
        String password=UUID.randomUUID()+"临时期限";String code=setup(password);
        try(var app=start("test",true,"expiry")){
            ready(app);String t=login(app,code,password);Thread.sleep(1100);assertEquals(200,call(app,"GET","/api/admin/auth/me",null,"X-Staff-Token","Bearer "+t).statusCode());Thread.sleep(2100);assertEquals(401,call(app,"GET","/api/admin/auth/me",null,"X-Staff-Token","Bearer "+t).statusCode());
            String next=login(app,code,password);
            for(int i=0;i<3;i++){Thread.sleep(1000);assertEquals(404,call(app,"GET","/api/admin/missing-business-probe",null,"X-Staff-Token","Bearer "+next).statusCode());}
            Thread.sleep(1300);assertEquals(401,call(app,"GET","/api/admin/auth/me",null,"X-Staff-Token","Bearer "+next).statusCode());
            Files.writeString(Path.of("target/p05-02-expiry.txt"),"真实JAR/Redis：test专用绝对4s、闲置2s；me不续闲置，业务识别可更新活跃度，绝对期仍终止。生产配置不能缩短/覆盖冻结期限。\n");
        }
    }
}
