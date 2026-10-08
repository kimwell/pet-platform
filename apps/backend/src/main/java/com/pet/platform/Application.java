package com.pet.platform;

import com.pet.platform.shared.config.EnvironmentSettings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
@EnableConfigurationProperties(EnvironmentSettings.class)
public class Application {
    @Bean
    InitializingBean verifyRuntimeProfile(EnvironmentSettings settings, Environment environment) {
        return () -> settings.verifyProfiles(environment.getActiveProfiles());
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
