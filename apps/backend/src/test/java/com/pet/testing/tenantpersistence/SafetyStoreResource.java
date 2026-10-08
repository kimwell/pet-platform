package com.pet.testing.tenantpersistence;

import com.pet.platform.shared.persistence.*;
import jakarta.persistence.*;
import java.util.UUID;

import com.pet.platform.shared.tenancy.StoreScopeGuard;
import com.pet.platform.shared.tenancy.TenantScopeGuard;
import com.pet.platform.shared.security.PrincipalType;

@Entity @Table(name = "safety_store_resource")
public class SafetyStoreResource extends StoreScopedEntity {
    @Version private long version;
    @Column(nullable = false) private String code;
    @Column(name = "display_name", nullable = false) private String displayName;
    @Enumerated(EnumType.STRING) @Column(name = "owner_type", nullable = false, updatable = false) private PrincipalType ownerType;
    @Column(name = "owner_id", nullable = false, updatable = false) private UUID ownerId;
    protected SafetyStoreResource() { }
    SafetyStoreResource(UUID store, StoreScopeGuard guard, String code) {
        initializeStore(store, guard); this.code = code; displayName = "门店夹具";
        var context = TenantScopeGuard.requireBusiness(); ownerType = context.principalType(); ownerId = context.principalId();
    }
    void rename(String name) { displayName = name; }
}
