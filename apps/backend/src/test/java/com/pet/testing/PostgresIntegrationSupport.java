package com.pet.testing;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** 真实 PostgreSQL 技术夹具：独立临时容器，不连接开发者日常数据库，不跳过 Docker 失败。 */
public abstract class PostgresIntegrationSupport {
    public static final String IMAGE = "postgres:17.11-bookworm@sha256:3645570cccdfa447589da9f57dd740faa29b30938e861289a5574b6ca6b03826";
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("p03_technical_fixture");

    static {
        POSTGRES.start();
        Runtime.getRuntime().addShutdownHook(new Thread(POSTGRES::stop, "p03-postgres-cleanup"));
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        RedisTestSupport.properties(registry);
        // Testcontainers 自带 loggerLevel 查询参数；应用 URL 保持无秘密/无参数形式。
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl().split("\\?", 2)[0]);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
