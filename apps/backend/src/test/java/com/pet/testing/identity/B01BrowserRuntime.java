package com.pet.testing.identity;

import com.pet.platform.Application;
import com.pet.platform.identity.application.*;
import com.pet.platform.identity.application.bootstrap.*;
import com.pet.platform.identity.infrastructure.bootstrap.*;
import com.pet.testing.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** 手动指定-Dtest=B01BrowserRuntime的正式浏览器环境；不参与默认verify，无测试API或业务替身。 */
@SpringBootTest(classes=Application.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"pet.public-origin=http://127.0.0.1:18098"})
@ActiveProfiles("test")
class B01BrowserRuntime extends PostgresIntegrationSupport {
    @LocalServerPort int port;
    @Autowired PasswordService passwords;
    @Autowired JsonMapper json;
    @Test void holdRealRuntimeForBrowserAcceptance()throws Exception {
        var privateDir=Path.of("../../.local-data/b01");Files.createDirectories(privateDir);
        String secret=UUID.randomUUID()+"临时验收", employeeSecret=UUID.randomUUID()+"初始技术", newSecret=UUID.randomUUID()+"更新技术";
        var env=Map.of("PET_BOOTSTRAP_DATABASE_URL",POSTGRES.getJdbcUrl().split("\\?",2)[0],"PET_BOOTSTRAP_DATABASE_USERNAME",IdentityDatabaseSupport.BOOTSTRAP,"PET_BOOTSTRAP_DATABASE_PASSWORD",POSTGRES.getPassword());
        BootstrapResult tenant;try(var request=new BootstrapRequest("b01-browser","B01隔离联调租户","admin",secret.toCharArray(),"main","联调第一门店")){tenant=new IdentityBootstrap(new BootstrapJdbc(env),passwords).initialize(request);}
        new BootstrapJdbc(env).upgradeIdentityManagement(tenant.tenantId());
        var owner=new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword()));
        UUID second=UUID.randomUUID();owner.update("insert into platform_store(id,tenant_id,code,name,status) values(?,?,?,'联调第二门店','ACTIVE')",second,tenant.tenantId(),"second");
        owner.update("insert into identity_employee_store(id,tenant_id,employee_id,store_id) values(?,?,?,?)",UUID.randomUUID(),tenant.tenantId(),tenant.employeeId(),second);
        Path credentials=privateDir.resolve("credentials.json");Files.writeString(credentials,json.writeValueAsString(Map.of("adminPassword",secret,"employeePassword",employeeSecret,"newPassword",newSecret)));Files.setPosixFilePermissions(credentials,PosixFilePermissions.fromString("rw-------"));
        Files.writeString(privateDir.resolve("runtime.json"),json.writeValueAsString(Map.of("backendPort",port,"tenantId",tenant.tenantId(),"adminId",tenant.employeeId(),"storeId",tenant.storeId(),"secondStoreId",second,"postgres",POSTGRES.getContainerId(),"redis",RedisTestSupport.REDIS.getContainerId())));
        long deadline=System.nanoTime()+Duration.ofMinutes(30).toNanos();
        try{
            while(!Files.exists(privateDir.resolve("finish"))&&System.nanoTime()<deadline)Thread.sleep(500);
            assertTrue(Files.exists(privateDir.resolve("finish")),"浏览器验收等待超时，未关闭为PASS");
            String evidencePrefix=System.getProperty("pet.b01.evidence-prefix","browser");assertTrue(evidencePrefix.matches("[a-z-]{1,32}"));
            var state=owner.queryForList("select id,login_name,display_name,status,version,security_version,authorization_version,password_change_required from identity_employee where tenant_id=? order by login_name",tenant.tenantId());
            var result=new LinkedHashMap<String,Object>();result.put("employees",state);result.put("roles",owner.queryForList("select id,code,name,status,version from identity_role where tenant_id=? order by code",tenant.tenantId()));result.put("grants",owner.queryForList("select role_id,permission_code,scope_type from identity_role_permission where tenant_id=? order by role_id,permission_code,scope_type",tenant.tenantId()));result.put("employeeRoles",owner.queryForList("select employee_id,role_id from identity_employee_role where tenant_id=?",tenant.tenantId()));result.put("employeeStores",owner.queryForList("select employee_id,store_id from identity_employee_store where tenant_id=?",tenant.tenantId()));result.put("cleanupPending",owner.queryForObject("select count(*) from identity_session_cleanup where tenant_id=? and not completed",Integer.class,tenant.tenantId()));
            Files.writeString(Path.of("../../docs/testing/evidence/B01/"+evidencePrefix+"-final-database.json"),json.writeValueAsString(result));
        }finally{Files.deleteIfExists(credentials);}
    }
}
