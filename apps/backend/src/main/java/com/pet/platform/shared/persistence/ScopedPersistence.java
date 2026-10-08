package com.pet.platform.shared.persistence;

import com.pet.platform.shared.exception.TenantAccessDeniedException;
import com.pet.platform.shared.tenancy.StoreScopeGuard;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.data.domain.*;

/** 只供本模块适配器继承；不公开任意Entity的save/merge或底层数据库句柄。 */
public abstract class ScopedPersistence<T extends TenantScopedEntity> {
    @FunctionalInterface
    public interface BusinessCondition<T> { Predicate predicate(Root<T> root, CriteriaBuilder cb); }
    private final EntityManager em;
    private final String permissionCode;
    private final Class<T> entityType;
    private final ResourceAccessPolicy policy;
    private final Set<String> mutableAttributes;
    private final Clock clock;
    private final StoreScopeGuard stores;

    protected ScopedPersistence(EntityManager em, String permissionCode, Class<T> entityType, ResourceAccessPolicy policy,
            Set<String> mutableAttributes, Clock clock, StoreScopeGuard stores) {
        this.em = Objects.requireNonNull(em); this.permissionCode = Objects.requireNonNull(permissionCode); this.entityType = entityType; this.policy = policy;
        this.mutableAttributes = Set.copyOf(mutableAttributes); this.clock = clock; this.stores = stores;
        if (mutableAttributes.stream().anyMatch(policy::isSecurityAttribute)) throw new IllegalArgumentException("业务更新白名单不能包含安全归属或审计字段");
    }
    private com.pet.platform.shared.tenancy.TenantContext enter() {
        com.pet.platform.shared.tenancy.TenantScopeGuard.requirePermission(permissionCode);
        return ScopedTransaction.enter(em);
    }
    private Predicate secured(Root<T> root, CriteriaBuilder cb, BusinessCondition<T> business) {
        var context = enter();
        policy.validate(context);
        Predicate condition = business == null ? cb.conjunction() : Objects.requireNonNull(business.predicate(root, cb));
        // 业务OR始终位于此AND内部，不能替换或短路安全条件。
        return cb.and(policy.predicate(root, cb, context), condition);
    }
    private TypedQuery<T> query(BusinessCondition<T> condition) {
        var cb = em.getCriteriaBuilder(); var query = cb.createQuery(entityType); var root = query.from(entityType);
        query.select(root).where(secured(root, cb, condition));
        return em.createQuery(query);
    }
    protected final Optional<T> find(UUID id) {
        return query((root, cb) -> cb.equal(root.get("id"), id)).getResultStream().findFirst();
    }
    protected final T require(UUID id) { return find(id).orElseThrow(TenantAccessDeniedException::new); }
    protected final long count(BusinessCondition<T> business) {
        var cb = em.getCriteriaBuilder(); var query = cb.createQuery(Long.class); var root = query.from(entityType);
        query.select(cb.count(root)).where(secured(root, cb, business));
        return em.createQuery(query).getSingleResult();
    }
    protected final boolean exists(UUID id) { return count((root, cb) -> cb.equal(root.get("id"), id)) != 0; }
    protected final List<T> findIds(Collection<UUID> ids) {
        var targets = batchIds(ids);
        return query((root, cb) -> root.get("id").in(targets)).getResultList();
    }
    protected final Page<T> page(BusinessCondition<T> business, Pageable pageable) {
        enter();
        if (pageable.isUnpaged() || pageable.getOffset() > Integer.MAX_VALUE || pageable.getPageSize() > 100) throw new IllegalArgumentException("分页必须来自受控分页适配器");
        var cb = em.getCriteriaBuilder(); var query = cb.createQuery(entityType); var root = query.from(entityType);
        query.select(root).where(secured(root, cb, business));
        var orders = new ArrayList<Order>();
        for (var order : pageable.getSort()) {
            if (!order.getProperty().matches("[a-zA-Z][a-zA-Z0-9]*")) throw new IllegalArgumentException("排序仅允许固定单层属性");
            orders.add(order.isAscending() ? cb.asc(root.get(order.getProperty()), jakarta.persistence.criteria.Nulls.LAST)
                    : cb.desc(root.get(order.getProperty()), jakarta.persistence.criteria.Nulls.LAST));
        }
        query.orderBy(orders);
        var items = em.createQuery(query).setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize()).getResultList();
        return new PageImpl<>(items, pageable, count(business));
    }
    protected final T insertNew(Supplier<T> factory) {
        var context = enter(); policy.validate(context);
        T entity = Objects.requireNonNull(factory.get());
        if (entity.getId() != null || !context.tenantId().equals(entity.getTenantId()) || em.contains(entity)) throw new TenantAccessDeniedException();
        if (entity instanceof StoreScopedEntity store) stores.requireStore(store.getStoreId());
        em.persist(entity); em.flush();
        // owner等资源策略必须也涵盖新行；失败由应用事务回滚。
        if (!exists(entity.getId())) throw new TenantAccessDeniedException();
        return entity;
    }
    protected final void update(UUID id, Consumer<T> allowedBusinessChange) {
        T entity = require(id);
        allowedBusinessChange.accept(entity);
        entity.validateTenantExecution();
        if (entity instanceof StoreScopedEntity store) store.validateStore();
        em.flush();
        if (!exists(id)) throw new TenantAccessDeniedException();
    }
    protected final void delete(UUID id) { deleteBatch(List.of(id)); }

    /** 内部原子集合命令；不是HTTP逐项独立事务的batch-actions协议。 */
    protected final int updateBatch(Collection<UUID> ids, String attribute, Object value) {
        if (!mutableAttributes.contains(attribute)) throw new IllegalArgumentException("字段不在业务更新白名单");
        var targets = lockAll(ids); em.flush();
        var cb = em.getCriteriaBuilder(); var update = cb.createCriteriaUpdate(entityType); var root = update.from(entityType);
        update.set(root.get(attribute), value);
        update.set(root.get("updatedAt"), clock.instant().truncatedTo(ChronoUnit.MILLIS));
        update.set(root.<Long>get("version"), cb.sum(root.<Long>get("version"), 1L));
        update.where(secured(root, cb, (r, builder) -> r.get("id").in(targets)));
        int affected = em.createQuery(update).executeUpdate();
        checkAffected(affected, targets.size()); em.clear();
        return affected;
    }
    protected final int deleteBatch(Collection<UUID> ids) {
        var targets = lockAll(ids); em.flush();
        var cb = em.getCriteriaBuilder(); var delete = cb.createCriteriaDelete(entityType); var root = delete.from(entityType);
        delete.where(secured(root, cb, (r, builder) -> r.get("id").in(targets)));
        int affected = em.createQuery(delete).executeUpdate();
        checkAffected(affected, targets.size()); em.clear();
        return affected;
    }
    private Set<UUID> lockAll(Collection<UUID> ids) {
        var targets = batchIds(ids);
        var rows = query((root, cb) -> root.get("id").in(targets)).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        if (rows.size() != targets.size()) throw new TenantAccessDeniedException();
        return targets;
    }
    private static Set<UUID> batchIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 100 || ids.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("批次须包含1至100个非空ID");
        return Set.copyOf(ids);
    }
    private static void checkAffected(int actual, int expected) {
        if (actual != expected) {
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new IllegalStateException("受控写入影响行数不一致");
        }
    }
}
