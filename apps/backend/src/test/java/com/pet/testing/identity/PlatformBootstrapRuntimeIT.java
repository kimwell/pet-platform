package com.pet.testing.identity;
import com.pet.testing.*;
import java.util.*;
import java.nio.file.*;
import java.sql.*;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;
/** 打包独立main与脚本真实stdin，不使用用户库，不在argv或文件写秘密。 */
class PlatformBootstrapRuntimeIT {
    @Test void packagedPlatformBootstrapUsesStdinAndDistinctCapabilityAndIsNotRepeatableCreation()throws Exception{
        try(var db=new PostgreSQLContainer(DockerImageName.parse(PostgresIntegrationSupport.IMAGE).asCompatibleSubstituteFor("postgres"))){
            db.start();IdentityDatabaseSupport.provision(db);Flyway.configure().dataSource(db.getJdbcUrl(),IdentityDatabaseSupport.MIGRATION,db.getPassword()).locations("classpath:db/migration").load().migrate();String secret=UUID.randomUUID()+"临时命令😀 ";
            var results=new ArrayList<String>();
            for(int i=0;i<2;i++){
                var builder=new ProcessBuilder("../../scripts/backend-identity.sh","platform-bootstrap","--admin-login","command-probe","--admin-name","命令技术管理员","--password-stdin").redirectErrorStream(true);
                builder.environment().put("JAVA_HOME",System.getProperty("java.home"));builder.environment().put("PET_PLATFORM_BOOTSTRAP_DATABASE_URL",db.getJdbcUrl().split("\\?",2)[0]);builder.environment().put("PET_PLATFORM_BOOTSTRAP_DATABASE_USERNAME",IdentityDatabaseSupport.PLATFORM_BOOTSTRAP);builder.environment().put("PET_PLATFORM_BOOTSTRAP_DATABASE_PASSWORD",db.getPassword());
                var process=builder.start();try(var stdin=process.getOutputStream()){stdin.write((secret+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));}
                String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);assertTrue(process.waitFor(30,java.util.concurrent.TimeUnit.SECONDS));assertEquals(i==0?0:2,process.exitValue());assertFalse(output.contains(secret));assertFalse(output.contains(db.getPassword()));assertFalse(output.contains("$pbkdf2"));results.add("exit="+process.exitValue()+" "+output);
            }
            Files.writeString(Path.of("target/p05-04-bootstrap-command.txt"),String.join("\n",results));
            try(var c=DriverManager.getConnection(db.getJdbcUrl(),db.getUsername(),db.getPassword());var s=c.createStatement();var rows=s.executeQuery("select count(*) from pet_control.platform_account")){rows.next();assertEquals(1,rows.getInt(1));}
        }
    }
}
