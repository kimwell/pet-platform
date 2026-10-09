package com.pet.testing.identity;

import com.pet.testing.IdentityDatabaseSupport;
import com.pet.testing.PostgresIntegrationSupport;
import java.nio.file.Path;
import java.util.List;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.*;

/** P03/P04生产没有SQL；只测试真实存在的空history→首次正式V1，不冒充历史业务升级。 */
@Testcontainers
class IdentityMigrationIT {
    @Container static final PostgreSQLContainer POSTGRES=new PostgreSQLContainer(DockerImageName.parse(PostgresIntegrationSupport.IMAGE).asCompatibleSubstituteFor("postgres"));
    @BeforeAll static void provision() { IdentityDatabaseSupport.provision(POSTGRES); }
    @Test void previouslyEmptyProductionHistoryUpgradesThenRemainsIdempotent(@TempDir Path emptyLocation) throws Exception {
        var empty=Flyway.configure().dataSource(POSTGRES.getJdbcUrl(),IdentityDatabaseSupport.MIGRATION,POSTGRES.getPassword())
            .locations("filesystem:"+emptyLocation).cleanDisabled(true).baselineOnMigrate(false).load();
        assertEquals(0,empty.migrate().migrationsExecuted);
        var production=Flyway.configure().dataSource(POSTGRES.getJdbcUrl(),IdentityDatabaseSupport.MIGRATION,POSTGRES.getPassword())
            .locations("classpath:db/migration").cleanDisabled(true).baselineOnMigrate(false).validateOnMigrate(true).outOfOrder(false).ignoreMigrationPatterns(new String[0]).load();
        assertEquals(8,production.migrate().migrationsExecuted);assertEquals(0,production.migrate().migrationsExecuted);
        assertTrue(production.validateWithResult().validationSuccessful);
        try(var c=DriverManager.getConnection(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());var s=c.createStatement();var r=s.executeQuery("select count(*) from public.identity_employee")) {
            r.next();assertEquals(0,r.getInt(1));
        }
    }
}
