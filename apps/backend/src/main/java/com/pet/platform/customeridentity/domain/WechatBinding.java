package com.pet.platform.customeridentity.domain;
import com.pet.platform.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.util.UUID;
/** 正式客户身份映射；创建仅经微信认证受限函数，不开放资料CRUD。 */
@Entity @Table(name="customer_wechat_binding",schema="public")
public class WechatBinding extends TenantScopedEntity {
 protected WechatBinding(){}
 @Column(name="customer_id",nullable=false,updatable=false) private UUID customerId;
 @Column(name="app_id",nullable=false,updatable=false,length=32) private String appId;
 @Column(name="open_id",nullable=false,updatable=false,length=128) private String openId;
 @Column(nullable=false) private String status;
 @Version private long version;
 public UUID getCustomerId(){return customerId;} public String getStatus(){return status;}
}
