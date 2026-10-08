package com.pet.testing.tenantpersistence;

import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.UUID;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 仅测试观察：先经原ScopedPersistence绑定，再读取同一真实JPA连接的事务/GUC。 */
public class AsyncDatabaseProbe {
    private final SafetyRepositories.Parent parents;
    private final EntityManager em;
    AsyncDatabaseProbe(SafetyRepositories.Parent parents, EntityManager em) { this.parents = parents; this.em = em; }
    @Transactional public Map<String, Object> observe() {
        long count = parents.total(null);
        return em.unwrap(Session.class).doReturningWork(connection -> {
            try (var s = connection.createStatement(); var r = s.executeQuery("select txid_current(), pg_backend_pid(), current_setting('pet.tenant_id',true), current_user")) {
                r.next(); return Map.of("count",count,"transactionId",r.getLong(1),"connectionPid",r.getInt(2),"tenantGuc",r.getString(3),"runtimeRole",r.getString(4),
                        "transactionActive",TransactionSynchronizationManager.isActualTransactionActive());
            }
        });
    }
    @Transactional public void renameBeforeCommit(UUID id, Runnable beforeCommit) { parents.rename(id,"期限内flush但提交应拒绝"); beforeCommit.run(); }
}
