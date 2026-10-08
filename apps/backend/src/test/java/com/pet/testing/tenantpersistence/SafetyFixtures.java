package com.pet.testing.tenantpersistence;

import com.pet.platform.shared.persistence.*;
import com.pet.platform.shared.security.*;
import com.pet.platform.shared.tenancy.*;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

@TestConfiguration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = SafetyParent.class)
public class SafetyFixtures {
    static class PrincipalFixture implements CurrentPrincipalProvider {
        private CurrentPrincipal value;
        void set(CurrentPrincipal value) { this.value = value; }
        public Optional<CurrentPrincipal> currentPrincipal() { return Optional.ofNullable(value); }
    }
    @Bean @Primary PrincipalFixture safetyPrincipal() { return new PrincipalFixture(); }
    @Bean @Primary StoreOwnershipReader safetyStoreFacts(JdbcTemplate jdbc) {
        return store -> jdbc.query("select tenant_id from safety_store_fact where id=?", (r, n) -> r.getObject(1, UUID.class), store).stream().findFirst();
    }
    @Bean SafetyRepositories.Parent safetyParents(EntityManager em, Clock clock, StoreScopeGuard stores) { return new SafetyRepositories.Parent(em, clock, stores); }
    @Bean SafetyRepositories.Child safetyChildren(EntityManager em, Clock clock, StoreScopeGuard stores) { return new SafetyRepositories.Child(em, clock, stores); }
    @Bean SafetyRepositories.Store safetyStores(EntityManager em, Clock clock, StoreScopeGuard stores) { return new SafetyRepositories.Store(em, clock, stores); }
    @Bean SafetyRepositories.Owned safetyOwned(EntityManager em, Clock clock, StoreScopeGuard stores) { return new SafetyRepositories.Owned(em, clock, stores); }
    @Bean SafetyApplicationService safetyService(SafetyRepositories.Parent p, SafetyRepositories.Child c, SafetyRepositories.Store s, SafetyRepositories.Owned o) { return new SafetyApplicationService(p, c, s, o); }
    @Bean SafetyBypassProbe safetyBypass(EntityManager em, SafetyRepositories.Parent p) { return new SafetyBypassProbe(em, p); }
}
