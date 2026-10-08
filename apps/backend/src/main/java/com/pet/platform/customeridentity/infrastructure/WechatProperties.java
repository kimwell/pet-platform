package com.pet.platform.customeridentity.infrastructure;
import java.util.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
/** 所有秘密由环境/外部秘密配置注入；默认关闭，没有通用AppID。 */
@ConfigurationProperties("pet.wechat")
public record WechatProperties(boolean enabled,Map<String,Application> applications,Map<String,Entry> entries) {
 public record Application(long version,String appId,String secretProperty,int timeoutMillis,int exchangesPerMinute) {}
 public record Entry(boolean enabled,String application,Set<String> tenantCodes) {}
 public WechatProperties {applications=applications==null?Map.of():Map.copyOf(applications);entries=entries==null?Map.of():Map.copyOf(entries);}
}
