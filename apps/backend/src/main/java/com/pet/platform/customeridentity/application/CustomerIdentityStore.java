package com.pet.platform.customeridentity.application;
import java.util.*;
/** 内部认证入口；不提供业务列表或任意租户执行。 */
public interface CustomerIdentityStore {
 record Tenant(UUID id,long securityVersion) {}
 Optional<Tenant> tenant(String code);
 Optional<CustomerIdentity> register(UUID tenant,String appId,String openId);
 Optional<CustomerIdentity> load(UUID tenant,UUID customer);
 long revoke(CustomerSessionPort.Fact fact,String trace);
 void event(UUID tenant,UUID customer,String result,String trace);
}
