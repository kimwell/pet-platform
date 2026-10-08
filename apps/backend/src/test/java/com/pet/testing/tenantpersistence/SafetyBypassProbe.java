package com.pet.testing.tenantpersistence;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.transaction.annotation.*;

/** 故意绕过的测试入口仅src/test；验证RLS，不是允许普通业务使用原生SQL。 */
public class SafetyBypassProbe {
    private final EntityManager em;
    private final SafetyRepositories.Parent parents;
    SafetyBypassProbe(EntityManager em, SafetyRepositories.Parent parents) { this.em = em; this.parents = parents; }
    @Transactional public int nativeRename(UUID id) { parents.total(null); return em.createNativeQuery("update safety_parent set display_name='原生写入' where id=:id").setParameter("id", id).executeUpdate(); }
    @Transactional public int jpqlRename(UUID id) { parents.total(null); return em.createQuery("update SafetyParent p set p.displayName='JPQL写入' where p.id=:id").setParameter("id", id).executeUpdate(); }
    @Transactional public void forceTenantMove(UUID id, UUID tenant) {
        parents.total(null); em.createNativeQuery("update safety_parent set tenant_id=:tenant where id=:id").setParameter("tenant",tenant).setParameter("id",id).executeUpdate();
    }
    @Transactional public void forgedNativeInsert(UUID tenant) {
        parents.total(null); em.createNativeQuery("insert into safety_parent(id,tenant_id,created_at,updated_at,version,code,display_name) values (:id,:tenant,now(),now(),0,'原生伪造','原生伪造')").setParameter("id",UUID.randomUUID()).setParameter("tenant",tenant).executeUpdate();
    }
    @Transactional public long withoutScope() { return ((Number)em.createNativeQuery("select count(*) from safety_parent").getSingleResult()).longValue(); }
    @Transactional(propagation = Propagation.REQUIRES_NEW) public long newTransactionCount() { return parents.total(null); }
    @Transactional public long requiredWithNew(SafetyBypassProbe proxy) { parents.total(null); long result = proxy.newTransactionCount(); return result + parents.total(null); }
}
