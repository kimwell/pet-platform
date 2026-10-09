package com.pet.testing.identity;

import com.pet.platform.identity.application.PasswordService;
import com.pet.platform.identity.application.bootstrap.*;
import com.pet.platform.identity.infrastructure.bootstrap.BootstrapJdbc;
import com.pet.testing.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;

/** 使用package后的生产JAR入口，而非测试类路径命令；只操作独立容器。 */
class EmployeeReadUpgradeRuntimeIT {
    @Test void packagedExplicitCommandRunsAndRejectsRuntimeCredentials(@TempDir Path temporary)throws Exception {
        try(var db=new PostgreSQLContainer(DockerImageName.parse(PostgresIntegrationSupport.IMAGE).asCompatibleSubstituteFor("postgres"))) {
            db.start();IdentityDatabaseSupport.provision(db);
            Flyway.configure().dataSource(db.getJdbcUrl(),IdentityDatabaseSupport.MIGRATION,db.getPassword()).locations("classpath:db/migration").load().migrate();
            var env=Map.of("PET_BOOTSTRAP_DATABASE_URL",db.getJdbcUrl().split("\\?",2)[0],"PET_BOOTSTRAP_DATABASE_USERNAME",IdentityDatabaseSupport.BOOTSTRAP,"PET_BOOTSTRAP_DATABASE_PASSWORD",db.getPassword());
            BootstrapResult initialized;
            try(var input=new BootstrapRequest("jar-upgrade","生产包技术租户","admin",(UUID.randomUUID()+"技术临时").toCharArray(),null,null)) {
                initialized=new IdentityBootstrap(new BootstrapJdbc(env),new PasswordService()).initialize(input);
            }
            assertEquals(0,run(env,initialized.tenantId(),temporary.resolve("success.log")));
            assertEquals(0,run(env,initialized.tenantId(),temporary.resolve("idempotent.log")));
            assertTrue(Files.readString(temporary.resolve("idempotent.log")).contains("新增范围条数=0"));
            var restricted=new HashMap<>(env);restricted.put("PET_BOOTSTRAP_DATABASE_USERNAME",IdentityDatabaseSupport.RUNTIME);
            assertEquals(2,run(restricted,initialized.tenantId(),temporary.resolve("denied.log")));
            assertFalse(Files.readString(temporary.resolve("denied.log")).contains(db.getPassword()));
            try(var c=DriverManager.getConnection(db.getJdbcUrl(),db.getUsername(),db.getPassword());var s=c.createStatement();var r=s.executeQuery("select permission_code,scope_type from identity_role_permission where permission_code='identity:user:detail'")) {
                assertTrue(r.next());assertEquals("TENANT",r.getString(2));assertFalse(r.next());
            }
        }
    }
    int run(Map<String,String> env,UUID tenant,Path log)throws Exception {
        var process=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),
                "-Dloader.main=com.pet.platform.identity.infrastructure.bootstrap.EmployeeReadUpgradeCommand",
                "-cp",Path.of("target/pet-platform-backend-0.0.0-SNAPSHOT.jar").toAbsolutePath().toString(),
                "org.springframework.boot.loader.launch.PropertiesLauncher","--tenant-id",tenant.toString());
        process.environment().putAll(env);process.redirectErrorStream(true);process.redirectOutput(log.toFile());var started=process.start();
        if(!started.waitFor(20,TimeUnit.SECONDS)) {started.destroyForcibly();fail("生产包命令未在限制时间内完成");}
        var evidence=Path.of("target/p07-01-command-observations");Files.createDirectories(evidence);
        Files.writeString(evidence.resolve(log.getFileName()+".json"),new tools.jackson.databind.json.JsonMapper().writeValueAsString(
                Map.of("cwd",Path.of("").toAbsolutePath().toString(),"command",process.command(),"exitCode",started.exitValue(),"output",Files.readString(log))));
        return started.exitValue();
    }
}
