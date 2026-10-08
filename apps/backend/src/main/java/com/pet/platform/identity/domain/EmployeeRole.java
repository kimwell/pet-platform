package com.pet.platform.identity.domain;

import com.pet.platform.shared.persistence.TenantScopedEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** 正式身份数据映射；本轮只读，创建仅经显式初始化函数，无HTTP实体或删除入口。 */
@Entity @Immutable @JsonIgnoreType
@Table(name = "identity_employee_role", schema = "public")
public class EmployeeRole extends TenantScopedEntity {
    @Version @Column(nullable = false) private long version;
    protected EmployeeRole() { }
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Column(name = "role_id", nullable = false) private UUID roleId;
    public UUID getEmployeeId() { return employeeId; }
    public UUID getRoleId() { return roleId; }
    public long getVersion() { return version; }
    @Override public String toString() { return "EmployeeRole[受限身份记录]"; }
}
