package com.pet.platform.customeridentity.domain;
import com.pet.platform.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.util.UUID;
/** 正式客户身份映射；创建仅经微信认证受限函数，不开放资料CRUD。 */
@Entity @Table(name="customer_subject",schema="public")
public class CustomerSubject extends TenantScopedEntity {
 protected CustomerSubject(){}
 @Column(nullable=false) private String status;
 @Column(name="security_version",nullable=false) private long securityVersion;
 @Version private long version;
 public String getStatus(){return status;} public long getSecurityVersion(){return securityVersion;} public long getVersion(){return version;}
}
