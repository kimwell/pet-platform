package com.pet.platform.shared.persistence;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(dateTimeProviderRef = "persistenceDateTimeProvider")
public class PersistenceConfiguration {
    /** 在连接池、Flyway、JPA 创建前校验，错误只包含配置名。 */
    @Bean
    static BeanFactoryPostProcessor databaseConfigurationValidation(Environment environment) {
        return beanFactory -> DatabaseConfigurationRules.validate(environment);
    }

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock persistenceClock() { return Clock.systemUTC(); }

    @Bean
    DateTimeProvider persistenceDateTimeProvider(Clock clock) {
        return () -> Optional.of(clock.instant().truncatedTo(ChronoUnit.MILLIS));
    }
}
