package com.pet.platform.shared.tenancy;

import java.time.Duration;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class AsyncExecutionConfiguration {
    @Bean(destroyMethod = "close") TenantTaskExecutor tenantTaskExecutor(Environment env,org.springframework.beans.factory.ObjectProvider<com.pet.platform.shared.security.TaskAuthority> authority) {
        return new TenantTaskExecutor(env.getProperty("pet.async.threads", Integer.class, 2),
                env.getProperty("pet.async.queue-capacity", Integer.class, 32),
                DurationStyle.detectAndParse(env.getProperty("pet.async.max-snapshot-age", "30s")),
                DurationStyle.detectAndParse(env.getProperty("pet.async.shutdown-wait", "5s")),authority.getIfAvailable());
    }
}
