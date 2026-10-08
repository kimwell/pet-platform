package com.pet.platform.platform.domain;

import com.pet.platform.shared.persistence.TenantScopedEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** 正式身份数据映射；本轮只读，创建仅经显式初始化函数，无HTTP实体或删除入口。 */
@Entity @Immutable @JsonIgnoreType
@Table(name = "platform_store", schema = "public")
public class Store extends TenantScopedEntity {
    @Version @Column(nullable = false) private long version;
    protected Store() { }
    @Column(name = "code", nullable = false, length = 32) private String code;
    @Column(name = "name", nullable = false, length = 100) private String name;
    @Column(name = "status", nullable = false, length = 16) private String status;
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public long getVersion() { return version; }
    public boolean isActive() { return "ACTIVE".equals(status); }
    @Override public String toString() { return "Store[受限身份记录]"; }
}
