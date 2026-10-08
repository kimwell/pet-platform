package com.pet.testing.identity;
import com.pet.testing.*;
import com.pet.platform.identity.application.*;
import com.pet.platform.identity.application.bootstrap.*;
import com.pet.platform.identity.infrastructure.bootstrap.*;
import java.util.*;
import java.sql.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;
/** 上一正式V2已含真实租户数据；仅追加V3，不改checksum/旧授权。 */
class PlatformMigrationIT {
    @Test void previousVersionTwoUpgradesWithoutTouchingTenantDataAndDoesNotBootstrap()throws Exception{
        try(var db=new PostgreSQLContainer(DockerImageName.parse(PostgresIntegrationSupport.IMAGE).asCompatibleSubstituteFor("postgres"))){
            db.start();IdentityDatabaseSupport.provision(db);
            var v2=Flyway.configure().dataSource(db.getJdbcUrl(),IdentityDatabaseSupport.MIGRATION,db.getPassword()).locations("classpath:db/migration").target("2").load();assertEquals(2,v2.migrate().migrationsExecuted);
            String password=UUID.randomUUID()+" 升级技术秘密😀";BootstrapResult staff;
            try(var r=new BootstrapRequest("upgrade-platform","升级技术租户","admin",password.toCharArray(),null,null)){staff=new IdentityBootstrap(new BootstrapJdbc(Map.of("PET_BOOTSTRAP_DATABASE_URL",db.getJdbcUrl().split("\\?",2)[0],"PET_BOOTSTRAP_DATABASE_USERNAME",IdentityDatabaseSupport.BOOTSTRAP,"PET_BOOTSTRAP_DATABASE_PASSWORD",db.getPassword())),new PasswordService()).initialize(r);}
            Map<String,Integer> checksums=new HashMap<>();
            try(var c=DriverManager.getConnection(db.getJdbcUrl(),db.getUsername(),db.getPassword());var s=c.createStatement();var q=s.executeQuery("select version,checksum from flyway_schema_history")){while(q.next())checksums.put(q.getString(1),q.getInt(2));}
            var latest=Flyway.configure().dataSource(db.getJdbcUrl(),IdentityDatabaseSupport.MIGRATION,db.getPassword()).locations("classpath:db/migration").load();assertEquals(1,latest.migrate().migrationsExecuted);assertEquals(0,latest.migrate().migrationsExecuted);assertTrue(latest.validateWithResult().validationSuccessful);
            try(var c=DriverManager.getConnection(db.getJdbcUrl(),db.getUsername(),db.getPassword());var s=c.createStatement()){
                try(var q=s.executeQuery("select version,checksum from flyway_schema_history where version in ('1','2')")){while(q.next())assertEquals(checksums.get(q.getString(1)),q.getInt(2));}
                try(var q=s.executeQuery("select count(*) from pet_control.platform_account")){q.next();assertEquals(0,q.getInt(1));}
                try(var q=c.prepareStatement("select password_hash,security_version from identity_employee where id=?")){q.setObject(1,staff.employeeId());try(var rows=q.executeQuery()){assertTrue(rows.next());assertTrue(new PasswordService().matches(password.toCharArray(),rows.getString(1)));assertEquals(0,rows.getLong(2));}}
                try(var q=s.executeQuery("select count(*) from identity_role_permission")){q.next();assertEquals(9,q.getInt(1));}
                try(var q=s.executeQuery("select count(*) from pg_class c join pg_namespace n on n.oid=c.relnamespace where n.nspname='pet_control' and c.relkind='r' and c.relrowsecurity and c.relforcerowsecurity")){q.next();assertEquals(5,q.getInt(1));}
                try(var q=s.executeQuery("select count(*) from pg_proc p join pg_namespace n on n.oid=p.pronamespace where n.nspname='pet_control' and p.prosecdef and 'search_path=pg_catalog, pg_temp'=any(p.proconfig)")){q.next();assertEquals(8,q.getInt(1));}
            }
        }
    }
}
