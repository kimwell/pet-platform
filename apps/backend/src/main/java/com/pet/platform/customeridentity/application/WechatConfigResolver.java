package com.pet.platform.customeridentity.application;
import java.util.UUID;
/** 入口由服务端选择，secret仅在受限Gateway内部使用。 */
public interface WechatConfigResolver {
 record Resolved(UUID tenantId,String configId,long version,String appId,String secret,int timeoutMillis,int exchangesPerMinute) {
   @Override public String toString(){return "Resolved[受限微信配置]";}
 }
 Resolved resolve(String tenantCode,String entryId);
}
