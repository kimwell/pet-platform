package com.pet.testing.persistence;

import com.pet.platform.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** 仅测试技术实体，位于生产基础包扫描之外，不是行业业务模型。 */
@Entity
@Table(name = "persistence_probe")
public class PersistenceProbe extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String code;
    @Column(name = "display_name")
    private String displayName;
    @Column(nullable = false)
    private String tag;

    protected PersistenceProbe() { }
    public PersistenceProbe(String code, String displayName, String tag) {
        this.code = code;
        this.displayName = displayName;
        this.tag = tag;
    }
    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public void rename(String displayName) { this.displayName = displayName; }
}
