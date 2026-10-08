package com.pet.testing.tenantpersistence;

import com.pet.platform.shared.persistence.*;
import com.pet.platform.shared.tenancy.StoreScopeGuard;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.*;
import java.util.function.*;
import org.springframework.data.domain.*;

/** 测试模块受控基础设施；所有底层方法protected，应用只得到固定命令。 */
final class SafetyRepositories {
    static class Parent extends ScopedPersistence<SafetyParent> {
        Parent(EntityManager em, Clock clock, StoreScopeGuard stores) { super(em, TenantPersistenceIT.PERMISSION, SafetyParent.class, ResourceAccessPolicy.tenantOnly(), Set.of("displayName"), clock, stores); }
        UUID create(String code) { return insertNew(() -> new SafetyParent(code, "初值")).getId(); }
        UUID attemptedEntity(SafetyParent entity) { return insertNew(() -> entity).getId(); }
        SafetyParent target(UUID id) { return require(id); }
        void rename(UUID id, String name) { update(id, entity -> entity.rename(name)); }
        void mutate(UUID id, Consumer<SafetyParent> change) { update(id, change); }
        void remove(UUID id) { delete(id); }
        int renameAll(Collection<UUID> ids, String value) { return updateBatch(ids, "displayName", value); }
        int removeAll(Collection<UUID> ids) { return deleteBatch(ids); }
        Page<SafetyParent> list(BusinessCondition<SafetyParent> condition, Pageable page) { return page(condition, page); }
        long total(BusinessCondition<SafetyParent> condition) { return count(condition); }
        boolean visible(UUID id) { return exists(id); }
        List<SafetyParent> ids(Collection<UUID> ids) { return findIds(ids); }
        void forbiddenField(Collection<UUID> ids) { updateBatch(ids, "tenantId", UUID.randomUUID()); }
    }
    static class Child extends ScopedPersistence<SafetyChild> {
        Child(EntityManager em, Clock clock, StoreScopeGuard stores) { super(em, TenantPersistenceIT.PERMISSION, SafetyChild.class, ResourceAccessPolicy.tenantOnly(), Set.of(), clock, stores); }
        UUID create(SafetyParent parent) { com.pet.platform.shared.tenancy.TenantScopeGuard.requireTenant(parent.getTenantId()); return insertNew(() -> new SafetyChild(parent)).getId(); }
        SafetyChild target(UUID id) { return require(id); }
    }
    static class Store extends ScopedPersistence<SafetyStoreResource> {
        private final StoreScopeGuard guard;
        Store(EntityManager em, Clock clock, StoreScopeGuard stores) { super(em, TenantPersistenceIT.PERMISSION, SafetyStoreResource.class,
            ResourceAccessPolicy.storesAndOwned("ownerType", "ownerId"), Set.of("displayName"), clock, stores); guard = stores; }
        UUID create(UUID store, String code) { return insertNew(() -> new SafetyStoreResource(store, guard, code)).getId(); }
        void rename(UUID id, String name) { update(id, e -> e.rename(name)); }
        void remove(UUID id) { delete(id); }
        SafetyStoreResource target(UUID id) { return require(id); }
        Page<SafetyStoreResource> list(Pageable page) { return page(null, page); }
        long total() { return count(null); }
        boolean visible(UUID id) { return exists(id); }
        void mutate(UUID id, Consumer<SafetyStoreResource> change) { update(id, change); }
        int renameAll(Collection<UUID> ids, String value) { return updateBatch(ids, "displayName", value); }
        int removeAll(Collection<UUID> ids) { return deleteBatch(ids); }
    }
    static class Owned extends ScopedPersistence<SafetyOwnedResource> {
        Owned(EntityManager em, Clock clock, StoreScopeGuard stores) { super(em, TenantPersistenceIT.PERMISSION, SafetyOwnedResource.class,
            ResourceAccessPolicy.owned("ownerType", "ownerId"), Set.of("displayName"), clock, stores); }
        void rename(UUID id, String name) { update(id, e -> e.rename(name)); }
        void remove(UUID id) { delete(id); }
        UUID create() { return insertNew(() -> new SafetyOwnedResource("本人夹具")).getId(); }
        Page<SafetyOwnedResource> list(Pageable page) { return page(null, page); }
        boolean visible(UUID id) { return exists(id); }
        long total() { return count(null); }
    }
}
