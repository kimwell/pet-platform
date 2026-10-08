package com.pet.platform.platform.domain;

import com.pet.platform.shared.persistence.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** 正式身份数据映射；本轮只读，创建仅经显式初始化函数，无HTTP实体或删除入口。 */
@Entity @Immutable @JsonIgnoreType
@Table(name = "platform_tenant", schema = "public")
public class Tenant extends BaseEntity {
    @Version @Column(nullable = false) private long version;
    protected Tenant() { }
    @Column(name = "code", nullable = false, length = 32) private String code;
    @Column(name = "name", nullable = false, length = 100) private String name;
    @Column(name = "status", nullable = false, length = 16) private String status;
    @Column(name = "security_version", nullable = false) private long securityVersion;
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public long getSecurityVersion() { return securityVersion; }
    public long getVersion() { return version; }
    public boolean isActive() { return "ACTIVE".equals(status); }
    @Override public String toString() { return "Tenant[受限身份记录]"; }
}
