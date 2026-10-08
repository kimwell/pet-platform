package com.pet.testing.persistence;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = PersistenceProbe.class)
@EnableJpaRepositories(basePackageClasses = ProbeRepository.class)
public class PersistenceFixtures {
    @Bean MutableClock fixtureClock() { return new MutableClock(); }
    @Bean ProbeApplicationService probeApplicationService(ProbeRepository repository) {
        return new ProbeApplicationService(repository);
    }

    public static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-08T01:02:03.123456789Z");
        public void set(Instant now) { this.now = now; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        @Override public Instant instant() { return now; }
    }
}
