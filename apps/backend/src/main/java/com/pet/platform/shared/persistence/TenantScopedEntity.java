package com.pet.platform.shared.persistence;

import com.pet.platform.shared.exception.TenantAccessDeniedException;
import com.pet.platform.shared.tenancy.TenantContext;
import com.pet.platform.shared.tenancy.TenantScopeGuard;
import jakarta.persistence.*;
import java.util.UUID;

/** 租户归属没有公共赋值入口；无参构造仅供JPA，业务构造显式初始化。 */
@MappedSuperclass
public abstract class TenantScopedEntity extends BaseEntity {
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
    @Transient private UUID originalTenantId;
    @Transient private TenantContext loadedContext;

    protected TenantScopedEntity() { }

    protected final void initializeTenant() {
        if (tenantId != null) throw new IllegalStateException("租户归属已经初始化");
        tenantId = TenantScopeGuard.requireBusiness().tenantId();
        originalTenantId = tenantId;
    }

    public UUID getTenantId() { return tenantId; }

    @PostLoad
    protected void rememberTenantExecution() {
        loadedContext = ScopedTransaction.requireCurrent();
        if (!loadedContext.tenantId().equals(tenantId)) throw new TenantAccessDeniedException();
        originalTenantId = tenantId;
    }

    @PrePersist @PreUpdate @PreRemove
    protected void validateTenantExecution() {
        var context = ScopedTransaction.requireCurrent();
        if (tenantId == null || !tenantId.equals(originalTenantId) || !context.tenantId().equals(tenantId)
                || loadedContext != null && !loadedContext.equals(context)) throw new TenantAccessDeniedException();
    }
}
