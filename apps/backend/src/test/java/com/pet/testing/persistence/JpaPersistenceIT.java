package com.pet.testing.persistence;

import com.pet.platform.Application;
import com.pet.platform.shared.api.*;
import com.pet.platform.shared.persistence.JpaPageAdapter;
import com.pet.testing.PostgresIntegrationSupport;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class, properties = "spring.flyway.locations=classpath:persistence-migrations")
@ActiveProfiles("test")
@Import(PersistenceFixtures.class)
@org.testcontainers.junit.jupiter.Testcontainers
class JpaPersistenceIT {
    @org.testcontainers.junit.jupiter.Container
    static final org.testcontainers.postgresql.PostgreSQLContainer POSTGRES = new org.testcontainers.postgresql.PostgreSQLContainer(
            org.testcontainers.utility.DockerImageName.parse(PostgresIntegrationSupport.IMAGE).asCompatibleSubstituteFor("postgres"));

    @org.springframework.test.context.DynamicPropertySource
    static void databaseProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        com.pet.testing.RedisTestSupport.properties(registry);
        // Testcontainers 自带 loggerLevel 查询参数；应用 URL 保持无秘密/无参数形式。
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl().split("\\?", 2)[0]);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired ProbeApplicationService service;
    @Autowired ProbeRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired javax.sql.DataSource dataSource;
    @Autowired Flyway flyway;
    @Autowired PersistenceFixtures.MutableClock clock;
    private static final Instant FIRST = Instant.parse("2026-10-08T01:02:03.123Z");
    private static final SortWhitelist SORTS = new SortWhitelist(
            Map.of("createdAt", "createdAt", "name", "displayName", "id", "id"),
            "createdAt", SortRule.Direction.DESC, "id");

    @BeforeEach void resetTechnicalFixture() {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "测试方法不持有自动回滚事务");
        repository.deleteAllInBatch();
        clock.set(Instant.parse("2026-10-08T01:02:03.123456789Z"));
    }

    @Test void migrationCompletedBeforeJpaValidationAndDoesNotRepeat() {
        assertEquals("1", flyway.info().current().getVersion().getVersion());
        assertEquals(1, jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class));
        assertEquals(0, flyway.migrate().migrationsExecuted);
        assertEquals(1, jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test void uuidV4AndAuditingSurviveRealPostgresRoundTrip() {
        UUID id = service.create("uuid", "技术夹具", "audit");
        assertEquals(4, id.version());
        assertEquals(2, id.variant());
        var entity = service.read(id);
        assertEquals(id, entity.getId());
        assertEquals(FIRST, entity.getCreatedAt());
        assertEquals(FIRST, entity.getUpdatedAt());
        assertEquals(id.toString(), id.toString().toLowerCase());
    }

    @Test void auditUpdatesOnlyOnActualChangeAndKeepsCreatedAt() {
        UUID id = service.create("audit", "初值", "audit");
        Instant second = Instant.parse("2026-10-08T01:02:05.987Z");
        clock.set(second.plusNanos(654321));
        service.rename(id, "已更新");
        var changed = service.read(id);
        assertEquals(FIRST, changed.getCreatedAt());
        assertEquals(second, changed.getUpdatedAt());
        clock.set(second.plusSeconds(10));
        service.rename(id, "已更新");
        assertEquals(second, service.read(id).getUpdatedAt());
    }

    @Test void timestampHasMillisecondPrecisionAndUtcInstantAcrossSessionTimezone() throws Exception {
        UUID id = service.create("time", "时间夹具", "audit");
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("set time zone 'Asia/Shanghai'");
            try (var query = connection.prepareStatement("select created_at from persistence_probe where id = ?")) {
                query.setObject(1, id);
                try (var result = query.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(FIRST, result.getObject(1, OffsetDateTime.class).toInstant());
                }
            } finally { statement.execute("set time zone 'UTC'"); }
            assertEquals(3, jdbc.queryForObject("select datetime_precision from information_schema.columns where table_name='persistence_probe' and column_name='created_at'", Integer.class));
        }
    }

    @Test void applicationServiceCommitsOutsideTestTransaction() {
        UUID id = service.create("committed", "提交夹具", "tx");
        assertEquals(id, jdbc.queryForObject("select id from persistence_probe where code='committed'", UUID.class));
    }

    @Test void runtimeFailureRollsBackFlushedWrite() {
        assertThrows(IllegalStateException.class, () -> service.failAfterWrite("rollback"));
        assertEquals(0L, repository.count());
    }

    @Test void runtimeFailureRollsBackAllWritesInSameServiceTransaction() {
        assertThrows(IllegalStateException.class, () -> service.failAfterTwoWrites("first", "second"));
        assertEquals(0, jdbc.queryForObject("select count(*) from persistence_probe", Integer.class));
    }

    @Test void databaseConstraintFailureAtFlushRollsBackEarlierWrite() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> service.failOnConstraint("duplicate"));
        assertEquals(0, jdbc.queryForObject("select count(*) from persistence_probe", Integer.class));
    }

    @Test void databasePagesKeepStableUniqueOrderAndActualTotal() {
        List<UUID> ids = List.of(service.create("a", "同名", "page"), service.create("b", "同名", "page"),
                service.create("c", "同名", "page"));
        var first = repository.findByTag("page", pageable(1, 2, Map.of()));
        var second = repository.findByTag("page", pageable(2, 2, Map.of()));
        var expected = jdbc.queryForList("select id from persistence_probe order by created_at desc, id desc", UUID.class);
        var actual = java.util.stream.Stream.concat(first.stream(), second.stream()).map(PersistenceProbe::getId).toList();
        assertEquals(expected, actual);
        assertEquals(3, actual.stream().distinct().count());
        assertTrue(actual.containsAll(ids));
        var response = JpaPageAdapter.fromPage(first.map(PersistenceProbe::getCode));
        assertEquals(1, response.page());
        assertEquals("3", response.total());
        assertEquals(2, response.items().size());
    }

    @Test void nullableSortUsesNullsLastForBothDirectionsAndSpecifications() {
        service.create("null", null, "sort");
        service.create("a", "a", "sort");
        service.create("b", "b", "sort");
        for (String direction : List.of("asc", "desc")) {
            var parameters = Map.of("sortBy", new String[]{"name"}, "sortOrder", new String[]{direction});
            var page = repository.findAll((root, query, cb) -> cb.equal(root.get("tag"), "sort"), pageable(1, 20, parameters));
            assertEquals(direction.equals("asc") ? List.of("a", "b", "null") : List.of("b", "a", "null"),
                    page.map(PersistenceProbe::getCode).getContent());
        }
    }

    @Test void filtersComposeWithPageAndEmptyOrOutOfRangeResults() {
        service.create("include1", "a", "selected");
        service.create("include2", "b", "selected");
        service.create("exclude", "c", "other");
        var selected = repository.findByTag("selected", pageable(1, 1, Map.of()));
        assertEquals("2", JpaPageAdapter.fromPage(selected.map(PersistenceProbe::getCode)).total());
        var empty = JpaPageAdapter.fromPage(repository.findByTag("absent", pageable(1, 20, Map.of())).map(PersistenceProbe::getCode));
        assertEquals("0", empty.total());
        assertTrue(empty.items().isEmpty());
        var beyond = JpaPageAdapter.fromPage(repository.findByTag("selected", pageable(8, 2, Map.of())).map(PersistenceProbe::getCode));
        assertEquals(8, beyond.page());
        assertEquals("2", beyond.total());
        assertTrue(beyond.items().isEmpty());
    }

    @Test void directUniqueSortIsNotDuplicatedAndCountReflectsLaterChanges() {
        UUID id = service.create("live-count", "a", "page");
        var page = pageable(1, 20, Map.of("sortBy", new String[]{"id"}));
        assertEquals(1, page.getSort().stream().count());
        assertEquals("1", JpaPageAdapter.fromPage(repository.findAll(page).map(PersistenceProbe::getCode)).total());
        repository.deleteById(id);
        assertEquals("0", JpaPageAdapter.fromPage(repository.findAll(page).map(PersistenceProbe::getCode)).total());
    }

    private org.springframework.data.domain.Pageable pageable(int page, int size, Map<String, String[]> parameters) {
        return JpaPageAdapter.toPageable(new PageQuery(page, size), SORTS, parameters);
    }
}
