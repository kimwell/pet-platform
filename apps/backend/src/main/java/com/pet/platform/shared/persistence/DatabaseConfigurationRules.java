package com.pet.platform.shared.persistence;

import com.pet.platform.shared.config.EnvironmentSettings;
import java.net.URI;
import org.springframework.core.env.Environment;

/** 安全默认值是运行约束，不能通过外部配置关闭全部结构校验。 */
final class DatabaseConfigurationRules {
    private DatabaseConfigurationRules() { }

    static void validate(Environment environment) {
        var settings = new EnvironmentSettings(environment.getProperty("pet.environment"),
                publicOrigin(environment));
        settings.verifyProfiles(environment.getActiveProfiles());
        String url = required(environment, "spring.datasource.url");
        required(environment, "spring.datasource.username");
        required(environment, "spring.datasource.password");
        try {
            if (!url.startsWith("jdbc:postgresql://")) { throw new IllegalArgumentException(); }
            URI uri = URI.create(url.substring(5));
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                    || uri.getFragment() != null || !uri.getPath().matches("/[A-Za-z0-9_-]+")
                    || uri.getPort() == 0 || uri.getPort() > 65535) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException("spring.datasource.url 必须是不含凭据或查询参数的 PostgreSQL JDBC 地址");
        }
        if ("true".equals(environment.getProperty("spring.flyway.enabled"))) {
            if(!url.equals(required(environment,"spring.flyway.url"))) throw new IllegalStateException("运行与迁移数据库地址必须一致");
            required(environment,"spring.flyway.user");
            required(environment,"spring.flyway.password");
        }
        expect(environment, "spring.jpa.open-in-view", "false");
        expect(environment, "spring.jpa.properties.hibernate.cache.use_second_level_cache", "false");
        expect(environment, "spring.jpa.properties.hibernate.cache.use_query_cache", "false");
        expect(environment, "spring.jpa.generate-ddl", "false");
        expect(environment, "spring.jpa.hibernate.ddl-auto", "validate");
        optionalExpected(environment, "spring.jpa.properties.hibernate.hbm2ddl.auto", "validate");
        optionalExpected(environment, "spring.jpa.properties.jakarta.persistence.schema-generation.database.action", "none");
        expect(environment, "spring.jpa.properties.hibernate.jdbc.time_zone", "UTC");
        expect(environment, "spring.jpa.properties.hibernate.order_by.default_null_ordering", "last");
        expect(environment, "spring.sql.init.mode", "never");
        expect(environment, "spring.flyway.clean-disabled", "true");
        expect(environment, "spring.flyway.baseline-on-migrate", "false");
        expect(environment, "spring.flyway.validate-on-migrate", "true");
        expect(environment, "spring.flyway.out-of-order", "false");
        expect(environment, "spring.flyway.validate-migration-naming", "true");
        expect(environment, "spring.flyway.fail-on-missing-locations", "true");
        if (settings.environment().equals("prod")) {
            expect(environment, "spring.flyway.enabled", "false");
        } else {
            String enabled = environment.getProperty("spring.flyway.enabled");
            if (!"true".equals(enabled) && !"false".equals(enabled)) {
                throw new IllegalStateException("spring.flyway.enabled 必须是 true 或 false");
            }
        }
    }

    private static URI publicOrigin(Environment environment) {
        String value = environment.getProperty("pet.public-origin");
        try { return value == null || value.isBlank() ? null : URI.create(value); }
        catch (IllegalArgumentException failure) {
            throw new IllegalStateException("pet.public-origin 必须是合法的公开来源");
        }
    }

    private static String required(Environment environment, String name) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) { throw new IllegalStateException("缺少必要数据库配置：" + name); }
        return value;
    }

    private static void expect(Environment environment, String name, String expected) {
        if (!expected.equals(environment.getProperty(name))) {
            throw new IllegalStateException(name + " 必须设置为 " + expected);
        }
    }

    private static void optionalExpected(Environment environment, String name, String expected) {
        if (environment.containsProperty(name)) { expect(environment, name, expected); }
    }
}
