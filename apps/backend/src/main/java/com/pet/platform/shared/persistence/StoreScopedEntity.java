package com.pet.platform.shared.persistence;

import com.pet.platform.shared.exception.TenantAccessDeniedException;
import com.pet.platform.shared.tenancy.StoreScopeGuard;
import jakarta.persistence.*;
import java.util.UUID;

/** 门店创建经权威事实Guard；普通更新不能迁移归属。 */
@MappedSuperclass
public abstract class StoreScopedEntity extends TenantScopedEntity {
    @Column(name = "store_id", nullable = false, updatable = false)
    private UUID storeId;
    @Transient private UUID originalStoreId;

    protected StoreScopedEntity() { }

    protected final void initializeStore(UUID storeId, StoreScopeGuard guard) {
        if (this.storeId != null) throw new IllegalStateException("门店归属已经初始化");
        guard.requireStore(storeId);
        initializeTenant();
        this.storeId = storeId;
        originalStoreId = storeId;
    }

    public UUID getStoreId() { return storeId; }

    @PostLoad protected void rememberStore() { originalStoreId = storeId; }
    @PrePersist @PreUpdate @PreRemove protected void validateStore() {
        if (storeId == null || !storeId.equals(originalStoreId)) throw new TenantAccessDeniedException();
    }
}
