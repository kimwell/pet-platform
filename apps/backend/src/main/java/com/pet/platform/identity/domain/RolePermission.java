package com.pet.platform.identity.domain;

import com.pet.platform.shared.persistence.TenantScopedEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** 正式身份数据映射；本轮只读，创建仅经显式初始化函数，无HTTP实体或删除入口。 */
@Entity @Immutable @JsonIgnoreType
@Table(name = "identity_role_permission", schema = "public")
public class RolePermission extends TenantScopedEntity {
    @Version @Column(nullable = false) private long version;
    protected RolePermission() { }
    @Column(name = "role_id", nullable = false) private UUID roleId;
    @Column(name = "permission_code", nullable = false, length = 100) private String permissionCode;
    @Column(name = "scope_type", nullable = false, length = 16) private String scopeType;
    public UUID getRoleId() { return roleId; }
    public String getPermissionCode() { return permissionCode; }
    public String getScopeType() { return scopeType; }
    public long getVersion() { return version; }
    @Override public String toString() { return "RolePermission[受限身份记录]"; }
}
