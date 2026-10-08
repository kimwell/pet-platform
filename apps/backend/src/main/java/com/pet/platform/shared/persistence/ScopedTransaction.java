package com.pet.platform.shared.persistence;

import com.pet.platform.shared.exception.TenantAccessDeniedException;
import com.pet.platform.shared.tenancy.TenantContext;
import com.pet.platform.shared.tenancy.TenantScopeGuard;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 同一JPA事务连接设置局部RLS参数；执行范围变化必须开启另一独立事务。 */
final class ScopedTransaction {
    private static final Object KEY = new Object();
    private record Binding(TenantContext context, Object factory) { }
    private ScopedTransaction() { }

    static TenantContext enter(EntityManager em) {
        var context = TenantScopeGuard.requireBusiness();
        if (!TransactionSynchronizationManager.isActualTransactionActive() || !em.isJoinedToTransaction()) {
            throw new IllegalStateException("受控持久化必须位于应用事务内");
        }
        var existing = (Binding) TransactionSynchronizationManager.getResource(KEY);
        if (existing != null) {
            requireCurrent();
            if (existing.factory() != em.getEntityManagerFactory()) throw new IllegalStateException("事务不能混用持久化工厂");
            return context;
        }
        em.unwrap(Session.class).doWork(connection -> {
            try (var statement = connection.prepareStatement("select set_config('pet.tenant_id', ?, true)")) {
                statement.setString(1, context.tenantId().toString());
                statement.execute();
            }
        });
        var binding = new Binding(context, em.getEntityManagerFactory());
        TransactionSynchronizationManager.bindResource(KEY, binding);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void suspend() { TransactionSynchronizationManager.unbindResource(KEY); }
            @Override public void resume() { TransactionSynchronizationManager.bindResource(KEY, binding); }
            @Override public void beforeCommit(boolean readOnly) { requireCurrent(); }
            @Override public void afterCompletion(int status) { TransactionSynchronizationManager.unbindResourceIfPossible(KEY); }
        });
        return context;
    }

    static TenantContext requireCurrent() {
        var current = TenantScopeGuard.requireBusiness();
        var binding = (Binding) TransactionSynchronizationManager.getResource(KEY);
        if (binding == null || !binding.context().equals(current)) throw new TenantAccessDeniedException();
        return current;
    }
}
