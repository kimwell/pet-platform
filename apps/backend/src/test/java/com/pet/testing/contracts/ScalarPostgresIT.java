package com.pet.testing.contracts;

import com.pet.testing.PostgresIntegrationSupport;
import java.math.BigDecimal;
import java.sql.DriverManager;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 真实PostgreSQL标量往返，临时技术表随连接销毁；不增加正式迁移或Entity。 */
class ScalarPostgresIT extends PostgresIntegrationSupport {
    @Test void protocolScalarTypesSurvivePostgresWithoutFloatingPointOrTimezoneShift() throws Exception {
        var id = UUID.fromString("12345678-1234-4234-8234-123456789abc");
        var instant = Instant.parse("2026-10-08T02:20:30.120Z");
        try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            try (var statement = connection.createStatement()) {
                statement.execute("SET TIME ZONE 'America/New_York'");
                statement.execute("CREATE TEMPORARY TABLE p03_scalar_fixture(id uuid, amount numeric(19,2), total bigint, occurred_at timestamptz(3), business_date date)");
            }
            try (var insert = connection.prepareStatement("INSERT INTO p03_scalar_fixture VALUES (?, ?, ?, ?, ?)")) {
                insert.setObject(1, id); insert.setBigDecimal(2, new BigDecimal("99999999999999999.99"));
                insert.setLong(3, 9007199254740993L); insert.setObject(4, instant.atOffset(ZoneOffset.UTC));
                insert.setObject(5, LocalDate.of(2026, 10, 8)); assertEquals(1, insert.executeUpdate());
            }
            try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT * FROM p03_scalar_fixture")) {
                assertTrue(result.next()); assertEquals(id, result.getObject("id", UUID.class));
                assertEquals("99999999999999999.99", result.getBigDecimal("amount").toPlainString());
                assertEquals("9007199254740993", Long.toString(result.getLong("total")));
                assertEquals(instant, result.getObject("occurred_at", OffsetDateTime.class).toInstant());
                assertEquals(LocalDate.of(2026, 10, 8), result.getObject("business_date", LocalDate.class));
            }
        }
    }
}
