package com.pet.testing.identity;

import com.pet.platform.identity.application.PasswordService;
import com.pet.platform.identity.application.bootstrap.*;
import com.pet.platform.identity.infrastructure.bootstrap.*;
import com.pet.testing.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;

/** 带正式V4数据升级；独立迁移/初始化/运行角色，禁止启动扩大授权。 */
class EmployeeReadMigrationIT {
    @Test void versionFourDataIsPreservedAndOnlyExplicitUpgradeAddsDetail()throws Exception {
        try(var db=new PostgreSQLContainer(DockerImageName.parse(PostgresIntegrationSupport.IMAGE).asCompatibleSubstituteFor("postgres"))) {
            db.start();IdentityDatabaseSupport.provision(db);
            var before=Flyway.configure().dataSource(db.getJdbcUrl(),IdentityDatabaseSupport.MIGRATION,db.getPassword()).locations("classpath:db/migration").target("4").load();
            assertEquals(4,before.migrate().migrationsExecuted);
            var checksums=Arrays.stream(before.info().applied()).map(i->i.getChecksum()).toList();
            var env=Map.of("PET_BOOTSTRAP_DATABASE_URL",db.getJdbcUrl().split("\\?",2)[0],"PET_BOOTSTRAP_DATABASE_USERNAME",IdentityDatabaseSupport.BOOTSTRAP,"PET_BOOTSTRAP_DATABASE_PASSWORD",db.getPassword());
            BootstrapResult initialized;
            try(var request=new BootstrapRequest("upgrade","升级技术租户","admin",(UUID.randomUUID()+"临时技术").toCharArray(),null,null)) {
                initialized=new IdentityBootstrap(new BootstrapJdbc(env),new PasswordService()).initialize(request);
            }
            var latest=Flyway.configure().dataSource(db.getJdbcUrl(),IdentityDatabaseSupport.MIGRATION,db.getPassword()).locations("classpath:db/migration").target("5").load();
            assertEquals(1,latest.migrate().migrationsExecuted);assertEquals(0,latest.migrate().migrationsExecuted);
            assertEquals(checksums,Arrays.stream(latest.info().applied()).limit(4).map(i->i.getChecksum()).toList());
            try(var c=DriverManager.getConnection(db.getJdbcUrl(),db.getUsername(),db.getPassword());var s=c.createStatement()) {
                try(var r=s.executeQuery("select count(*) from identity_role_permission")) {r.next();assertEquals(9,r.getInt(1));}
                try(var r=s.executeQuery("select id,authorization_version from identity_employee")) {r.next();assertEquals(initialized.employeeId(),r.getObject(1));assertEquals(0,r.getLong(2));}
                // 无该命令参数可选择的普通角色；不得受到补充影响。
                s.execute("insert into identity_role(id,tenant_id,code,name,status) values(gen_random_uuid(),'"+initialized.tenantId()+"','ordinary','普通技术角色','ACTIVE')");
            }
            try(var workers=Executors.newFixedThreadPool(2)) {
                var x=workers.submit(()->new BootstrapJdbc(env).upgradeEmployeeRead(initialized.tenantId()));
                var y=workers.submit(()->new BootstrapJdbc(env).upgradeEmployeeRead(initialized.tenantId()));
                assertEquals(1,x.get(15,TimeUnit.SECONDS)+y.get(15,TimeUnit.SECONDS));
            }
            try(var c=DriverManager.getConnection(db.getJdbcUrl(),db.getUsername(),db.getPassword());var s=c.createStatement()) {
                try(var r=s.executeQuery("select count(*) from identity_role_permission where permission_code='identity:user:detail'")) {r.next();assertEquals(1,r.getInt(1));}
                try(var r=s.executeQuery("select authorization_version,version from identity_employee")) {r.next();assertEquals(1,r.getLong(1));assertEquals(1,r.getLong(2));}
                try(var r=s.executeQuery("select count(*) from identity_role_permission p join identity_role r on r.id=p.role_id where r.code='ordinary'")) {r.next();assertEquals(0,r.getInt(1));}
                try(var r=s.executeQuery("select prosecdef,'search_path=pg_catalog, pg_temp'=any(proconfig),pg_get_userbyid(proowner) from pg_proc where oid='pet_identity.upgrade_employee_read(uuid)'::regprocedure")) {r.next();assertTrue(r.getBoolean(1));assertTrue(r.getBoolean(2));assertEquals("pet_bootstrap_owner",r.getString(3));}
            }
            try(var c=DriverManager.getConnection(db.getJdbcUrl(),IdentityDatabaseSupport.RUNTIME,db.getPassword());var s=c.createStatement()) {
                assertThrows(SQLException.class,()->s.execute("select pet_identity.upgrade_employee_read('"+initialized.tenantId()+"')"));
                assertThrows(SQLException.class,()->s.execute("set role pet_bootstrap_owner"));
                try(var r=s.executeQuery("select rolsuper,rolbypassrls from pg_roles where rolname=current_user")) {r.next();assertFalse(r.getBoolean(1));assertFalse(r.getBoolean(2));}
            }
        }
    }
}
