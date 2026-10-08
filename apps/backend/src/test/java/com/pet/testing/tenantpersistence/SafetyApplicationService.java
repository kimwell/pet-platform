package com.pet.testing.tenantpersistence;

import com.pet.platform.shared.api.*;
import com.pet.platform.shared.persistence.*;
import com.pet.platform.shared.tenancy.*;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;

/** 代理事务技术用例，测试方法自身不自动回滚。 */
public class SafetyApplicationService {
    private final SafetyRepositories.Parent parents;
    private final SafetyRepositories.Child children;
    private final SafetyRepositories.Store stores;
    private final SafetyRepositories.Owned owned;
    private static final SortWhitelist SORTS = new SortWhitelist(Map.of("id", "id", "name", "displayName"), "id", SortRule.Direction.ASC, "id");
    SafetyApplicationService(SafetyRepositories.Parent parents, SafetyRepositories.Child children, SafetyRepositories.Store stores, SafetyRepositories.Owned owned) {
        this.parents = parents; this.children = children; this.stores = stores; this.owned = owned;
    }
    private org.springframework.data.domain.Pageable page(int size) { return JpaPageAdapter.toPageable(new PageQuery(1, size), SORTS, Map.of()); }
    @Transactional public UUID create(String code) { return parents.create(code); }
    @Transactional public UUID createStore(UUID store, String code) { return stores.create(store, code); }
    @Transactional public UUID createOwned() { return owned.create(); }
    @Transactional public UUID createChild(UUID parent) { return children.create(parents.target(parent)); }
    @Transactional(readOnly = true) public String parentOfChild(UUID id) { return parents.target(children.target(id).parentId()).name(); }
    @Transactional(readOnly = true) public SafetyParent detached(UUID id) { return parents.target(id); }
    @Transactional public UUID attemptedEntity(SafetyParent entity) { return parents.attemptedEntity(entity); }
    @Transactional public UUID forgedCreate(UUID tenant) { var entity = new SafetyParent("伪造", "伪造"); changeField(entity, TenantScopedEntity.class, "tenantId", tenant); return parents.attemptedEntity(entity); }
    @Transactional public void renameStore(UUID id, String name) { stores.rename(id,name); }
    @Transactional public void removeStore(UUID id) { stores.remove(id); }
    @Transactional public void renameOwned(UUID id, String name) { owned.rename(id,name); }
    @Transactional public void removeOwned(UUID id) { owned.remove(id); }
    @Transactional public void rename(UUID id, String name) { parents.rename(id, name); }
    @Transactional public void remove(UUID id) { parents.remove(id); }
    @Transactional public int renameAll(Collection<UUID> ids, String value) { return parents.renameAll(ids, value); }
    @Transactional public int removeAll(Collection<UUID> ids) { return parents.removeAll(ids); }
    @Transactional public int renameStores(Collection<UUID> ids, String value) { return stores.renameAll(ids, value); }
    @Transactional public int removeStores(Collection<UUID> ids) { return stores.removeAll(ids); }
    @Transactional(readOnly = true) public String name(UUID id) { return parents.target(id).name(); }
    @Transactional(readOnly = true) public PageResponse<UUID> listParents(int size) { return JpaPageAdapter.fromPage(parents.list(null, page(size)).map(TenantScopedEntity::getId)); }
    @Transactional(readOnly = true) public PageResponse<UUID> orParents(int size) { return JpaPageAdapter.fromPage(parents.list((r, cb) -> cb.or(cb.equal(r.get("code"), "shared-code"), cb.equal(r.get("displayName"), "初值")), page(size)).map(TenantScopedEntity::getId)); }
    @Transactional(readOnly = true) public long countParents() { return parents.total(null); }
    @Transactional(readOnly = true) public boolean existsParent(UUID id) { return parents.visible(id); }
    @Transactional(readOnly = true) public List<UUID> parentIds(Collection<UUID> ids) { return parents.ids(ids).stream().map(TenantScopedEntity::getId).toList(); }
    @Transactional(readOnly = true) public PageResponse<UUID> listStores() { return JpaPageAdapter.fromPage(stores.list(page(100)).map(TenantScopedEntity::getId)); }
    @Transactional(readOnly = true) public boolean existsStore(UUID id) { return stores.visible(id); }
    @Transactional(readOnly = true) public long countStores() { return stores.total(); }
    @Transactional(readOnly = true) public PageResponse<UUID> listOwned() { return JpaPageAdapter.fromPage(owned.list(page(100)).map(TenantScopedEntity::getId)); }
    @Transactional(readOnly = true) public long countOwned() { return owned.total(); }
    @Transactional(readOnly = true) public boolean existsOwned(UUID id) { return owned.visible(id); }
    @Transactional public void mutateTenant(UUID id, UUID tenant) { parents.mutate(id, e -> changeField(e, TenantScopedEntity.class, "tenantId", tenant)); }
    @Transactional public void mutateStore(UUID id, UUID store) { stores.mutate(id, e -> { changeField(e, StoreScopedEntity.class, "storeId", store); e.rename("不应写入"); }); }
    @Transactional public void forbiddenField(Collection<UUID> ids) { parents.forbiddenField(ids); }
    @Transactional public void failAfterFlushedWrite(UUID id) { parents.rename(id, "已flush但应回滚"); throw new IllegalStateException("安全回滚夹具"); }
    @Transactional public void readThenNarrowAndWrite(UUID id, DataScope narrow) {
        stores.target(id);
        try (var scope = TenantExecutionScope.narrow(narrow)) {
            stores.mutate(id, e -> e.rename("范围变化不能写回"));
        }
    }
    @Transactional(readOnly = true) public List<UUID> narrowInside(DataScope narrow) {
        try (var scope = TenantExecutionScope.narrow(narrow)) { return stores.list(page(100)).map(TenantScopedEntity::getId).getContent(); }
        // 整个事务必须保持第一次访问的范围；commit前关闭收窄也会拒绝。
    }
    static void changeField(Object entity, Class<?> declaring, String name, Object value) {
        try { var field = declaring.getDeclaredField(name); field.setAccessible(true); field.set(entity, value); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
}
