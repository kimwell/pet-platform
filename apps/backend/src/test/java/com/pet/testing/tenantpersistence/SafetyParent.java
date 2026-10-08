package com.pet.testing.tenantpersistence;

import com.pet.platform.shared.persistence.*;
import jakarta.persistence.*;
import java.util.UUID;

/** 租户级父资源技术夹具。 */
@Entity @Table(name = "safety_parent")
public class SafetyParent extends TenantScopedEntity {
    @Version private long version;
    @Column(nullable = false) private String code;
    @Column(name = "display_name", nullable = false) private String displayName;
    protected SafetyParent() { }
    SafetyParent(String code, String name) { initializeTenant(); this.code = code; displayName = name; }
    void rename(String name) { displayName = name; }
    String name() { return displayName; }
}
