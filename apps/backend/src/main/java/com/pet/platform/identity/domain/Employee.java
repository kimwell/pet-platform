package com.pet.platform.identity.domain;

import com.pet.platform.shared.persistence.TenantScopedEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** 正式身份数据映射；本轮只读，创建仅经显式初始化函数，无HTTP实体或删除入口。 */
@Entity @Immutable @JsonIgnoreType
@Table(name = "identity_employee", schema = "public")
public class Employee extends TenantScopedEntity {
    @Version @Column(nullable = false) private long version;
    protected Employee() { }
    @Column(name = "login_name", nullable = false, length = 64) private String loginName;
    @Column(name = "display_name", nullable = false, length = 100) private String displayName;
    @Column(name = "status", nullable = false, length = 16) private String status;
    @Column(name = "password_hash", nullable = false, length = 256) private String passwordHash;
    @Column(name = "security_version", nullable = false) private long securityVersion;
    @Column(name = "authorization_version", nullable = false) private long authorizationVersion;
    public String getLoginName() { return loginName; }
    public String getDisplayName() { return displayName; }
    public String getStatus() { return status; }
    public long getSecurityVersion() { return securityVersion; }
    public long getAuthorizationVersion() { return authorizationVersion; }
    public long getVersion() { return version; }
    public boolean isActive() { return "ACTIVE".equals(status); }
    @Override public String toString() { return "Employee[受限身份记录]"; }
}
