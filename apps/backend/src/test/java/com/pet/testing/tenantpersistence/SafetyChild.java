package com.pet.testing.tenantpersistence;

import com.pet.platform.shared.persistence.*;
import jakarta.persistence.*;
import java.util.UUID;

/** 复合外键技术夹具；只存引用ID，关联投影必须再次经父资源受控查询。 */
@Entity @Table(name = "safety_child")
public class SafetyChild extends TenantScopedEntity {
    @Version private long version;
    @Column(name = "parent_id", nullable = false, updatable = false) private UUID parentId;
    @Column(name = "display_name", nullable = false) private String displayName;
    protected SafetyChild() { }
    SafetyChild(SafetyParent parent) { initializeTenant(); parentId = parent.getId(); displayName = "子资源夹具"; }
    UUID parentId() { return parentId; }
}
