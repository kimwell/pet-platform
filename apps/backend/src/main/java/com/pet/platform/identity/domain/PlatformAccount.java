package com.pet.platform.identity.domain;

import com.pet.platform.shared.persistence.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

/** 独立控制面主体，不属于Tenant；凭据不向HTTP或通用Repository暴露。 */
@Entity @Immutable @JsonIgnoreType @Table(name="platform_account",schema="pet_control")
public class PlatformAccount extends BaseEntity {
    protected PlatformAccount() { }
    @Column(name="login_name",nullable=false,length=64) private String loginName;
    @Column(name="display_name",nullable=false,length=100) private String displayName;
    @Column(nullable=false,length=16) private String status;
    @Column(name="password_hash",nullable=false,length=256) private String passwordHash;
    @Column(name="security_version",nullable=false) private long securityVersion;
    @Column(name="authorization_version",nullable=false) private long authorizationVersion;
    @Version @Column(nullable=false) private long version;
    @Override public String toString(){return "PlatformAccount[受限控制面记录]";}
}
