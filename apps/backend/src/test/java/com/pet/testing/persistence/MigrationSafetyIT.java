package com.pet.testing.persistence;

import com.pet.platform.Application;
import com.pet.testing.PostgresIntegrationSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

/** 专用临时容器及 schema；不会修改开发数据库或源迁移的历史和校验和。 */
@Testcontainers
class MigrationSafetyIT {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse(PostgresIntegrationSupport.IMAGE).asCompatibleSubstituteFor("postgres"));

    private Flyway flyway(String schema, String location) {
        return Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema).defaultSchema(schema).locations(location)
                .cleanDisabled(true).baselineOnMigrate(false).validateOnMigrate(true).outOfOrder(false)
                .ignoreMigrationPatterns(new String[0]).load();
    }

    private void sql(String sql) throws Exception {
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) { statement.execute(sql); }
    }

    @Test void emptyDatabaseCreatesHistoryAndRepeatedMigrationIsIdempotent() {
        var flyway = flyway("empty_migration", "classpath:persistence-migrations");
        assertEquals(1, flyway.migrate().migrationsExecuted);
        assertEquals(1, java.util.Arrays.stream(flyway.info().applied()).filter(info -> info.getVersion() != null).count());
        assertEquals(0, flyway.migrate().migrationsExecuted);
        assertTrue(flyway.validateWithResult().validationSuccessful);
    }

    @Test void nonEmptySchemaWithoutHistoryCannotBeAutomaticallyBaselined() throws Exception {
        sql("create schema nonempty_migration; create table nonempty_migration.preexisting_probe (id uuid)");
        assertThrows(FlywayException.class, () -> flyway("nonempty_migration", "classpath:persistence-migrations").migrate());
    }

    @Test void checksumConflictFailsAgainstTemporaryCopy(@TempDir Path directory) throws Exception {
        Path migration = directory.resolve("V1__shared_persistence_probe.sql");
        try (var input = getClass().getResourceAsStream("/persistence-migrations/V1__shared_persistence_probe.sql")) {
            Files.copy(input, migration);
        }
        var flyway = flyway("checksum_migration", "filesystem:" + directory.toAbsolutePath());
        flyway.migrate();
        // 只修改临时迁移副本，生产及测试源文件保持原样。
        Files.writeString(migration, Files.readString(migration, StandardCharsets.UTF_8) + "\nSELECT 1;\n");
        assertFalse(flyway.validateWithResult().validationSuccessful);
        assertThrows(FlywayException.class, flyway::migrate);
    }

    @Test void cleanIsRefused() {
        var flyway = flyway("clean_disabled", "classpath:persistence-migrations");
        flyway.migrate();
        assertThrows(FlywayException.class, flyway::clean);
        assertEquals(1, java.util.Arrays.stream(flyway.info().applied()).filter(info -> info.getVersion() != null).count());
    }

    @Test void mismatchedStructureFailsJpaValidation() throws Exception {
        flyway("mismatch_validation", "classpath:persistence-migrations").migrate();
        sql("alter table mismatch_validation.persistence_probe drop column display_name");
        var failure = assertThrows(Exception.class, () -> {
            try (var context = new SpringApplicationBuilder(Application.class, PersistenceFixtures.class)
                    .web(WebApplicationType.NONE).profiles("test")
                    .initializers(applicationContext -> applicationContext.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("technicalRedis", java.util.Map.of("spring.data.redis.host", com.pet.testing.RedisTestSupport.REDIS.getHost(),
                            "spring.data.redis.port", com.pet.testing.RedisTestSupport.REDIS.getMappedPort(6379),
                            "spring.data.redis.password", com.pet.testing.RedisTestSupport.password()))))
                    .run(
                            "--spring.datasource.url=" + POSTGRES.getJdbcUrl().split("\\?", 2)[0],
                            "--spring.datasource.username=" + POSTGRES.getUsername(),
                            "--spring.datasource.password=" + POSTGRES.getPassword(),
                            "--spring.flyway.enabled=false",
                            "--spring.datasource.hikari.schema=mismatch_validation",
                            "--spring.jpa.properties.hibernate.default_schema=mismatch_validation")) {
                fail("结构缺失时不得启动成功");
            }
        });
        Throwable cause = failure;
        while (cause.getCause() != null) { cause = cause.getCause(); }
        assertTrue(cause.getMessage().contains("missing column [display_name]"), cause::getMessage);
    }
}
