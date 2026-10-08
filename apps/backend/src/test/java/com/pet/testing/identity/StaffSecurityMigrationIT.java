package com.pet.testing.identity;

import com.pet.platform.identity.application.PasswordService;
import com.pet.platform.identity.application.bootstrap.*;
import com.pet.platform.identity.infrastructure.bootstrap.BootstrapJdbc;
import com.pet.testing.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;

/** 从正式V1且已有真实初始化数据升级V2；不使用或清理用户数据库。 */
class StaffSecurityMigrationIT {
    @Test void versionOneDataAndChecksumSurviveUpgradeAndNewTablesKeepRestrictedRls()throws Exception {
        try(var database=new PostgreSQLContainer(DockerImageName.parse(PostgresIntegrationSupport.IMAGE).asCompatibleSubstituteFor("postgres"))) {
            database.start();IdentityDatabaseSupport.provision(database);
            var v1=Flyway.configure().dataSource(database.getJdbcUrl(),IdentityDatabaseSupport.MIGRATION,database.getPassword()).locations("classpath:db/migration").target("1").load();
            assertEquals(1,v1.migrate().migrationsExecuted);
            String secret=UUID.randomUUID()+" 临时升级😀 ";BootstrapResult identity;
            try(var input=new BootstrapRequest("upgrade-security","升级技术租户","admin",secret.toCharArray(),"store","升级门店")) {
                identity=new IdentityBootstrap(new BootstrapJdbc(Map.of("PET_BOOTSTRAP_DATABASE_URL",database.getJdbcUrl().split("\\?",2)[0],"PET_BOOTSTRAP_DATABASE_USERNAME",IdentityDatabaseSupport.BOOTSTRAP,"PET_BOOTSTRAP_DATABASE_PASSWORD",database.getPassword())),new PasswordService()).initialize(input);
            }
            int checksum;
            try(var c=DriverManager.getConnection(database.getJdbcUrl(),database.getUsername(),database.getPassword());var q=c.createStatement();var r=q.executeQuery("select checksum from flyway_schema_history where version='1'")){r.next();checksum=r.getInt(1);}
            var current=Flyway.configure().dataSource(database.getJdbcUrl(),IdentityDatabaseSupport.MIGRATION,database.getPassword()).locations("classpath:db/migration").target("2").load();
            assertEquals(1,current.migrate().migrationsExecuted);assertEquals(0,current.migrate().migrationsExecuted);assertTrue(current.validateWithResult().validationSuccessful);
            try(var c=DriverManager.getConnection(database.getJdbcUrl(),database.getUsername(),database.getPassword());var s=c.createStatement()) {
                try(var r=s.executeQuery("select checksum from flyway_schema_history where version='1'")){r.next();assertEquals(checksum,r.getInt(1));}
                try(var q=c.prepareStatement("select password_hash,security_version,password_change_required,system_reserved from identity_employee where id=?")){q.setObject(1,identity.employeeId());try(var r=q.executeQuery()){assertTrue(r.next());assertTrue(new PasswordService().matches(secret.toCharArray(),r.getString(1)));assertEquals(0,r.getLong(2));assertFalse(r.getBoolean(3));assertFalse(r.getBoolean(4));}}
                try(var r=s.executeQuery("select count(*) from identity_role_permission")){r.next();assertEquals(9,r.getInt(1));}
            }
            try(var c=DriverManager.getConnection(database.getJdbcUrl(),IdentityDatabaseSupport.RUNTIME,database.getPassword());var s=c.createStatement()) {
                try(var r=s.executeQuery("select count(*) from identity_session_cleanup")){r.next();assertEquals(0,r.getInt(1));}
                assertThrows(SQLException.class,() -> s.executeQuery("select password_hash from identity_employee"));
                assertThrows(SQLException.class,() -> s.executeUpdate("update identity_employee set system_reserved=true"));
                assertThrows(SQLException.class,() -> s.executeUpdate("delete from identity_security_event"));
                c.setAutoCommit(false);s.execute("select set_config('pet.tenant_id','"+UUID.randomUUID()+"',true)");
                assertThrows(SQLException.class,() -> {try(var q=c.prepareStatement("insert into identity_session_cleanup(id,tenant_id,employee_id,revoke_before) values(?,?,?,1)")){q.setObject(1,UUID.randomUUID());q.setObject(2,identity.tenantId());q.setObject(3,identity.employeeId());q.executeUpdate();}});c.rollback();
            }
        }
    }
}
