package com.pet.platform.shared.persistence;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigurationRulesTest {
    private MockEnvironment configuration(String profile) throws IOException {
        var environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        var loader = new YamlPropertySourceLoader();
        for (String file : new String[]{"application-" + profile + ".yml", "application.yml"}) {
            loader.load(file, new ClassPathResource(file)).forEach(environment.getPropertySources()::addLast);
        }
        environment.withProperty("pet.public-origin", "https://example.invalid")
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:15432/fixture")
                .withProperty("spring.datasource.username", "technical_fixture")
                .withProperty("spring.datasource.password", "technical_fixture")
                .withProperty("spring.flyway.url", "jdbc:postgresql://localhost:15432/fixture")
                .withProperty("spring.flyway.user", "technical_migration")
                .withProperty("spring.flyway.password", "technical_migration");
        return environment;
    }

    @Test void localAndProductionDefaultsHaveExplicitSafeStrategies() throws Exception {
        var local = configuration("local");
        assertDoesNotThrow(() -> DatabaseConfigurationRules.validate(local));
        var prod = configuration("prod");
        assertDoesNotThrow(() -> DatabaseConfigurationRules.validate(prod));
        assertEquals("false", prod.getProperty("spring.flyway.enabled"));
        assertEquals("validate", prod.getProperty("spring.jpa.hibernate.ddl-auto"));
    }

    @Test void missingConfigurationNamesAreReportedWithoutValues() throws Exception {
        for (String name : new String[]{"spring.datasource.url", "spring.datasource.username", "spring.datasource.password"}) {
            var environment = configuration("prod").withProperty(name, "");
            var error = assertThrows(IllegalStateException.class, () -> DatabaseConfigurationRules.validate(environment));
            assertTrue(error.getMessage().contains(name));
            assertFalse(error.getMessage().contains("technical_fixture"));
        }
    }

    @Test void refusesSecretsInUrlAndOtherDatabaseSystems() throws Exception {
        for (String url : new String[]{"jdbc:h2:mem:test", "jdbc:postgresql://secret:secret@localhost/fixture",
                "jdbc:postgresql://localhost/fixture?password=TECHNICAL_SECRET", "jdbc:postgresql://localhost:65536/fixture"}) {
            var environment = configuration("local").withProperty("spring.datasource.url", url);
            var error = assertThrows(IllegalStateException.class, () -> DatabaseConfigurationRules.validate(environment));
            assertFalse(error.getMessage().contains("TECHNICAL_SECRET"));
            assertFalse(error.getMessage().contains(url));
        }
    }

    @Test void externalOverridesCannotEnableDangerousDefaults() throws Exception {
        for (var entry : Map.of("spring.jpa.open-in-view", "true", "spring.jpa.hibernate.ddl-auto", "update",
                "spring.jpa.generate-ddl", "true",
                "spring.jpa.properties.jakarta.persistence.schema-generation.database.action", "create",
                "spring.sql.init.mode", "always", "spring.flyway.clean-disabled", "false",
                "spring.flyway.baseline-on-migrate", "true", "spring.flyway.validate-on-migrate", "false",
                "spring.flyway.out-of-order", "true").entrySet()) {
            var environment = configuration("local").withProperty(entry.getKey(), entry.getValue());
            assertThrows(IllegalStateException.class, () -> DatabaseConfigurationRules.validate(environment));
        }
    }

    @Test void sharedEntityCachesCannotBeEnabledByOverrides() throws Exception {
        for (String property : new String[]{"spring.jpa.properties.hibernate.cache.use_second_level_cache", "spring.jpa.properties.hibernate.cache.use_query_cache"}) {
            assertThrows(IllegalStateException.class, () -> DatabaseConfigurationRules.validate(configuration("local").withProperty(property, "true")));
        }
    }

    @Test void productionCannotRunStartupMigrations() throws Exception {
        var environment = configuration("prod").withProperty("spring.flyway.enabled", "true");
        assertThrows(IllegalStateException.class, () -> DatabaseConfigurationRules.validate(environment));
    }
}
