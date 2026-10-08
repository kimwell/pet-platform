package com.pet.testing.tenantpersistence;

import com.pet.platform.shared.persistence.*;
import jakarta.persistence.*;
import java.util.UUID;

import com.pet.platform.shared.security.PrincipalType;
import com.pet.platform.shared.tenancy.TenantScopeGuard;

@Entity @Table(name = "safety_owned_resource")
public class SafetyOwnedResource extends TenantScopedEntity {
    @Version private long version;
    @Enumerated(EnumType.STRING) @Column(name = "owner_type", nullable = false, updatable = false) private PrincipalType ownerType;
    @Column(name = "owner_id", nullable = false, updatable = false) private UUID ownerId;
    @Column(name = "display_name", nullable = false) private String displayName;
    protected SafetyOwnedResource() { }
    SafetyOwnedResource(String unusedBusinessName) { initializeTenant(); var context = TenantScopeGuard.requireBusiness();
        ownerType = context.principalType(); ownerId = context.principalId(); displayName = "本人夹具"; }
    void rename(String name) { displayName = name; }
}
