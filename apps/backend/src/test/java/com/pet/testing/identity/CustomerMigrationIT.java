package com.pet.testing.identity;
import com.pet.testing.*;import java.util.*;import java.sql.*;
import org.flywaydb.core.Flyway;import org.junit.jupiter.api.*;import org.testcontainers.postgresql.PostgreSQLContainer;import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;
/** 正式V3到V4升级，既有checksum/员工/平台身份不变，独立临时真实PostgreSQL。 */
class CustomerMigrationIT {
 @Test void versionThreeUpgradesWithoutChangingEarlierMigrationsOrCreatingCustomers()throws Exception{
  try(var db=new PostgreSQLContainer(DockerImageName.parse(PostgresIntegrationSupport.IMAGE).asCompatibleSubstituteFor("postgres"))){db.start();IdentityDatabaseSupport.provision(db);
   var v3=Flyway.configure().dataSource(db.getJdbcUrl(),IdentityDatabaseSupport.MIGRATION,db.getPassword()).locations("classpath:db/migration").target("3").load();assertEquals(3,v3.migrate().migrationsExecuted);
   var before=Arrays.stream(v3.info().applied()).map(i->i.getChecksum()).toList();var latest=Flyway.configure().dataSource(db.getJdbcUrl(),IdentityDatabaseSupport.MIGRATION,db.getPassword()).locations("classpath:db/migration").load();assertEquals(1,latest.migrate().migrationsExecuted);assertEquals(0,latest.migrate().migrationsExecuted);assertTrue(latest.validateWithResult().validationSuccessful);assertEquals(before,Arrays.stream(latest.info().applied()).limit(3).map(i->i.getChecksum()).toList());
   try(var c=DriverManager.getConnection(db.getJdbcUrl(),IdentityDatabaseSupport.RUNTIME,db.getPassword());var s=c.createStatement()){
    try(var q=s.executeQuery("select count(*) from public.customer_subject")){q.next();assertEquals(0,q.getInt(1));}
    try(var q=s.executeQuery("select count(*) from pg_proc p join pg_namespace n on n.oid=p.pronamespace where n.nspname='pet_customer' and p.prosecdef and 'search_path=pg_catalog, pg_temp'=any(p.proconfig)")){q.next();assertEquals(5,q.getInt(1));}
    assertThrows(SQLException.class,()->s.execute("select * from pet_customer.security_event"));assertThrows(SQLException.class,()->s.execute("insert into public.customer_subject(id,tenant_id,status) values(gen_random_uuid(),gen_random_uuid(),'ACTIVE')"));
   }
  }
 }
}
